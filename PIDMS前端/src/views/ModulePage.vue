<script setup>
import { ref, computed, watch } from "vue";
import { useRouter } from "vue-router";
import { getModule } from "@/config/modules.js";
import { getModuleApi, http } from "@/api/request.js";
import { useToast } from "@/composables/useToast.js";
import DataTable from "@/components/DataTable.vue";
import SearchPanel from "@/components/SearchPanel.vue";
import ModalForm from "@/components/ModalForm.vue";
import DetailPanel from "@/components/DetailPanel.vue";
import ConfirmDialog from "@/components/ConfirmDialog.vue";

const props = defineProps({
  moduleKey: { type: String, required: true },
});

const toast = useToast();
const router = useRouter();
const module = computed(() => getModule(props.moduleKey));
const api = computed(() => getModuleApi(module.value));

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const loading = ref(false);

const keyword = ref("");
const advFilters = ref({});
const showAdv = ref(false);
const searchPanel = ref(null);

const modal = ref({ visible: false, mode: "create", record: null });
const detail = ref({ visible: false, record: null });
const confirm = ref({ visible: false, title: "", message: "", onOk: null });
const tableRef = ref(null);
const selected = ref([]);

const title = computed(() => module.value.name);

/* ---------- 远程选项（enumFrom：表单/搜索下拉动态读取字典资源） ----------
 * 规则：
 * 1. 同时扫描 module.form 与 module.search 里所有 enumFrom 字段；
 * 2. 同一资源（resource+params 相同）只拉一次，不同字段共用同一份选项；
 * 3. 支持 src.params 作为字典过滤参数（如 { status: 'enabled' } 只显示启用项）；
 * 4. 编辑回显时：记录当前值若不在选项列表，仍保留为可选项，避免下拉空白。
 */
const dynamicOpts = ref({});
// 列表/详情回显用字典映射（不过滤状态，保证历史数据也能回显中文名）；
// 与 dynamicOpts（交互下拉，按 src.params 过滤）分开维护。
const displayOpts = ref({});
const formFields = computed(() =>
  (module.value.form || []).map((f) =>
    f.enumFrom ? { ...f, options: dynamicOpts.value[f.name] || [] } : f
  )
);
const searchFields = computed(() =>
  (module.value.search || []).map((f) =>
    f.enumFrom ? { ...f, options: dynamicOpts.value[f.name] || [] } : f
  )
);
// 列表/详情枚举列：若能按字段名找到字典选项，则把 options 挂到列上，
// 使列表单元格 / 详情抽屉能按 code 反向显示字典中文名（含自定义状态）。
const displayColumns = computed(() =>
  (module.value.columns || []).map((c) => {
    const opts = displayOpts.value[c.name];
    return opts ? { ...c, options: opts } : c;
  })
);
const detailFields = computed(() =>
  (module.value.detail || []).map((f) => {
    const opts = displayOpts.value[f.name];
    if (opts) return { ...f, options: opts, type: f.type || "enum" };
    return f;
  })
);

function enumFromSources() {
  const seen = new Map();
  [...(module.value.form || []), ...(module.value.search || [])].forEach((f) => {
    if (!f.enumFrom) return;
    const src = f.enumFrom;
    // path 型（如「上级节点」，候选依赖所选项目）由 ModalForm 回调 loadFieldOptions 按需加载
    if (src.path || !src.resource) return;
    const key = `${src.resource}|${JSON.stringify(src.params || {})}`;
    if (!seen.has(key)) seen.set(key, { fields: [], src });
    seen.get(key).fields.push(f);
  });
  return [...seen.values()];
}

/* 记录里的值 → 逐值数组（用于判断「值还在不在候选里」）。
 * 多值字段（field.valueSeparator，如「项目成员」的 "吴十,赵六"）逗号串要拆开逐个判断。 */
function recordValues(f, raw) {
  if (raw === null || raw === undefined || raw === "") return [];
  if (!f.valueSeparator) return [raw];
  if (Array.isArray(raw)) return raw.filter((x) => x !== null && x !== undefined && x !== "");
  return String(raw)
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean);
}

/* 候选补全：记录里已有、但不在候选里的值，补成可选项。
 * 场景：人已离职（status=disabled 不在「在职」候选里）、字典项已停用/删除。
 * 不补的话编辑时下拉是空的，一保存原值就被冲掉。多值字段逐个姓名补。 */
function withFallback(f, raw, list) {
  const miss = recordValues(f, raw).filter((v) => !list.some((o) => String(o.value) === String(v)));
  return [...miss.map((v) => ({ value: v, label: v })), ...list];
}

async function loadOptions(record) {
  const groups = enumFromSources();
  await Promise.all(groups.map(async ({ fields, src }) => {
    const vk = src.valueKey || "id";
    const lk = src.labelKey || vk;
    const toOpts = (list) => list
      .map((r) => ({ value: r[vk], label: r[lk] == null ? r[vk] : r[lk] }))
      // 数据兜底：跳过编码为空的行（历史行若缺编码，由升级脚本补齐或在字典编辑中补填），避免下拉出现空值选项
      .filter((o) => o.value !== null && o.value !== undefined && String(o.value).trim() !== "");

    // 交互下拉：按 src.params 过滤（如仅启用项）
    const params = { page: 1, pageSize: 1000, ...(src.params || {}) };
    const data = await getModuleApi({ resource: src.resource }).list(params);
    const opts = toOpts((data && data.list) || (Array.isArray(data) ? data : []));

    // 列表/详情回显映射：不过滤（停用/封存的历史行也要能回显中文名），仅在有过滤参数时额外拉一次全量
    let allOpts = opts;
    if (src.params && Object.keys(src.params).length) {
      const dataAll = await getModuleApi({ resource: src.resource }).list({ page: 1, pageSize: 1000 });
      allOpts = toOpts((dataAll && dataAll.list) || (Array.isArray(dataAll) ? dataAll : []));
    }

    fields.forEach((f) => {
      // 编辑回显：当前值若不在候选里（人已离职、字典项停用/删除）由 withFallback 补成可选项
      const raw = record && record[f.name];
      dynamicOpts.value[f.name] = withFallback(f, raw, opts);
      displayOpts.value[f.name] = withFallback(f, raw, allOpts);
    });
  }));
}

async function load() {
  loading.value = true;
  try {
    const data = await api.value.list({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      ...advFilters.value,
    });
    rows.value = data.list || data || [];
    total.value = data.total ?? rows.value.length;
  } catch (e) {
    toast.error(e.message || "列表加载失败");
  } finally {
    loading.value = false;
  }
}

/* ---------- 联动下拉（enumFrom.dependsOn：如「上级节点」依赖「项目名称」） ----------
 * 这类字段的候选不能一次性预加载，要等依赖项选好后再按 path + 依赖值拉取；
 * 由 ModalForm 在打开弹窗、依赖项变化时回调本函数。
 */
async function loadFieldOptions(field, values) {
  const src = field.enumFrom || {};
  if (!src.path) return [];
  const params = { ...(src.params || {}) };
  if (src.dependsOn) {
    const dep = values[src.dependsOn];
    if (dep === "" || dep === null || dep === undefined) return []; // 依赖项还没选，暂无候选
    params[src.dependsOn] = dep;
  }
  if (src.excludeSelf && modal.value.record?.id) params.excludeId = modal.value.record.id;

  const data = await http.get(src.path, params);
  const list = Array.isArray(data) ? data : (data && data.list) || [];
  const vk = src.valueKey || "id";
  const lk = src.labelKey || vk;
  return list
    .filter((r) => r[vk] !== null && r[vk] !== undefined && String(r[vk]).trim() !== "")
    .map((r) => {
      const label = r[lk] === null || r[lk] === undefined ? String(r[vk]) : String(r[lk]);
      // indentBy：层级字段（如 level），用缩进 + 折线让下拉里也能看出父子关系
      const depth = src.indentBy ? Number(r[src.indentBy]) || 0 : 0;
      return { value: r[vk], label: depth > 0 ? "　".repeat(depth) + "└ " + label : label };
    });
}

/* ---------- 树形列表（module.tree：子节点收纳在总节点下，点击展开/折叠） ----------
 * 后端按普通分页返回扁平行，这里把「本页」的行按 parentId 组成树：
 * 上级节点不在本页的行提升为顶层展示（否则翻页后子节点会整片消失）。
 */
const treeExpanded = ref(new Set());

function flattenTree(list) {
  const byId = new Map(list.map((r) => [String(r.id), r]));
  const childrenOf = new Map();
  const roots = [];
  list.forEach((r) => {
    const pid = r.parentId === null || r.parentId === undefined ? null : String(r.parentId);
    if (pid !== null && pid !== String(r.id) && byId.has(pid)) {
      if (!childrenOf.has(pid)) childrenOf.set(pid, []);
      childrenOf.get(pid).push(r);
    } else {
      roots.push(r);
    }
  });

  // 可达 = 顶层节点的整棵子树。判据必须是「可达」而不是「已渲染」：
  // 折叠中的子节点本来就没渲染，拿「已渲染」当判据会把它们全提到顶层，收起就失效了。
  const reachable = new Set();
  const mark = (row) => {
    const key = String(row.id);
    if (reachable.has(key)) return;
    reachable.add(key);
    (childrenOf.get(key) || []).forEach(mark);
  };
  roots.forEach(mark);

  const out = [];
  const emitted = new Set();
  const walk = (row, depth) => {
    const key = String(row.id);
    if (emitted.has(key)) return; // 环状脏数据保护
    emitted.add(key);
    const kids = childrenOf.get(key) || [];
    const expanded = treeExpanded.value.has(key);
    out.push({ ...row, __depth: depth, __hasChildren: kids.length > 0, __expanded: expanded });
    if (expanded) kids.forEach((k) => walk(k, depth + 1));
  };
  roots.forEach((r) => walk(r, 0));
  // 兜底：互为父子的脏数据谁都不是根（够不到），这里当顶层补回来，避免整行不显示
  list.forEach((r) => {
    if (!reachable.has(String(r.id))) walk(r, 0);
  });
  return out;
}

const visibleRows = computed(() => (module.value.tree ? flattenTree(rows.value) : rows.value));

function onToggleExpand(row) {
  const key = String(row.id);
  const s = new Set(treeExpanded.value);
  s.has(key) ? s.delete(key) : s.add(key);
  treeExpanded.value = s;
}
function expandAll() {
  treeExpanded.value = new Set(rows.value.filter((r) => r.childCount > 0).map((r) => String(r.id)));
}
function collapseAll() {
  treeExpanded.value = new Set();
}

watch(() => props.moduleKey, () => {
  page.value = 1;
  keyword.value = "";
  advFilters.value = {};
  showAdv.value = false;
  dynamicOpts.value = {};
  displayOpts.value = {};
  treeExpanded.value = new Set();
  loadOptions().catch(() => {}); // 搜索/表单下拉的字典选项，失败不阻塞列表
  load();
}, { immediate: true });

/* ---------- 搜索 ---------- */
function onQuickSearch() {
  page.value = 1;
  load();
}
function onAdvSearch() {
  advFilters.value = { ...(searchPanel.value?.filters || {}) };
  page.value = 1;
  load();
}
function onAdvReset() {
  advFilters.value = {};
  page.value = 1;
  load();
}

/* ---------- 新增 / 编辑 / 查看 ---------- */
async function openCreate() {
  try {
    await loadOptions();
    modal.value = { visible: true, mode: "create", record: null };
  } catch (e) {
    toast.error(e.message || "加载可选项失败");
  }
}
async function openEdit(record) {
  try {
    await loadOptions(record);
    modal.value = { visible: true, mode: "edit", record };
  } catch (e) {
    toast.error(e.message || "加载可选项失败");
  }
}
function openDetail(record) {
  detail.value = { visible: true, record };
}

async function onModalSubmit(data) {
  const payload = normalizeSubmit(data);
  try {
    if (modal.value.mode === "create") {
      await api.value.create(payload);
      toast.success(`已新增${title.value}`);
    } else {
      await api.value.update(modal.value.record.id, payload);
      toast.success(`已更新${title.value}`);
    }
    modal.value.visible = false;
    load();
  } catch (e) {
    toast.error(e.message || "保存失败");
  }
}

/**
 * 提交前的字段规整：下拉留空时按后端约定翻译。
 * 例如进度节点的「上级节点」留空 = 顶层节点，而后端编辑接口把 parentId=null 当作「本次不修改」，
 * 所以要带上 topLevel 标记，否则「把子节点提到顶层」保存不生效（与 actual_end_date 同一个坑）。
 */
function normalizeSubmit(data) {
  const out = { ...data };
  (module.value.form || []).forEach((f) => {
    if (!f.emptyToNull) return;
    const v = out[f.name];
    if (v === "" || v === null || v === undefined) {
      delete out[f.name];
      if (f.emptyFlag) out[f.emptyFlag] = true;
    }
  });
  return out;
}

/* ---------- 行操作 ---------- */
async function onTableAction({ name, record }) {
  if (name === "查看") return openDetail(record);
  if (name === "编辑") return openEdit(record);
  if (name === "删除") return askDelete(record);
  // 项目的「进度」动作：跳到该项目的进度详情页（甘特图 + 节点管理）
  if (name === "进度") return router.push(`/project/${record.id}/progress`);
  const sp = module.value.special.find((s) => s.label === name);
  if (sp) {
    // 动作文案以行当前状态现算，不依赖表格传来的 label，
    // 避免已归档行点「取消归档」时仍提示「归档成功」
    return runSpecial(sp, record, resolveRowActionLabel(name, record));
  }
}

// 归档是开关动作：已归档行显示「取消归档」，未归档行显示「归档」
function resolveRowActionLabel(a, record) {
  if (a === "归档" && record && typeof record.archiveStatus === "string") {
    const v = record.archiveStatus;
    if (v === "archived" || v === "已归档") return "取消归档";
  }
  return a;
}

function askDelete(record) {
  confirm.value = {
    visible: true,
    title: "删除确认",
    message: `确定要删除「${record[firstDisplay(record)]}」吗？此操作不可恢复。`,
    onOk: async () => {
      try {
        await api.value.remove(record.id);
        toast.success("已删除");
        confirm.value.visible = false;
        load();
      } catch (e) {
        toast.error(e.message || "删除失败");
      }
    },
  };
}

function runSpecial(sp, record, actLabel = sp.label) {
  const run = async () => {
    try {
      await api.value.special(sp, record.id);
      toast.success(`${actLabel}成功`);
      load();
    } catch (e) {
      toast.error(e.message || `${actLabel}失败`);
    }
  };
  if (sp.kind === "confirm") {
    confirm.value = {
      visible: true,
      title: `${actLabel}确认`,
      message: `确定要对「${record[firstDisplay(record)]}」执行「${actLabel}」操作吗？`,
      onOk: async () => {
        confirm.value.visible = false;
        await run();
      },
    };
  } else if (sp.kind === "approve" || sp.kind === "reject") {
    confirm.value = {
      visible: true,
      title: sp.kind === "approve" ? "审批通过" : "审批驳回",
      message: sp.kind === "approve"
        ? `确定审批通过「${record[firstDisplay(record)]}」吗？`
        : `确定驳回「${record[firstDisplay(record)]}」吗？`,
      onOk: async () => {
        confirm.value.visible = false;
        await run();
      },
    };
  } else {
    run();
  }
}

/* ---------- 批量操作（勾选行后触发的工具栏按钮） ---------- */
function onSelectionChange(ids) {
  selected.value = ids || [];
}
async function doBatch(b) {
  const n = selected.value.length;
  if (!n) {
    toast.warn("请先在表格中勾选要操作的项目");
    return;
  }
  const run = async () => {
    try {
      await api.value.batch(b, selected.value);
      toast.success(`已为选中的 ${n} 个项目申请编号（已有编号的自动跳过）`);
      tableRef.value?.clearSelection();
      load();
    } catch (e) {
      toast.error(e.message || `${b.label}失败`);
    }
  };
  if (b.kind === "confirm") {
    confirm.value = {
      visible: true,
      title: `${b.label}确认`,
      message: `确定为选中的 ${n} 个项目申请编号吗？已有编号的项目会自动跳过。`,
      onOk: async () => {
        confirm.value.visible = false;
        await run();
      },
    };
  } else {
    run();
  }
}

function firstDisplay(record) {
  const c = module.value.columns[0];
  return c ? record[c.name] ?? record.id : record.id;
}

/* ---------- 导出 / 导入 ---------- */
async function doExport() {
  try {
    const r = await api.value.export({ keyword: keyword.value, ...advFilters.value });
    toast.success(`导出成功：${r.fileName || "文件已生成"}`);
  } catch (e) {
    toast.error(e.message || "导出失败");
  }
}
function doImport() {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".xlsx,.xls";
  input.onchange = async () => {
    const file = input.files[0];
    if (!file) return;
    const fd = new FormData();
    fd.append("file", file);
    try {
      const r = await api.value.import(fd);
      toast.success(`导入完成：成功 ${r.successCount} 条，失败 ${r.failCount} 条`);
      load();
    } catch (e) {
      toast.error(e.message || "导入失败");
    }
  };
  input.click();
}

/* ---------- 分页 ---------- */
function gotoPage(p) {
  page.value = p;
  load();
}
function changeSize(n) {
  pageSize.value = n;
  page.value = 1;
  load();
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const pageList = computed(() => {
  const t = totalPages.value;
  const cur = page.value;
  const arr = [];
  for (let i = Math.max(1, cur - 2); i <= Math.min(t, cur + 2); i++) arr.push(i);
  return arr;
});
</script>

<template>
  <div class="rise">
    <div class="page-head">
      <div class="page-head__eyebrow">{{ module.group }} · MODULE</div>
      <h1 class="page-head__title">{{ module.name }}</h1>
    </div>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__left">
          <div class="search" style="position: relative;">
            <span class="search__icon">⌕</span>
            <input
              type="search"
              v-model="keyword"
              placeholder="输入关键字搜索…"
              aria-label="关键字搜索"
              @keydown.enter="onQuickSearch"
            />
          </div>
          <button class="btn" @click="showAdv = !showAdv">
            {{ showAdv ? "收起高级搜索" : "高级搜索" }}
          </button>
          <template v-if="module.tree">
            <button class="btn" @click="expandAll">展开全部</button>
            <button class="btn" @click="collapseAll">收起全部</button>
          </template>
        </div>
        <div class="toolbar__right">
          <button
            v-for="b in module.batch || []"
            :key="b.label"
            class="btn batch-btn"
            :disabled="!selected.length"
            :title="selected.length ? b.label : '请先在表格勾选要操作的项目'"
            @click="doBatch(b)"
          >{{ b.label }}</button>
          <button v-if="module.toolbar.includes('导出')" class="btn" @click="doExport">导出</button>
          <button v-if="module.toolbar.includes('导入')" class="btn" @click="doImport">导入</button>
          <button v-if="module.form.length" class="btn btn--primary" @click="openCreate">+ 新增</button>
        </div>
      </div>

      <SearchPanel
        v-if="showAdv"
        ref="searchPanel"
        :fields="searchFields"
        @search="onAdvSearch"
        @reset="onAdvReset"
      />

      <DataTable
        ref="tableRef"
        :columns="displayColumns"
        :rows="visibleRows"
        :loading="loading"
        :row-actions="module.rowActions"
        :tree="!!module.tree"
        :action-label="resolveRowActionLabel"
        @action="onTableAction"
        @toggle-expand="onToggleExpand"
        @selection-change="onSelectionChange"
      />

      <div class="pagination">
        <div class="page-info">
          <span>已选择 <b>{{ selected.length }}</b> 条</span>
          <span style="margin-left: 14px;">共 <b>{{ total }}</b> 条</span>
        </div>
        <div class="page-controls">
          <select class="page-size" :value="pageSize" @change="changeSize(+$event.target.value)">
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
          <button class="page-btn" :disabled="page <= 1" @click="gotoPage(page - 1)">‹</button>
          <template v-for="p in pageList" :key="p">
            <button class="page-btn" :class="{ 'is-active': p === page }" @click="gotoPage(p)">{{ p }}</button>
          </template>
          <button class="page-btn" :disabled="page >= totalPages" @click="gotoPage(page + 1)">›</button>
          <span style="font-size: 12.5px; color: var(--muted); margin-left: 6px;">
            前往
            <input
              type="text"
              style="width: 42px; padding: 4px 6px; border: 1px solid var(--line); border-radius: 6px; text-align: center; font-size: 12.5px;"
              :value="page"
              @keydown.enter="gotoPage(+$event.target.value || 1)"
            />
            页
          </span>
        </div>
      </div>
    </section>

    <!-- 新增/编辑弹窗 -->
    <ModalForm
      :visible="modal.visible"
      :title="modal.mode === 'create' ? '新增' + title : '编辑' + title"
      :fields="formFields"
      :record="modal.record"
      :load-field-options="loadFieldOptions"
      @close="modal.visible = false"
      @submit="onModalSubmit"
    />

    <!-- 详情抽屉 -->
    <DetailPanel
      :visible="detail.visible"
      :title="title"
      :fields="detailFields"
      :record="detail.record"
      @close="detail.visible = false"
    />

    <!-- 确认弹窗 -->
    <ConfirmDialog
      :visible="confirm.visible"
      :title="confirm.title"
      :message="confirm.message"
      @confirm="confirm.onOk && confirm.onOk()"
      @cancel="confirm.visible = false"
    />
  </div>
</template>

<style scoped>
.batch-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
