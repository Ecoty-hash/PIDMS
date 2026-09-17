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
              :class="{ 'field--full': f.type === 'textarea' || f.type === 'file' }"
            >
              <label>
                {{ f.label }}
                <span v-if="f.required" class="req">*</span>
              </label>
              <FieldControl :field="f" v-model="form[f.name]" />
              <div v-if="showError(f)" class="hint" style="color: var(--red)">{{ f.label }}为必填项</div>
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

watch(
  () => [props.visible, props.record],
  () => {
    tried.value = false;
    drag.dx = 0;
    drag.dy = 0;
    Object.keys(form).forEach((k) => delete form[k]);
    props.fields.forEach((f) => {
      form[f.name] = props.record?.[f.name] ?? defaultValue(f);
    });
  },
  { immediate: true }
);

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
  emit("submit", { ...form });
}
</script>
