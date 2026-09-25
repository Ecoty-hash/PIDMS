<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { progressApi } from "@/api/request.js";
import { useToast } from "@/composables/useToast.js";
import ModalForm from "@/components/ModalForm.vue";
import ConfirmDialog from "@/components/ConfirmDialog.vue";
import StatusBadge from "@/components/StatusBadge.vue";

/**
 * 项目进度详情页：单个项目的进度总览 + 节点管理。
 *
 * 数据来自 GET /api/progress/board?projectId=，一次拿齐：
 *   项目基础信息 + 进度汇总（实际/计划/差值/节点数/已完成/逾期）
 *   + 节点列表（甘特图与列表共用）+ 负责人候选。
 *
 * 页面上的进度、计划进度、逾期判定全部用后端下发的值，前端不另算一套——
 * 后端由 ProjectProgressCalculator 统一口径，和工作台、可视化管理同源。
 * 增删改节点后重新拉 board，甘特图与统计随之刷新。
 */

const route = useRoute();
const router = useRouter();
const toast = useToast();

const projectId = computed(() => {
  const n = Number(route.params.projectId);
  return Number.isFinite(n) ? n : null;
});

const data = ref(null);
const loading = ref(true);

const project = computed(() => data.value?.project || null);
/** 节点树：顶层节点数组，子节点收在各自的 children 里（后端已按层级组好） */
const nodeTree = computed(() => data.value?.nodes || []);

/**
 * 展开/折叠：一份状态同时作用于甘特图与节点列表，两个视图的层级始终一致。
 * 默认全部收起，子节点收纳在总节点下（点箭头展开）。
 */
const expanded = ref(new Set());

/** 按当前展开状态拍平节点树：每项带上 __depth / __hasChildren / __expanded 供模板渲染 */
function flatten(list, depth, out) {
  (list || []).forEach((n) => {
    const key = String(n.id);
    const kids = n.children || [];
    const open = expanded.value.has(key);
    out.push({ ...n, __depth: depth, __hasChildren: kids.length > 0, __expanded: open });
    if (open) flatten(kids, depth + 1, out);
  });
  return out;
}

/** 当前可见的节点（含缩进层级） */
const visibleNodes = computed(() => flatten(nodeTree.value, 0, []));

/** 全部节点（不管展开与否），用于统计口径与查找 */
const allNodes = computed(() => flattenAll(nodeTree.value, 0, []));
function flattenAll(list, depth, out) {
  (list || []).forEach((n) => {
    out.push({ ...n, __depth: depth });
    flattenAll(n.children, depth + 1, out);
  });
  return out;
}

function toggleExpand(node) {
  const key = String(node.id);
  const s = new Set(expanded.value);
  s.has(key) ? s.delete(key) : s.add(key);
  expanded.value = s;
}
function expandAll() {
  expanded.value = new Set(
    allNodes.value.filter((n) => (n.children || []).length > 0).map((n) => String(n.id))
  );
}
function collapseAll() {
  expanded.value = new Set();
}

/** 是否有子节点（没有子节点时不显示展开/收起按钮，免得白占位置） */
const hasTree = computed(() => allNodes.value.some((n) => (n.children || []).length > 0));

/** 按 id 找节点（含子节点） */
function findNode(id) {
  return allNodes.value.find((n) => String(n.id) === String(id)) || null;
}

/** 选中某个节点时的「缩进标签」，用于下拉与折叠提示 */
function indentLabel(level, name) {
  return (level > 0 ? "　".repeat(level) + "└ " : "") + name;
}

/** 节点状态：固定三值枚举（与后端 cm_progress.progress_status 对齐） */
const STATUS_OPTIONS = [
  { value: "in-progress", label: "进行中" },
  { value: "completed", label: "已完成" },
  { value: "delayed", label: "延期" },
];

/** 负责人候选：后端给的在职内部人员名单（本项目负责人 / 成员排在名单最前） */
const responsibleOptions = computed(() =>
  (data.value?.responsibleOptions || []).map((name) => ({ value: name, label: name }))
);

// ------------------------------------------------------------------ 格式化
const DAY = 86400000;

const STATUS_DONE = "completed";

function num(v, digits = 1) {
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(digits) : (0).toFixed(digits);
}

function pct(v) {
  const n = Number(v) || 0;
  return `${Math.max(0, Math.min(100, n))}%`;
}

function signed(v) {
  const n = Number(v) || 0;
  return (n > 0 ? "+" : "") + n.toFixed(1);
}

function deltaClass(v) {
  const n = Number(v) || 0;
  if (n >= 0) return "is-ok";
  return n <= -10 ? "is-bad" : "is-warn";
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : "—";
}

/** 解析后端下发的 "YYYY-MM-DD"：用本地时间构造，避免时区把日期挪一天 */
function toDate(v) {
  if (!v) return null;
  const s = String(v).slice(0, 10);
  const parts = s.split("-");
  if (parts.length !== 3) return null;
  const [y, m, d] = parts.map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

// ------------------------------------------------------------------ 甘特图
/**
 * 时间轴范围：取所有节点计划起止的最早/最晚，两端各留 3% 余量；
 * 节点都没填计划日期时退回项目的开工/竣工日期。返回 null 表示画不出来。
 */
const scale = computed(() => {
  const dates = [];
  allNodes.value.forEach((n) => {
    // 父节点常常不填自己的计划日期，用后端算好的有效区间（自身日期，缺省取子树最早/最晚）
    const a = toDate(n.effectivePlanStart || n.planStartDate);
    const b = toDate(n.effectivePlanEnd || n.planEndDate);
    if (a) dates.push(a.getTime());
    if (b) dates.push(b.getTime());
  });
  if (!dates.length) {
    const a = toDate(project.value?.startDate);
    const b = toDate(project.value?.completionDate);
    if (a) dates.push(a.getTime());
    if (b) dates.push(b.getTime());
  }
  if (!dates.length) return null;

  let min = new Date(Math.min(...dates));
  let max = new Date(Math.max(...dates));
  const raw = max - min;
  if (raw <= 0) {
    // 所有节点同一天：前后各撑 3 天，条形才看得见
    min = new Date(min.getTime() - 3 * DAY);
    max = new Date(max.getTime() + 3 * DAY);
  } else {
    const pad = raw * 0.03;
    min = new Date(min.getTime() - pad);
    max = new Date(max.getTime() + pad);
  }
  return { min, max, span: max - min };
});

/** 日期 → 时间轴上的百分比位置（0-100） */
function posOf(date) {
  const s = scale.value;
  if (!s || !date) return null;
  return ((date - s.min) / s.span) * 100;
}

/**
 * 刻度：按跨度选月步长，保证刻度数不超过 ~14 个，
 * 长周期项目不会挤成一团。
 */
const ticks = computed(() => {
  const s = scale.value;
  if (!s) return [];
  const spanDays = s.span / DAY;
  const stepMonths = spanDays <= 120 ? 1 : spanDays <= 300 ? 2 : spanDays <= 700 ? 3 : spanDays <= 1400 ? 6 : 12;
  const out = [];
  const d = new Date(s.min.getFullYear(), s.min.getMonth(), 1);
  if (d < s.min) d.setMonth(d.getMonth() + 1);
  while (d <= s.max) {
    out.push({
      label: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
      left: posOf(d),
    });
    d.setMonth(d.getMonth() + stepMonths);
  }
  return out;
});

/** 今天在时间轴上的位置；不在范围内则不画（返回 null） */
const todayLeft = computed(() => {
  const s = scale.value;
  if (!s) return null;
  const t = new Date();
  t.setHours(0, 0, 0, 0);
  if (t < s.min || t > s.max) return null;
  return posOf(t);
});

/** 单个节点的条形位置：计划区间 + 完成度填充比例（父节点用子树的有效区间） */
function barOf(node) {
  const s = scale.value;
  if (!s) return null;
  let a = toDate(node.effectivePlanStart || node.planStartDate);
  let b = toDate(node.effectivePlanEnd || node.planEndDate);
  if (!a && !b) return null;
  if (!a) a = b;
  if (!b) b = a;
  const left = posOf(a);
  const right = posOf(b);
  // 单日节点宽度会是 0，给个最小宽度保证可见
  const width = Math.max(right - left, 0.6);
  const done = Math.max(0, Math.min(100, Number(node.completionPercent) || 0));
  return { left, width, done };
}

/** 甘特图行：当前可见节点 + 预先算好的条形（模板里少调几次函数） */
const rows = computed(() => visibleNodes.value.map((n) => ({ ...n, bar: barOf(n) })));

/** 一根条形上的悬浮说明 */
function barTitle(n) {
  const parts = [`${n.progressName}`];
  if (n.__depth > 0) parts[0] = "└ " + parts[0];
  if (n.childCount) parts.push(`含 ${n.childCount} 个子节点（完成度由子节点按权重汇总）`);
  parts.push(`计划 ${fmtDate(n.effectivePlanStart || n.planStartDate)} → ${fmtDate(n.effectivePlanEnd || n.planEndDate)}`);
  parts.push(`完成度 ${num(n.completionPercent)}%`);
  if (n.expectedPercent !== null && n.expectedPercent !== undefined) {
    parts.push(`期望值（理论进度）${num(n.expectedPercent)}%`);
  }
  if (n.effectiveWeight !== null && n.effectiveWeight !== undefined) {
    parts.push(`占总进度 ${num(n.effectiveWeight)}%${n.weightAuto ? "（自动）" : ""}`);
  }
  if (n.overdue) parts.push(`已逾期 ${n.overdueDays} 天`);
  return parts.join("\n");
}

// ------------------------------------------------------------------ 统计
/**
 * 统计口径与工作台 / 可视化管理一致：只统计叶子节点（父节点由子树汇总，算了会重复计数）。
 * 数字直接用后端 ProjectProgressCalculator 的产物，前端不另算一套。
 */
const stats = computed(() => {
  const total = project.value?.leafCount ?? allNodes.value.length;
  const done = project.value?.completedNodeCount ?? 0;
  const overdue = project.value?.overdueNodeCount ?? 0;
  return { total, done, undone: Math.max(total - done, 0), overdue, top: project.value?.topNodeCount ?? 0 };
});

const overdueNodes = computed(() =>
  allNodes.value
    .filter((n) => n.overdue)
    .sort((a, b) => (b.overdueDays || 0) - (a.overdueDays || 0))
);

const BAR_MAX_H = 96;

/** 完成 / 未完成 柱状图（两根柱，各自高度按最大值等比换算） */
const barItems = computed(() => {
  const s = stats.value;
  const max = Math.max(s.done, s.undone, 1);
  return [
    { label: "已完成", value: s.done, h: Math.round((s.done / max) * BAR_MAX_H), cls: "bc-bar--done" },
    { label: "未完成", value: s.undone, h: Math.round((s.undone / max) * BAR_MAX_H), cls: "bc-bar--todo" },
  ];
});

const barGuides = computed(() => [0, 33, 66, 100].map((p) => ({ p, y: 118 - (BAR_MAX_H * p) / 100 })));

// 环形完成度
const RING_R = 44;
const RING_C = 2 * Math.PI * RING_R;

function ringDash(v) {
  const p = Math.max(0, Math.min(100, Number(v) || 0));
  return `${(RING_C * p) / 100} ${RING_C}`;
}

/** 环形图上的计划进度刻度角度（从 12 点顺时针） */
const planAngle = computed(() => {
  const p = Math.max(0, Math.min(100, Number(project.value?.planProgress) || 0));
  return (360 * p) / 100;
});

// ------------------------------------------------------------------ 数据加载
async function load() {
  if (!projectId.value) {
    loading.value = false;
    data.value = null;
    return;
  }
  loading.value = true;
  try {
    data.value = await progressApi.board(projectId.value);
  } catch (e) {
    data.value = null;
    toast.error(e.message || "项目进度加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(projectId, load);

// ------------------------------------------------------------------ 节点增删改
const modal = ref({ visible: false, title: "新增节点", record: null });
const confirm = ref({ visible: false, title: "删除确认", message: "", onOk: () => {} });

const nodeFields = computed(() => {
  // 负责人候选来自内部人员名单；历史数据里若有已离职/改名的人，保留原值避免编辑时下拉被清空
  const ownerOptions = responsibleOptions.value.slice();
  const current = modal.value.record?.responsiblePerson;
  if (current && !ownerOptions.some((o) => o.value === current)) {
    ownerOptions.unshift({ value: current, label: current });
  }
  // 上级节点候选：同项目节点（后端已排除自身与子孙），由 loadFieldOptions 按需拉取
  const isParent = Boolean(modal.value.record?.id) && hasChildren(modal.value.record.id);
  return [
    {
      name: "parentId",
      label: "上级节点",
      type: "enum",
      emptyLabel: "（顶层节点）",
      hint: "不选则为顶层节点；子节点会收纳在上级节点下，可展开/折叠",
      enumFrom: { path: "/progress/parent-options", loadOnOpen: true },
    },
    { name: "progressName", label: "节点名称", type: "string", required: true, placeholder: "如：车站土建施工" },
    { name: "progressCode", label: "节点编号", type: "string", placeholder: "如：PLAN-ST-001" },
    { name: "planStartDate", label: "计划开始时间", type: "date", required: true },
    { name: "planEndDate", label: "计划结束时间", type: "date", required: true },
    { name: "responsiblePerson", label: "负责人", type: "enum", options: ownerOptions, placeholder: "请选择负责人" },
    {
      name: "weight",
      label: "占总进度%",
      type: "number",
      readonlyWhen: isParent ? "childCount" : "",
      placeholder: "0-100，不填则与同级未填节点平分",
      hint: "只有叶子节点需要填：项目进度 = Σ(占总进度% × 完成度) ÷ Σ权重；父节点的份额由子节点汇总",
    },
    {
      name: "completionPercent",
      label: "完成百分比",
      type: "number",
      readonlyWhen: "completionAuto",
      placeholder: "0-100",
      hint: isParent
        ? "该节点有子节点：完成度由子节点按权重自动汇总，不能手填"
        : "0-100 的整数或小数；节点状态选「已完成」时由系统锁定为 100",
    },
    {
      name: "progressStatus",
      label: "节点状态",
      type: "enum",
      required: true,
      options: STATUS_OPTIONS,
      hint: "选「已完成」会自动记 100% 完成度并补实际结束日期；改回进行中则清空实际结束日期",
    },
    { name: "remark", label: "节点描述", type: "textarea", placeholder: "节点说明 / 主要施工内容" },
  ];
});

/** 该节点是否已有子节点（有子节点时完成度 / 占总进度% 都不可手填） */
function hasChildren(id) {
  const node = findNode(id);
  return Boolean(node && (node.childCount > 0 || (node.children || []).length > 0));
}

/**
 * 上级节点候选：本项目节点（带层级缩进）。
 * 编辑时传 excludeId，后端会把自身与子孙一起排除，避免形成循环层级。
 */
async function loadFieldOptions() {
  if (!projectId.value) return [];
  const list = await progressApi.parentOptions(projectId.value, modal.value.record?.id);
  return (list || []).map((o) => ({ value: o.id, label: indentLabel(o.level || 0, o.name) }));
}

function openCreate() {
  modal.value = { visible: true, title: "新增进度节点", record: null };
}

/** 在某个节点下新增子节点：上级节点预填为该节点 */
function openCreateChild(node) {
  modal.value = {
    visible: true,
    title: `新增「${node.progressName}」的子节点`,
    record: { parentId: node.id },
  };
}

function openEdit(node) {
  modal.value = { visible: true, title: "编辑进度节点", record: node };
}

async function submitNode(form) {
  const start = toDate(form.planStartDate);
  const end = toDate(form.planEndDate);
  if (start && end && end < start) {
    toast.error("计划结束时间不能早于计划开始时间");
    return;
  }
  const numOrNull = (v) =>
    v === "" || v === null || v === undefined ? null : Number(v);

  const payload = {
    projectId: projectId.value,
    progressName: form.progressName,
    progressCode: form.progressCode || null,
    planStartDate: form.planStartDate || null,
    planEndDate: form.planEndDate || null,
    responsiblePerson: form.responsiblePerson || null,
    progressStatus: form.progressStatus,
    remark: form.remark || null,
  };
  // 只读字段（父节点的「完成百分比」「占总进度%」）弹窗提交时会整键删掉，
  // 这里不能再补成 null：null 在更新接口里是「不修改」，但写成 null 容易被误读成「清空」
  if ("completionPercent" in form) payload.completionPercent = numOrNull(form.completionPercent);
  if ("weight" in form) payload.weight = numOrNull(form.weight);

  const editing = modal.value.record?.id;
  // 上级节点：选了就带上 parentId；留空表示顶层节点（编辑接口把 null 当作「不修改」，要显式带 topLevel）
  const parentId = form.parentId === "" || form.parentId === null || form.parentId === undefined
    ? null : Number(form.parentId);
  if (parentId !== null) {
    payload.parentId = parentId;
  } else if (editing) {
    payload.topLevel = true;
  }

  try {
    let nodeId = editing;
    if (editing) {
      await progressApi.update(editing, payload);
      toast.success("节点已更新");
    } else {
      const created = await progressApi.create(payload);
      nodeId = typeof created === "number" ? created : null;
      toast.success("节点已创建");
      // 新增子节点后自动展开上级节点，免得新节点藏在收起的树里找不到
      if (parentId !== null) {
        expanded.value = new Set([...expanded.value, String(parentId)]);
      }
    }
    modal.value.visible = false;
    await load(); // 图表与统计随节点一起刷新
    verifyOwnerSaved(nodeId, payload);
  } catch (e) {
    toast.error(e.message || "保存失败");
  }
}

/**
 * 保存后回查节点负责人是否真的落库。
 * 库结构比代码旧（缺 responsible_person 列）或后端没重新构建重启时，负责人会被静默丢掉，
 * 界面上只表现为「填了还是 —」，很难定位。这里当场把成因点出来，不让它再无声失败。
 */
function verifyOwnerSaved(nodeId, payload) {
  if (!payload.responsiblePerson) return; // 本次就是留空，无需回查
  const saved = allNodes.value.find((n) =>
    nodeId
      ? n.id === nodeId
      : n.progressName === payload.progressName && n.planStartDate === payload.planStartDate
  );
  if (!saved || saved.responsiblePerson === payload.responsiblePerson) return;
  toast.error(
    `节点负责人「${payload.responsiblePerson}」没有写进数据库：` +
      "请确认后端已重新构建并重启，且已执行 database/upgrade/13_add_progress_responsible_person.sql"
  );
}

/** 列表内直接改节点完成状态：只提交状态，完成度与实际结束日期由后端按口径联动 */
async function changeStatus(node, status) {
  if (status === node.progressStatus) return;
  try {
    await progressApi.update(node.id, { progressStatus: status });
    toast.success(`「${node.progressName}」已改为${STATUS_OPTIONS.find((o) => o.value === status)?.label || status}`);
    await load();
  } catch (e) {
    toast.error(e.message || "状态修改失败");
    await load(); // 失败时把下拉还原成库里的值
  }
}

function askRemove(node) {
  const kids = node.childCount || 0;
  confirm.value = {
    visible: true,
    title: "删除节点",
    message: kids
      ? `「${node.progressName}」下还有 ${kids} 个子节点，删除时会一并删除，且不再计入项目进度，此操作不可恢复。`
      : `确定删除节点「${node.progressName}」吗？删除后该节点不再计入项目进度，此操作不可恢复。`,
    onOk: async () => {
      try {
        await progressApi.remove(node.id);
        toast.success("已删除");
        confirm.value.visible = false;
        await load();
      } catch (e) {
        toast.error(e.message || "删除失败");
      }
    },
  };
}

function goBack() {
  router.push("/project");
}
</script>

<template>
  <div class="rise">
    <div class="page-head pp-head">
      <div>
        <div class="page-head__eyebrow">PIDMS · PROJECT SCHEDULE</div>
        <h1 class="page-head__title">项目进度</h1>
      </div>
      <div class="pp-head__right">
        <button class="btn btn--sm" @click="goBack">← 返回项目列表</button>
        <button class="btn btn--sm" :disabled="loading" @click="load">刷新</button>
        <template v-if="hasTree">
          <button class="btn btn--sm" @click="expandAll">展开全部</button>
          <button class="btn btn--sm" @click="collapseAll">收起全部</button>
        </template>
        <button class="btn btn--sm btn--primary" :disabled="!project" @click="openCreate">新增节点</button>
      </div>
    </div>

    <div v-if="loading" class="loading"><span class="spinner"></span>加载项目进度…</div>

    <template v-else-if="project">
      <!-- ================= 项目基础信息 ================= -->
      <section class="pp-card">
        <div class="pp-card__main">
          <div class="pp-card__eyebrow">PROJECT</div>
          <h2 class="pp-card__name">{{ project.projectName }}</h2>
          <div class="pp-meta-grid">
            <div class="pp-meta">
              <span class="pp-meta__k">项目编号</span>
              <span class="pp-meta__v pp-mono">{{ project.projectCode || "—" }}</span>
            </div>
            <div class="pp-meta">
              <span class="pp-meta__k">项目负责人</span>
              <span class="pp-meta__v">{{ project.projectLeader || "未指定" }}</span>
            </div>
            <div class="pp-meta">
              <span class="pp-meta__k">起止时间</span>
              <span class="pp-meta__v pp-mono">
                {{ fmtDate(project.startDate) }} → {{ fmtDate(project.completionDate) }}
              </span>
            </div>
            <div class="pp-meta">
              <span class="pp-meta__k">项目状态</span>
              <span class="pp-meta__v"><StatusBadge :value="project.projectStatusName" /></span>
            </div>
          </div>
        </div>

        <div class="pp-card__ring">
          <div class="ring-wrap">
            <svg viewBox="0 0 120 120" class="ring" role="img"
                 :aria-label="`总体实际进度 ${num(project.actualProgress)}%，计划进度 ${num(project.planProgress)}%`">
              <circle cx="60" cy="60" :r="RING_R" class="ring__bg" />
              <circle cx="60" cy="60" :r="RING_R" class="ring__val"
                      :stroke-dasharray="ringDash(project.actualProgress)"
                      transform="rotate(-90 60 60)" />
              <line x1="60" y1="11" x2="60" y2="17" class="ring__plan"
                    :transform="`rotate(${planAngle} 60 60)`" />
            </svg>
            <div class="ring-center">
              <div class="ring-num">{{ num(project.actualProgress) }}<small>%</small></div>
              <div class="ring-cap">实际进度</div>
            </div>
          </div>
          <div class="ring-foot">
            <div class="ring-foot__row">
              <span>计划进度</span>
              <b class="pp-mono">{{ num(project.planProgress) }}%</b>
            </div>
            <div class="ring-foot__row">
              <span>期望值（理论）</span>
              <b class="pp-mono">{{ num(project.expectedProgress) }}%</b>
            </div>
            <div class="ring-foot__row">
              <span>进度差</span>
              <b class="pp-mono" :class="deltaClass(project.progressDelta)">{{ signed(project.progressDelta) }}</b>
            </div>
            <div class="ring-foot__row">
              <span>节点</span>
              <b class="pp-mono">{{ stats.done }} / {{ stats.total }} 已完成</b>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= 甘特图 ================= -->
      <section class="panel mt20">
        <div class="panel__head">
          <div>
            <div class="panel__eyebrow">SCHEDULE</div>
            <h2 class="panel__title">节点甘特图</h2>
          </div>
          <div class="legend-inline">
            <span><i class="lg-track"></i>计划工期</span>
            <span><i class="lg-fill"></i>已完成部分</span>
            <span><i class="lg-today"></i>今天</span>
            <span><i class="lg-over"></i>逾期节点</span>
          </div>
        </div>
        <div class="panel__body">
          <div v-if="!visibleNodes.length" class="empty">还没有进度节点，点右上角「新增节点」创建第一个</div>
          <div v-else-if="!scale" class="empty">节点都没填计划起止日期，无法绘制甘特图</div>
          <div v-else class="gantt">
            <div class="gantt__hd">
              <div class="gantt__hd-cell">节点 / 负责人</div>
              <div class="gantt__axis">
                <span
                  v-for="t in ticks"
                  :key="t.label"
                  class="gantt__tick"
                  :style="{ left: t.left + '%' }"
                >{{ t.label }}</span>
              </div>
            </div>

            <div v-for="r in rows" :key="r.id" class="gantt__row" :class="{ 'is-child': r.__depth > 0 }">
              <div class="gantt__cell" :style="{ paddingLeft: r.__depth * 18 + 'px' }">
                <div class="gantt__name-row">
                  <button
                    v-if="r.__hasChildren"
                    class="tree-toggle"
                    :aria-expanded="r.__expanded ? 'true' : 'false'"
                    :title="r.__expanded ? '收起子节点' : '展开子节点'"
                    @click="toggleExpand(r)"
                  >{{ r.__expanded ? "▾" : "▸" }}</button>
                  <span v-else class="tree-leaf" aria-hidden="true">·</span>
                  <span class="gantt__name" :title="r.progressName">{{ r.progressName }}</span>
                  <span v-if="r.childCount" class="gantt__kids">{{ r.childCount }} 子</span>
                </div>
                <div class="gantt__who">
                  {{ r.responsiblePerson || "未指定负责人" }}
                  <span v-if="r.effectiveWeight !== null && r.effectiveWeight !== undefined" class="gantt__w">
                    · 占总进度 {{ num(r.effectiveWeight) }}%
                  </span>
                </div>
              </div>
              <div class="gantt__track">
                <i v-for="t in ticks" :key="t.label" class="gantt__grid" :style="{ left: t.left + '%' }"></i>
                <i v-if="todayLeft !== null" class="gantt__today" :style="{ left: todayLeft + '%' }"></i>
                <span
                  v-if="r.bar"
                  class="gantt__bar"
                  :class="{ 'is-overdue': r.overdue, 'is-done': r.completed }"
                  :style="{ left: r.bar.left + '%', width: r.bar.width + '%' }"
                  :title="barTitle(r)"
                >
                  <i class="gantt__fill" :style="{ width: r.bar.done + '%' }"></i>
                </span>
                <span v-else class="gantt__nodate">未填计划日期</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= 配套统计 ================= -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">NODE STATS</div>
              <h2 class="panel__title">节点完成情况</h2>
            </div>
            <span class="tag" :class="stats.overdue ? 'tag--red' : 'tag--gray'">
              逾期 {{ stats.overdue }} 个
            </span>
          </div>
          <div class="panel__body">
            <div v-if="!stats.total" class="empty">还没有进度节点</div>
            <div v-else class="bc-wrap">
              <svg viewBox="0 0 240 140" class="barchart" role="img"
                   :aria-label="`节点完成情况：已完成 ${stats.done} 个，未完成 ${stats.undone} 个`">
                <line v-for="g in barGuides" :key="g.p" x1="34" :y1="g.y" x2="226" :y2="g.y" class="bc-grid" />
                <line x1="34" y1="118" x2="226" y2="118" class="bc-axis" />
                <g v-for="(b, i) in barItems" :key="b.label">
                  <rect
                    :x="46 + i * 90"
                    :y="118 - b.h"
                    width="52"
                    :height="Math.max(b.h, 2)"
                    rx="4"
                    :class="b.cls"
                  />
                  <text :x="72 + i * 90" :y="112 - b.h" class="bc-val">{{ b.value }}</text>
                  <text :x="72 + i * 90" y="134" class="bc-label">{{ b.label }}</text>
                </g>
              </svg>
              <div class="bc-legend">
                <span>共 <b class="pp-mono">{{ stats.total }}</b> 个节点</span>
                <span>完成率 <b class="pp-mono">{{ stats.total ? num((stats.done / stats.total) * 100) : "0.0" }}%</b></span>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">OVERDUE</div>
              <h2 class="panel__title">逾期节点</h2>
            </div>
          </div>
          <div class="panel__body">
            <div v-if="!overdueNodes.length" class="empty">没有逾期节点，进度均按计划推进</div>
            <div v-else class="risk-list">
              <div v-for="n in overdueNodes" :key="n.id" class="risk">
                <div class="risk__main">
                  <div class="risk__name">{{ n.progressName }}</div>
                  <div class="risk__sub">
                    {{ n.responsiblePerson || "未指定负责人" }} · 计划结束 {{ fmtDate(n.effectivePlanEnd || n.planEndDate) }}
                  </div>
                </div>
                <div class="risk__meta">
                  <span class="risk__days">逾期 {{ n.overdueDays }} 天</span>
                  <span class="risk__pct">完成 {{ num(n.completionPercent) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- ================= 节点列表 ================= -->
      <section class="panel mt20">
        <div class="panel__head">
          <div>
            <div class="panel__eyebrow">NODES</div>
            <h2 class="panel__title">进度节点</h2>
          </div>
          <div class="pp-head__right">
            <span class="pp-count">
              共 {{ stats.total }} 个节点<template v-if="stats.top && stats.top !== stats.total">（{{ stats.top }} 个顶层）</template>
            </span>
            <template v-if="hasTree">
              <button class="btn btn--sm" @click="expandAll">展开全部</button>
              <button class="btn btn--sm" @click="collapseAll">收起全部</button>
            </template>
            <button class="btn btn--sm btn--primary" @click="openCreate">新增节点</button>
          </div>
        </div>
        <div class="panel__body">
          <div v-if="!visibleNodes.length" class="empty">还没有进度节点，点「新增节点」创建第一个</div>
          <div v-else class="pp-tablewrap">
            <table class="table">
              <thead>
                <tr>
                  <th>节点名称</th>
                  <th>负责人</th>
                  <th>计划开始</th>
                  <th>计划结束</th>
                  <th>实际结束</th>
                  <th>占总进度%</th>
                  <th>完成度</th>
                  <th>期望值</th>
                  <th>节点状态</th>
                  <th>逾期</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="n in visibleNodes" :key="n.id" :class="{ 'is-child': n.__depth > 0 }">
                  <td>
                    <div class="pp-node-row" :style="{ paddingLeft: n.__depth * 18 + 'px' }">
                      <button
                        v-if="n.__hasChildren"
                        class="tree-toggle"
                        :aria-expanded="n.__expanded ? 'true' : 'false'"
                        :title="n.__expanded ? '收起子节点' : '展开子节点'"
                        @click="toggleExpand(n)"
                      >{{ n.__expanded ? "▾" : "▸" }}</button>
                      <span v-else class="tree-leaf" aria-hidden="true">·</span>
                      <div>
                        <div class="pp-node-name">{{ n.progressName }}</div>
                        <div v-if="n.progressCode || n.remark" class="pp-node-sub">
                          <span v-if="n.progressCode" class="pp-mono">{{ n.progressCode }}</span>
                          <span v-if="n.remark">{{ n.remark }}</span>
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>{{ n.responsiblePerson || "未指定" }}</td>
                  <td class="mono">{{ fmtDate(n.effectivePlanStart || n.planStartDate) }}</td>
                  <td class="mono">{{ fmtDate(n.effectivePlanEnd || n.planEndDate) }}</td>
                  <td class="mono">{{ fmtDate(n.actualEndDate) }}</td>
                  <td class="mono">
                    <template v-if="n.effectiveWeight !== null && n.effectiveWeight !== undefined">
                      {{ num(n.effectiveWeight) }}%
                      <span v-if="n.weightAuto" class="tag tag--auto" title="由子节点汇总或按剩余权重平分">自动</span>
                    </template>
                    <span v-else>—</span>
                  </td>
                  <td>
                    <div class="mini">
                      <div class="mini__bar" :title="`完成度 ${num(n.completionPercent)}%`">
                        <i :style="{ width: pct(n.completionPercent) }"></i>
                      </div>
                      <span class="mini__num pp-mono">{{ num(n.completionPercent) }}%</span>
                      <span v-if="n.completionAuto" class="tag tag--auto" title="由子节点按权重汇总">自动</span>
                    </div>
                  </td>
                  <td class="mono">{{ num(n.expectedPercent) }}%</td>
                  <td>
                    <select
                      class="st-select"
                      :class="'is-' + n.progressStatus"
                      :value="n.progressStatus"
                      @change="changeStatus(n, $event.target.value)"
                    >
                      <option v-for="o in STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
                    </select>
                  </td>
                  <td>
                    <span v-if="n.overdue" class="tag tag--red">{{ n.overdueDays }} 天</span>
                    <span v-else class="tag tag--gray">—</span>
                  </td>
                  <td class="cell--actions">
                    <a @click="openCreateChild(n)">＋子节点</a>
                    <a class="sep">|</a>
                    <a @click="openEdit(n)">编辑</a>
                    <a class="sep">|</a>
                    <a class="danger" @click="askRemove(n)">删除</a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <p class="pp-note">{{ data.description }}</p>
    </template>

    <div v-else class="empty">项目进度加载失败，请返回项目列表重试</div>

    <ModalForm
      :visible="modal.visible"
      :title="modal.title"
      :fields="nodeFields"
      :record="modal.record"
      :load-field-options="loadFieldOptions"
      @close="modal.visible = false"
      @submit="submitNode"
    />

    <ConfirmDialog
      :visible="confirm.visible"
      :title="confirm.title"
      :message="confirm.message"
      confirm-text="删除"
      @confirm="confirm.onOk"
      @cancel="confirm.visible = false"
    />
  </div>
</template>

<style scoped>
.pp-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.pp-head__right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.mt20 { margin-top: 20px; }
.pp-mono { font-family: var(--mono); }

/* ---------- 项目基础信息卡 ---------- */
.pp-card {
  display: grid; grid-template-columns: 1fr auto; gap: 24px; align-items: center;
  background: var(--card); border: 1px solid var(--line); border-left: 3px solid var(--blue);
  border-radius: var(--radius); padding: 20px 24px; box-shadow: var(--shadow-sm);
}
.pp-card__eyebrow {
  font-size: 10px; letter-spacing: 1.5px; color: var(--muted);
  font-family: var(--mono); margin-bottom: 6px;
}
.pp-card__name {
  font-family: var(--num); font-size: 21px; font-weight: 600; color: var(--ink);
  margin: 0 0 14px; letter-spacing: -.2px; line-height: 1.3;
}
.pp-meta-grid { display: grid; grid-template-columns: repeat(4, minmax(120px, 1fr)); gap: 14px 20px; }
.pp-meta { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.pp-meta__k { font-size: 10.5px; letter-spacing: .6px; color: var(--muted); font-family: var(--mono); text-transform: uppercase; }
.pp-meta__v { font-size: 13.5px; color: var(--ink); font-weight: 500; }

.pp-card__ring { display: flex; align-items: center; gap: 20px; flex-shrink: 0; }
.ring-wrap { position: relative; width: 132px; height: 132px; }
.ring { width: 132px; height: 132px; }
.ring__bg { fill: none; stroke: var(--grid); stroke-width: 10; }
.ring__val {
  fill: none; stroke: var(--blue); stroke-width: 10; stroke-linecap: round;
  transition: stroke-dasharray .6s cubic-bezier(.2, .8, .2, 1);
}
.ring__plan { stroke: var(--amber); stroke-width: 2.5; stroke-linecap: round; }
.ring-center {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 2px;
}
.ring-num { font-family: var(--num); font-size: 26px; font-weight: 600; color: var(--ink); line-height: 1; }
.ring-num small { font-size: 12px; color: var(--muted); margin-left: 1px; }
.ring-cap { font-size: 11px; color: var(--muted); }
.ring-foot { display: flex; flex-direction: column; gap: 8px; min-width: 116px; }
.ring-foot__row {
  display: flex; align-items: baseline; justify-content: space-between; gap: 12px;
  font-size: 12px; color: var(--muted); border-bottom: 1px dashed var(--line); padding-bottom: 6px;
}
.ring-foot__row:last-child { border-bottom: none; padding-bottom: 0; }
.ring-foot__row b { font-size: 13px; color: var(--ink); font-weight: 600; }
.ring-foot__row b.is-ok { color: var(--green); }
.ring-foot__row b.is-warn { color: var(--amber); }
.ring-foot__row b.is-bad { color: var(--red); }

/* ---------- 甘特图 ---------- */
.gantt { --label-w: 232px; }
.gantt__hd {
  display: grid; grid-template-columns: var(--label-w) 1fr;
  border-bottom: 1px solid var(--line); padding-bottom: 7px; margin-bottom: 2px;
}
.gantt__hd-cell {
  font-size: 10.5px; letter-spacing: .8px; color: var(--muted);
  font-family: var(--mono); text-transform: uppercase;
}
.gantt__axis { position: relative; height: 16px; }
.gantt__tick {
  position: absolute; top: 0; transform: translateX(-50%);
  font-size: 11px; color: var(--muted); font-family: var(--mono); white-space: nowrap;
}
.gantt__tick:first-child { transform: none; }
.gantt__row {
  display: grid; grid-template-columns: var(--label-w) 1fr;
  align-items: center; border-bottom: 1px solid var(--line);
}
.gantt__row:last-child { border-bottom: none; }
.gantt__cell { padding: 9px 16px 9px 0; min-width: 0; }
.gantt__name-row { display: flex; align-items: center; gap: 5px; min-width: 0; }
.gantt__name {
  font-size: 13px; color: var(--ink); font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.gantt__who { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
.gantt__w { font-family: var(--mono); }
/* 子节点行：左侧一道浅色竖线，一眼看出归属哪个总节点 */
.gantt__row.is-child { background: rgba(29, 111, 209, .028); }
.gantt__row.is-child .gantt__name { font-weight: 400; color: var(--steel); }
.gantt__kids {
  flex-shrink: 0; font-size: 10.5px; font-family: var(--mono); color: var(--muted);
  background: rgba(138, 153, 171, .14); border-radius: 4px; padding: 1px 5px;
}

/* 展开/折叠箭头（甘特图与节点列表共用一套样式） */
.tree-toggle {
  flex-shrink: 0; width: 16px; height: 16px; padding: 0;
  border: 1px solid var(--line); border-radius: 4px;
  background: var(--card); color: var(--steel);
  font-size: 10px; line-height: 1; cursor: pointer;
}
.tree-toggle:hover { border-color: var(--blue); color: var(--blue); background: var(--blue-soft); }
.tree-leaf { flex-shrink: 0; display: inline-block; width: 16px; text-align: center; color: var(--line-strong); }
/* 「自动」标记：该值由子节点汇总得出，不是手填 */
.tag--auto { margin-left: 6px; padding: 1px 5px; font-size: 10.5px; background: rgba(138, 153, 171, .14); color: var(--muted); }
.tag--auto::before { display: none; }
.gantt__track { position: relative; height: 36px; }
.gantt__grid { position: absolute; top: 4px; bottom: 4px; width: 1px; background: var(--grid); z-index: 0; }
.gantt__today { position: absolute; top: 0; bottom: 0; width: 1px; background: var(--amber); opacity: .8; z-index: 2; }
.gantt__bar {
  position: absolute; top: 50%; transform: translateY(-50%); height: 13px;
  border-radius: 7px; background: var(--blue-soft);
  border: 1px solid rgba(29, 111, 209, .3);
  overflow: hidden; z-index: 1; cursor: default;
}
.gantt__bar.is-overdue { background: var(--red-soft); border-color: rgba(194, 64, 46, .38); }
.gantt__bar.is-done { background: var(--green-soft); border-color: rgba(30, 142, 94, .35); }
.gantt__fill { display: block; height: 100%; background: var(--blue); }
.gantt__bar.is-overdue .gantt__fill { background: var(--red); }
.gantt__bar.is-done .gantt__fill { background: var(--green); }
.gantt__nodate {
  position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  font-size: 11.5px; color: var(--muted);
}

.legend-inline { display: flex; gap: 14px; font-size: 11.5px; color: var(--muted); flex-wrap: wrap; }
.legend-inline i { display: inline-block; vertical-align: middle; margin-right: 5px; }
.lg-track { width: 16px; height: 8px; border-radius: 4px; background: var(--blue-soft); border: 1px solid rgba(29, 111, 209, .3); }
.lg-fill { width: 16px; height: 8px; border-radius: 4px; background: var(--blue); }
.lg-today { width: 2px; height: 12px; background: var(--amber); }
.lg-over { width: 16px; height: 8px; border-radius: 4px; background: var(--red); }

/* ---------- 柱状图 ---------- */
.bc-wrap { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.barchart { width: 100%; max-width: 340px; height: auto; }
.bc-grid { stroke: var(--grid); stroke-width: 1; }
.bc-axis { stroke: var(--line-strong); stroke-width: 1; }
.bc-bar--done { fill: var(--green); }
.bc-bar--todo { fill: #b8c6d6; }
.bc-val { font-family: var(--num); font-size: 15px; font-weight: 600; fill: var(--ink); text-anchor: middle; }
.bc-label { font-size: 11.5px; fill: var(--muted); text-anchor: middle; }
.bc-legend { display: flex; gap: 20px; font-size: 12px; color: var(--muted); }
.bc-legend b { color: var(--ink); font-weight: 600; }

/* ---------- 逾期清单 ---------- */
.risk-list { display: flex; flex-direction: column; }
.risk {
  display: flex; align-items: center; gap: 12px; padding: 9px 0 9px 12px;
  border-bottom: 1px solid var(--line); position: relative;
}
.risk:last-child { border-bottom: none; }
.risk::before {
  content: ""; position: absolute; left: 0; top: 11px; bottom: 11px; width: 3px;
  border-radius: 2px; background: var(--red);
}
.risk__main { flex: 1; min-width: 0; }
.risk__name {
  font-size: 13.5px; font-weight: 600; color: var(--ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.risk__sub {
  font-size: 11.5px; color: var(--muted); margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.risk__meta { text-align: right; flex-shrink: 0; }
.risk__days { display: block; font-family: var(--num); font-weight: 600; font-size: 14px; color: var(--red); }
.risk__pct { font-size: 11px; color: var(--muted); font-family: var(--mono); }

/* ---------- 节点列表 ---------- */
.pp-count { font-size: 12px; color: var(--muted); }
.pp-tablewrap { overflow-x: auto; }
.pp-node-row { display: flex; align-items: flex-start; gap: 5px; min-width: 0; }
tr.is-child { background: rgba(29, 111, 209, .028); }
.pp-node-name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
tr.is-child .pp-node-name { font-weight: 500; color: var(--steel); }
.pp-node-sub { display: flex; gap: 10px; font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.pp-node-sub span { max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.mini { display: flex; align-items: center; gap: 8px; }
.mini__bar { position: relative; width: 64px; height: 7px; border-radius: 4px; background: var(--grid); flex-shrink: 0; }
.mini__bar i {
  position: absolute; left: 0; top: 0; bottom: 0; border-radius: 4px;
  background: linear-gradient(90deg, var(--blue), #3b86e0);
}
.mini__num { font-size: 12px; color: var(--ink); }

.st-select {
  border: 1px solid var(--line); border-radius: 6px; padding: 3px 7px;
  font-size: 12px; background: var(--card); cursor: pointer; font-family: inherit;
}
.st-select:focus { outline: none; border-color: var(--blue); box-shadow: var(--ring); }
.st-select.is-in-progress { color: var(--blue); border-color: rgba(29, 111, 209, .35); background: var(--blue-soft); }
.st-select.is-completed { color: var(--green); border-color: rgba(30, 142, 94, .35); background: var(--green-soft); }
.st-select.is-delayed { color: var(--red); border-color: rgba(194, 64, 46, .35); background: var(--red-soft); }

.pp-note {
  margin: 18px 2px 0; font-size: 11.5px; color: var(--muted);
  line-height: 1.7; font-family: var(--mono);
}

/* ---------- 响应式 ---------- */
@media (max-width: 1180px) {
  .pp-card { grid-template-columns: 1fr; }
  .pp-card__ring { justify-content: flex-start; }
  .pp-meta-grid { grid-template-columns: repeat(2, minmax(120px, 1fr)); }
}
@media (max-width: 1080px) {
  .grid-2 { grid-template-columns: 1fr; }
}
@media (max-width: 860px) {
  .gantt { --label-w: 148px; }
  .pp-meta-grid { grid-template-columns: 1fr; }
}
</style>
