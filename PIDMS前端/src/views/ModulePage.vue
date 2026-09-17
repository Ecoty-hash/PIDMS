<script setup>
import { ref, computed, watch } from "vue";
import { getModule } from "@/config/modules.js";
import { getModuleApi } from "@/api/request.js";
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
    const key = `${src.resource}|${JSON.stringify(src.params || {})}`;
    if (!seen.has(key)) seen.set(key, { fields: [], src });
    seen.get(key).fields.push(f);
  });
  return [...seen.values()];
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
      const cur = record && record[f.name];
      let merged = opts;
      let mergedAll = allOpts;
      // 编辑/回显时：当前值若已不在列表（如已被停用/删除），保留原值可选项
      const needFallback = cur !== null && cur !== undefined && cur !== "";
      if (needFallback && !merged.some((o) => String(o.value) === String(cur))) {
        merged = [{ value: cur, label: cur }, ...merged];
      }
      if (needFallback && !mergedAll.some((o) => String(o.value) === String(cur))) {
        mergedAll = [{ value: cur, label: cur }, ...mergedAll];
      }
      dynamicOpts.value[f.name] = merged;
      displayOpts.value[f.name] = mergedAll;
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

watch(() => props.moduleKey, () => {
  page.value = 1;
  keyword.value = "";
  advFilters.value = {};
  showAdv.value = false;
  dynamicOpts.value = {};
  displayOpts.value = {};
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
  try {
    if (modal.value.mode === "create") {
      await api.value.create(data);
      toast.success(`已新增${title.value}`);
    } else {
      await api.value.update(modal.value.record.id, data);
      toast.success(`已更新${title.value}`);
    }
    modal.value.visible = false;
    load();
  } catch (e) {
    toast.error(e.message || "保存失败");
  }
}

/* ---------- 行操作 ---------- */
async function onTableAction({ name, record }) {
  if (name === "查看") return openDetail(record);
  if (name === "编辑") return openEdit(record);
  if (name === "删除") return askDelete(record);
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
        :rows="rows"
        :loading="loading"
        :row-actions="module.rowActions"
        :action-label="resolveRowActionLabel"
        @action="onTableAction"
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
