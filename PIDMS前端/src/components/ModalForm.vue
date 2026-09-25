<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @mousedown.self="$emit('close')">
      <div ref="modalEl" class="modal modal--form" role="dialog" aria-modal="true" :style="modalStyle">
        <div
          class="modal__head"
          :class="{ dragging: drag.active }"
          @pointerdown="onHeadPointerDown"
        >
          <div class="modal__title">{{ title }}</div>
          <button class="modal__close" @click="$emit('close')" aria-label="关闭">✕</button>
        </div>
        <div class="modal__body">
          <div class="form">
            <div
              v-for="f in fields"
              :key="f.name"
              class="form-field"
              :class="{ 'field--full': f.type === 'textarea' || f.type === 'file' || f.fullWidth }"
            >
              <label>
                {{ f.label }}
                <span v-if="f.required" class="req">*</span>
              </label>
              <FieldControl
                :field="controlOf(f)"
                v-model="form[f.name]"
                @update:model-value="onFieldChange(f, $event)"
              />
              <div v-if="showError(f)" class="hint" style="color: var(--red)">{{ f.label }}为必填项</div>
              <div v-else-if="f.hint" class="hint">{{ f.hint }}</div>
            </div>
          </div>
        </div>
        <div class="modal__foot">
          <button class="btn" @click="$emit('close')">取消</button>
          <button class="btn btn--primary" @click="submit">保存</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, ref, computed, watch, onUnmounted } from "vue";
import FieldControl from "./FieldControl.vue";

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "表单" },
  fields: { type: Array, default: () => [] },
  record: { type: Object, default: null },
  /**
   * 联动下拉的选项加载器：field 带 enumFrom.dependsOn 时由父组件提供，
   * 返回 [{ value, label }]；返回空数组表示依赖项还没选，暂时没有候选。
   */
  loadFieldOptions: { type: Function, default: null },
});
const emit = defineEmits(["close", "submit"]);

const form = reactive({});
const tried = ref(false);

// ---------- 标题栏拖拽 ----------
const modalEl = ref(null);
const drag = reactive({ active: false, sx: 0, sy: 0, ox: 0, oy: 0, dx: 0, dy: 0 });
const modalStyle = computed(() => {
  if (!drag.dx && !drag.dy) return undefined;
  return { transform: `translate(${drag.dx}px, ${drag.dy}px)`, transition: drag.active ? "none" : "" };
});

function clamp(v, min, max) {
  return Math.min(Math.max(v, min), max);
}
function onHeadPointerDown(e) {
  // 只在主键 + 标题栏空白处拖拽，避免干扰按钮/输入控件
  if (e.button !== 0) return;
  if (e.target.closest("button, a, input, select, textarea, label")) return;
  drag.active = true;
  drag.sx = e.clientX;
  drag.sy = e.clientY;
  drag.ox = drag.dx;
  drag.oy = drag.dy;
  document.body.style.userSelect = "none";
  window.addEventListener("pointermove", onPointerMove);
  window.addEventListener("pointerup", onPointerUp);
  window.addEventListener("pointercancel", onPointerUp);
}
function onPointerMove(e) {
  if (!drag.active) return;
  const el = modalEl.value;
  if (!el) return;
  const vw = window.innerWidth;
  const vh = window.innerHeight;
  const mw = el.offsetWidth;
  const mh = el.offsetHeight;
  const baseX = (vw - mw) / 2;
  const baseY = (vh - mh) / 2;
  const rawX = drag.ox + (e.clientX - drag.sx);
  const rawY = drag.oy + (e.clientY - drag.sy);
  // 保留至少一部分在可视区内，标题栏始终可触及
  drag.dx = clamp(rawX, -baseX - mw + 120, vw - 120 - baseX - mw);
  drag.dy = clamp(rawY, -baseY - mh + 40, vh - 90 - baseY);
}
function onPointerUp() {
  drag.active = false;
  document.body.style.userSelect = "";
  window.removeEventListener("pointermove", onPointerMove);
  window.removeEventListener("pointerup", onPointerUp);
  window.removeEventListener("pointercancel", onPointerUp);
}

/* ---------- 联动下拉 / 只读字段 ----------
 * enumFrom.dependsOn 的字段（如进度节点的「上级节点」依赖「项目名称」）：
 * 打开弹窗时按依赖值加载一次；依赖值变化时清空重选，避免留着上一个项目的节点。
 * readonlyWhen 命中记录字段时（如父节点的完成度由子节点汇总）输入框置灰，且提交时不带上该字段。
 */
const linkedOpts = reactive({});
const readonlyMap = reactive({});

watch(
  () => [props.visible, props.record],
  () => {
    tried.value = false;
    drag.dx = 0;
    drag.dy = 0;
    Object.keys(form).forEach((k) => delete form[k]);
    Object.keys(linkedOpts).forEach((k) => delete linkedOpts[k]);
    props.fields.forEach((f) => {
      form[f.name] = recordValue(f);
    });
    // 联动下拉（enumFrom.dependsOn）与只读字段按当前记录重算
    props.fields.forEach((f) => {
      readonlyMap[f.name] = isReadonly(f);
      loadLinked(f);
    });
  },
  { immediate: true }
);

function isReadonly(f) {
  return Boolean(f.readonlyWhen && props.record && props.record[f.readonlyWhen]);
}

function controlOf(f) {
  const opts = linkedOpts[f.name];
  if (!opts && !readonlyMap[f.name]) return f;
  return { ...f, ...(opts ? { options: opts } : {}), disabled: Boolean(readonlyMap[f.name]) };
}

/**
 * 联动下拉（enumFrom.dependsOn：如「上级节点」依赖「项目名称」）：
 * 打开弹窗时按依赖值加载一次；依赖值变化时清空重选，避免留着上一个项目的节点。
 * enumFrom.loadOnOpen（如项目进度详情页的「上级节点」，项目已由路由固定）则只需打开时拉一次。
 */
async function loadLinked(f) {
  if (!props.loadFieldOptions || !f.enumFrom) return;
  if (!f.enumFrom.dependsOn && !f.enumFrom.loadOnOpen) return;
  try {
    linkedOpts[f.name] = (await props.loadFieldOptions(f, { ...form })) || [];
  } catch {
    linkedOpts[f.name] = [];
  }
}

function dependentFields(name) {
  return props.fields.filter((f) => f.enumFrom && f.enumFrom.dependsOn === name);
}

function onFieldChange(field, value) {
  const deps = dependentFields(field.name);
  deps.forEach((d) => {
    form[d.name] = defaultValue(d);
    loadLinked(d);
  });
}

// 弹窗打开时锁住底层页面滚动；Esc 可关闭
function onKeydown(e) {
  if (e.key === "Escape") emit("close");
}
watch(
  () => props.visible,
  (v) => {
    document.body.style.overflow = v ? "hidden" : "";
    if (v) window.addEventListener("keydown", onKeydown);
    else window.removeEventListener("keydown", onKeydown);
  },
  { immediate: true }
);
onUnmounted(() => {
  document.body.style.overflow = "";
  document.body.style.userSelect = "";
  window.removeEventListener("keydown", onKeydown);
  window.removeEventListener("pointermove", onPointerMove);
  window.removeEventListener("pointerup", onPointerUp);
  window.removeEventListener("pointercancel", onPointerUp);
});

function defaultValue(f) {
  if (f.type === "checkbox") return [];
  if (f.type === "file") return [];
  if (f.type === "number") return null;
  return "";
}

/* ---------- 多值字段的字符串 ↔ 数组互转 ----------
 * 带 valueSeparator 的字段（如项目的「项目成员」）：库里/接口里是 "吴十,张文琦" 这样的
 * 逗号串（后端字段是 VARCHAR），表单里用复选组勾选，所以要在这里做一次互转。
 * 反斜杠转义不处理——姓名里不会出现逗号或中文逗号。
 */
function recordValue(f) {
  const raw = props.record ? props.record[f.name] : undefined;
  if (!f.valueSeparator) return raw === undefined || raw === null ? defaultValue(f) : raw;
  // 编辑回显：字符串拆成数组；已经是数组（如 mock 里的脏数据）就直接用
  if (Array.isArray(raw)) return raw.filter((x) => x !== null && x !== undefined && x !== "");
  if (raw === undefined || raw === null) return defaultValue(f);
  return String(raw)
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean);
}

function showError(f) {
  if (!f.required || !tried.value) return false;
  const v = form[f.name];
  return v === "" || v === null || v === undefined || (Array.isArray(v) && v.length === 0);
}

function submit() {
  tried.value = true;
  const missing = props.fields.some((f) => {
    if (!f.required) return false;
    const v = form[f.name];
    return v === "" || v === null || v === undefined || (Array.isArray(v) && v.length === 0);
  });
  if (missing) return;
  const payload = { ...form };
  // 只读字段（父节点的完成度 / 占总进度%）不参与提交，避免把汇总值写回库里
  props.fields.forEach((f) => {
    if (readonlyMap[f.name]) {
      delete payload[f.name];
      return;
    }
    // 多值字段提交前拼回字符串（未勾选则提交空串，等于清空——null 会被 updateById 跳过）
    if (f.valueSeparator) {
      const v = payload[f.name];
      payload[f.name] = Array.isArray(v) ? v.join(f.valueSeparator) : v === null || v === undefined ? "" : v;
    }
  });
  emit("submit", payload);
}
</script>
