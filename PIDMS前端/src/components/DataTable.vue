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
          <td v-for="(c, ci) in columns" :key="c.name">
            <!-- 树形模块（进度管理）：第一列按层级缩进，可展开/折叠子节点 -->
            <span
              v-if="tree && ci === 0"
              class="tree-grip"
              :style="{ paddingLeft: (row.__depth || 0) * 16 + 'px' }"
            >
              <button
                v-if="row.__hasChildren"
                class="tree-toggle"
                :aria-expanded="row.__expanded ? 'true' : 'false'"
                :title="row.__expanded ? '收起子节点' : '展开子节点'"
                @click.stop="$emit('toggle-expand', row)"
              >{{ row.__expanded ? "▾" : "▸" }}</button>
              <span v-else class="tree-leaf" aria-hidden="true">·</span>
            </span>
            <!-- 枚举 → 状态标签；若列带字典 options 则先按 code 解析中文名 -->
            <StatusBadge v-if="c.type === 'enum'" :value="enumText(c, row[c.name])" />
            <!-- 名称/标题类 → 可点击 -->
            <a
              v-else-if="isLinkCol(c.name)"
              class="link"
              @click="$emit('action', { name: 'view', record: row })"
            >{{ format(row[c.name]) }}</a>
            <span v-else :class="{ num: c.type === 'number', mono: isMonoCol(c.name) }">{{ format(row[c.name]) }}</span>
            <!-- 自动汇总标记（父节点的完成度 / 占总进度% 由子节点算出来，不是手填的） -->
            <span v-if="c.autoKey && row[c.autoKey]" class="tag tag--auto" title="由子节点自动汇总">自动</span>
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
  // 树形表格：行的 __depth / __hasChildren / __expanded 由父组件算好，这里只负责缩进与箭头
  tree: { type: Boolean, default: false },
  // 可选的按行解析动作文字函数：actionLabel(actionKey, row) => 显示文案（默认原样）
  actionLabel: { type: Function, default: null },
});
const emit = defineEmits(["action", "selection-change", "toggle-expand"]);

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

<style scoped>
/* 树形缩进：箭头的占位宽度固定，叶子节点的文字与父节点对齐 */
.tree-grip { display: inline-flex; align-items: center; margin-right: 4px; }
.tree-toggle {
  width: 16px;
  height: 16px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: #fff;
  color: var(--steel);
  font-size: 10px;
  line-height: 1;
  cursor: pointer;
}
.tree-toggle:hover { border-color: var(--blue); color: var(--blue); background: var(--blue-soft); }
.tree-leaf { display: inline-block; width: 16px; text-align: center; color: var(--line-strong); }
/* 「自动」标记：提示该值由子节点汇总得出，不是手填 */
.tag--auto {
  margin-left: 6px;
  padding: 1px 6px;
  font-size: 11px;
  background: rgba(138, 153, 171, .14);
  color: var(--muted);
}
.tag--auto::before { display: none; }
</style>
