<template>
  <div class="table-wrap">
    <div v-if="loading" class="loading"><span class="spinner"></span>加载中…</div>
    <table v-else-if="rows.length" class="table">
      <thead>
        <tr>
          <th v-if="selectable" style="width: 36px;">
            <input type="checkbox" class="check" :checked="allChecked" @change="toggleAll" />
          </th>
          <th v-for="c in columns" :key="c.name">{{ c.label }}</th>
          <th v-if="rowActions.length">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id" :class="{ 'is-selected': selected.has(row.id) }">
          <td v-if="selectable">
            <input type="checkbox" class="check" :checked="selected.has(row.id)" @change="toggle(row.id)" />
          </td>
          <td v-for="c in columns" :key="c.name">
            <!-- 枚举 → 状态标签；若列带字典 options 则先按 code 解析中文名 -->
            <StatusBadge v-if="c.type === 'enum'" :value="enumText(c, row[c.name])" />
            <!-- 名称/标题类 → 可点击 -->
            <a
              v-else-if="isLinkCol(c.name)"
              class="link"
              @click="$emit('action', { name: 'view', record: row })"
            >{{ format(row[c.name]) }}</a>
            <span v-else :class="{ num: c.type === 'number', mono: isMonoCol(c.name) }">{{ format(row[c.name]) }}</span>
          </td>
          <td v-if="rowActions.length" class="cell--actions">
            <template v-for="(a, i) in rowActions" :key="a">
              <a
                v-if="i > 0"
                class="sep"
              >|</a>
              <a
                :class="actionClass(a)"
                @click="$emit('action', { name: a, record: row, label: textOf(a, row) })"
              >{{ textOf(a, row) }}</a>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无数据，点击右上角「新增」创建第一条记录</div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import StatusBadge from "./StatusBadge.vue";

const props = defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  selectable: { type: Boolean, default: true },
  rowActions: { type: Array, default: () => [] },
  // 可选的按行解析动作文字函数：actionLabel(actionKey, row) => 显示文案（默认原样）
  actionLabel: { type: Function, default: null },
});
const emit = defineEmits(["action", "selection-change"]);

const selected = ref(new Set());

// 按钮显示文字：未提供 actionLabel 时直接用动作名
function textOf(a, row) {
  return typeof props.actionLabel === "function" ? props.actionLabel(a, row) : a;
}

const allChecked = computed(() => props.rows.length > 0 && props.rows.every((r) => selected.value.has(r.id)));

function toggle(id) {
  const s = new Set(selected.value);
  s.has(id) ? s.delete(id) : s.add(id);
  selected.value = s;
  emit("selection-change", [...s]);
}
function toggleAll(e) {
  const s = new Set(e.target.checked ? props.rows.map((r) => r.id) : []);
  selected.value = s;
  emit("selection-change", [...s]);
}

// 名称/标题列做链接
function isLinkCol(name) {
  return /name|title|名称|标题/.test(name);
}
// 编号/编码/单号列用等宽字体
function isMonoCol(name) {
  return /code|no|编号|单号|编码|No/.test(name);
}
function actionClass(a) {
  if (a === "删除" || a === "驳回" || a === "禁用") return "danger";
  if (a === "归档" || a === "封存" || a === "解封" || a === "启用") return "amber";
  return "";
}
// 枚举单元格文案：若列携带字典 options，先把存储的 code 映射为字典中文名（无匹配则回退原值）
function enumText(c, val) {
  const opts = c.options;
  if (Array.isArray(opts) && opts.length && val !== null && val !== undefined && val !== "") {
    const hit = opts.find((o) => String(o.value) === String(val));
    if (hit) return hit.label;
  }
  return val;
}
function format(v) {
  if (v === null || v === undefined || v === "") return "—";
  return String(v);
}

// 供父组件在批量操作完成后清空勾选
function clearSelection() {
  selected.value = new Set();
  emit("selection-change", []);
}
defineExpose({ clearSelection });
</script>
