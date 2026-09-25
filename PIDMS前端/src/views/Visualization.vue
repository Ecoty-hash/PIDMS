<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { dashboardApi } from "@/api/request.js";
import { useToast } from "@/composables/useToast.js";

/**
 * 可视化管理（全局视角）：看全公司的项目盘子。
 * 数据来自 /api/visualization/*，全部为后端实时聚合（无 mock）：
 *  - dashboard        → 规模、总体进度（实际 vs 计划）、状态分布、分项目进度对比
 *  - progress-trend   → 计划 vs 实际 进度趋势（日/周/月）
 *  - inspection-stats → 质量/安全检查合格率与整改闭环率
 *  - key-nodes        → 关键节点时间线（可按项目筛选）
 * 图表全部为手写内联 SVG（保持蓝图纸设计语言，无外部图表库、无网络请求）。
 */
const toast = useToast();

const hero = ref(null);
const rates = ref(null);
const trend = ref({ points: [] });
const nodes = ref([]);
const gran = ref("week");
const nodeFilter = ref("");
const loading = ref(true);
const nodeLoading = ref(false);

/**
 * 关键节点：后端按层级组好树（顶层节点 + children），大屏默认只展开顶层，
 * 点箭头可以展开查看子节点各自的完成度、占总进度% 与期望值。
 */
const expanded = ref(new Set());

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

const visibleNodes = computed(() => flatten(nodes.value, 0, []));
const hasTree = computed(() => visibleNodes.value.some((n) => n.__hasChildren));

function toggleExpand(n) {
  const key = String(n.id);
  const s = new Set(expanded.value);
  s.has(key) ? s.delete(key) : s.add(key);
  expanded.value = s;
}
function expandAll() {
  const ids = [];
  const walk = (list) =>
    (list || []).forEach((n) => {
      if ((n.children || []).length) {
        ids.push(String(n.id));
        walk(n.children);
      }
    });
  walk(nodes.value);
  expanded.value = new Set(ids);
}
function collapseAll() {
  expanded.value = new Set();
}
/** 项目状态字典编码 → 颜色（字典是动态的，未知编码走兜底色） */
const STATUS_COLORS = {
  planning: "#d98c0b",
  "in-progress": "#1d6fd1",
  completed: "#1e8e5e",
  suspended: "#8a99ab",
  terminated: "#c2402e",
  unknown: "#c3d0df",
};
const FALLBACK_COLORS = ["#4f8fd9", "#6aa3c9", "#8a99ab", "#d98c0b", "#c2402e", "#1e8e5e"];

const NODE_TAG = {
  completed: ["green", "已完成"],
  current: ["blue", "进行中"],
  pending: ["gray", "未开始"],
};

function esc(s) {
  return String(s == null ? "" : s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function num(v, digits = 1) {
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(digits) : (0).toFixed(digits);
}

const clamp = (v) => Math.max(0, Math.min(100, Number(v) || 0));
const pct = (v) => clamp(v) + "%";

function signed(v) {
  const n = Number(v) || 0;
  return (n > 0 ? "+" : "") + n.toFixed(1);
}

function deltaClass(v) {
  const n = Number(v) || 0;
  if (n >= 0) return "is-ok";
  return n <= -10 ? "is-bad" : "is-warn";
}

function statusColor(code, index) {
  return STATUS_COLORS[code] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : "—";
}

/* ---------------- 手写 SVG ---------------- */

/** 结构柱进度仪：本项目的签名视觉，柱内实心为实际进度，琥珀虚线为计划进度 */
function columnGauge(actual, plan) {
  const H = 186, W = 232, top = 10, bottom = H - 10, colX = 88, colW = 46;
  const fill = clamp(actual);
  const fillH = ((bottom - top) * fill) / 100;
  const fillY = bottom - fillH;
  const planY = bottom - ((bottom - top) * clamp(plan)) / 100;

  let svg = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="总体实际进度 ${fill}%，计划进度 ${clamp(plan)}%">`;
  svg += `<g stroke="rgba(29,111,209,.09)" stroke-width="1">`;
  for (let i = 0; i <= 4; i++) {
    const y = top + ((bottom - top) * i) / 4;
    svg += `<line x1="6" y1="${y}" x2="${W - 6}" y2="${y}"/>`;
  }
  svg += `</g>`;
  svg += `<g font-family="Consolas, monospace" font-size="9" fill="#8a99ab" text-anchor="end">`;
  [0, 25, 50, 75, 100].forEach((t) => {
    const y = bottom - ((bottom - top) * t) / 100;
    svg += `<line x1="${colX - 12}" y1="${y}" x2="${colX}" y2="${y}" stroke="#8a99ab" stroke-width="1"/>`;
    svg += `<text x="${colX - 16}" y="${y + 3}">${t}</text>`;
  });
  svg += `</g>`;
  svg += `<rect x="${colX}" y="${top}" width="${colW}" height="${bottom - top}" fill="none" stroke="#1d6fd1" stroke-opacity=".55" stroke-width="1.2"/>`;
  if (fillH > 0) {
    svg += `<defs><linearGradient id="vzCol" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2f83dd"/><stop offset="1" stop-color="#1559ad"/></linearGradient></defs>`;
    svg += `<rect x="${colX + 1.5}" y="${fillY}" width="${colW - 3}" height="${fillH}" fill="url(#vzCol)" opacity=".9"/>`;
  }
  // 计划进度：琥珀虚线 + 左右箭标
  svg += `<line x1="${colX - 8}" y1="${planY}" x2="${colX + colW + 8}" y2="${planY}" stroke="#d98c0b" stroke-width="1.3" stroke-dasharray="5 3"/>`;
  svg += `<path d="M ${colX - 12} ${planY - 4} l 4 4 l -4 4" fill="none" stroke="#d98c0b" stroke-width="1.2"/>`;
  svg += `<path d="M ${colX + colW + 12} ${planY - 4} l -4 4 l 4 4" fill="none" stroke="#d98c0b" stroke-width="1.2"/>`;
  svg += `<line x1="${colX - 4}" y1="${fillY}" x2="${colX + colW + 4}" y2="${fillY}" stroke="#16324f" stroke-width="1.6"/>`;
  // 柱顶基准标记
  svg += `<g stroke="#1d6fd1" stroke-opacity=".55" stroke-width="1"><circle cx="${colX + colW / 2}" cy="${top - 6}" r="5" fill="none"/><path d="M ${colX + colW / 2} ${top - 13} v 14 M ${colX + colW / 2 - 7} ${top - 6} h 14"/></g>`;
  svg += `<text x="${colX + colW + 16}" y="${fillY + 4}" fill="#16324f" font-family="Bahnschrift, Segoe UI, sans-serif" font-size="19" font-weight="600">${fill.toFixed(1)}%</text>`;
  svg += `</svg>`;
  return svg;
}

/** 状态分布环形图 */
function donut(segments) {
  const R = 62, C = 2 * Math.PI * R, cx = 84, cy = 84;
  let svg = `<svg viewBox="0 0 168 168" role="img" aria-label="项目状态分布">`;
  svg += `<circle cx="${cx}" cy="${cy}" r="${R}" fill="none" stroke="#e6eaee" stroke-width="22"/>`;
  let offset = 0;
  (segments || []).forEach((s, i) => {
    const len = (C * clamp(s.percent)) / 100;
    if (len <= 0) return;
    svg += `<circle cx="${cx}" cy="${cy}" r="${R}" fill="none" stroke="${statusColor(s.statusCode, i)}" stroke-width="22" stroke-dasharray="${len} ${C - len}" stroke-dashoffset="${-offset}" transform="rotate(-90 ${cx} ${cy})"/>`;
    offset += len;
  });
  svg += `</svg>`;
  return svg;
}

/** 计划 vs 实际 折线图 */
function lineChart(points) {
  const W = 620, H = 250, padL = 36, padR = 16, padT = 14, padB = 28;
  const iw = W - padL - padR, ih = H - padT - padB;
  const list = points || [];
  const X = (i) => padL + (list.length <= 1 ? iw / 2 : (i * iw) / (list.length - 1));
  const Y = (v) => padT + ih - (clamp(v) / 100) * ih;

  let svg = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="计划与实际进度趋势">`;
  svg += `<g stroke="#e6eaee" stroke-width="1">`;
  [0, 25, 50, 75, 100].forEach((v) => {
    svg += `<line x1="${padL}" y1="${Y(v)}" x2="${W - padR}" y2="${Y(v)}"/>`;
    svg += `<text x="${padL - 7}" y="${Y(v) + 3}" fill="#7b8794" font-family="Consolas,monospace" font-size="9.5" text-anchor="end">${v}</text>`;
  });
  svg += `</g>`;
  list.forEach((p, i) => {
    svg += `<text x="${X(i)}" y="${H - 9}" fill="#7b8794" font-family="Consolas,monospace" font-size="10" text-anchor="middle">${esc(p.label)}</text>`;
  });
  if (list.length) {
    const poly = (key, color) => {
      const pts = list.map((p, i) => `${X(i).toFixed(1)},${Y(p[key]).toFixed(1)}`).join(" ");
      svg += `<polyline points="${pts}" fill="none" stroke="${color}" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round"/>`;
      list.forEach((p, i) => {
        svg += `<circle cx="${X(i).toFixed(1)}" cy="${Y(p[key]).toFixed(1)}" r="3.2" fill="${color}"/>`;
      });
    };
    poly("plan", "#d98c0b");
    poly("actual", "#1d6fd1");
  }
  svg += `</svg>`;
  return svg;
}

/* ---------------- 数据 ---------------- */

const ongoingRate = computed(() => {
  const total = Number(hero.value?.totalProjects) || 0;
  return total ? num(((Number(hero.value?.ongoingProjects) || 0) * 100) / total) : "0.0";
});

/** 分项目进度对比只展示最近 10 个，避免大屏被撑爆 */
const topProjects = computed(() => (hero.value?.projectProgress || []).slice(0, 10));

const nodeOptions = computed(() =>
  (hero.value?.projectProgress || []).map((p) => ({ id: p.projectId, name: p.projectName }))
);

async function load() {
  loading.value = true;
  try {
    const [d, r, t] = await Promise.all([
      dashboardApi.overview(),
      dashboardApi.inspectionStats(),
      dashboardApi.progressTrend(gran.value),
    ]);
    hero.value = d;
    rates.value = r;
    trend.value = t;
    await loadNodes();
  } catch (e) {
    toast.error(e.message || "可视化数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadNodes() {
  nodeLoading.value = true;
  try {
    nodes.value = await dashboardApi.keyNodes(nodeFilter.value || undefined);
  } catch (e) {
    toast.error(e.message || "关键节点加载失败");
  } finally {
    nodeLoading.value = false;
  }
}

async function switchGran(g) {
  gran.value = g;
  try {
    trend.value = await dashboardApi.progressTrend(g);
  } catch (e) {
    toast.error(e.message || "趋势数据加载失败");
  }
}

watch(nodeFilter, () => {
  expanded.value = new Set(); // 换了项目，之前展开的节点 id 不再适用
  loadNodes();
});

onMounted(load);
</script>

<template>
  <div class="rise">
    <div class="page-head vz-head">
      <div>
        <div class="page-head__eyebrow">PIDMS · VISUAL CONSOLE</div>
        <h1 class="page-head__title">可视化管理</h1>
      </div>
      <div class="vz-head__right">
        <span class="vz-head__stamp">全局视角 · 实时聚合</span>
        <button class="btn btn--sm" :disabled="loading" @click="load">刷新</button>
      </div>
    </div>

    <div v-if="loading" class="loading"><span class="spinner"></span>加载全局数据…</div>

    <template v-else-if="hero">
      <!-- ============ 规模指标 ============ -->
      <section class="kpis">
        <div class="kpi">
          <div class="kpi__k">项目总数</div>
          <div class="kpi__v">{{ hero.totalProjects }}<small>个</small></div>
          <div class="kpi__f">在建占比 {{ ongoingRate }}%</div>
        </div>
        <div class="kpi is-blue">
          <div class="kpi__k">在建项目</div>
          <div class="kpi__v">{{ hero.ongoingProjects }}<small>个</small></div>
          <div class="kpi__f">已完工 {{ hero.completedProjects }} 个</div>
        </div>
        <div class="kpi" :class="{ 'is-alert': hero.delayedProjects > 0 }">
          <div class="kpi__k">延期项目</div>
          <div class="kpi__v">{{ hero.delayedProjects }}<small>个</small></div>
          <div class="kpi__f">存在逾期未完成节点</div>
        </div>
        <div class="kpi" :class="{ 'is-alert': hero.overdueNodeCount > 0 }">
          <div class="kpi__k">逾期节点</div>
          <div class="kpi__v">{{ hero.overdueNodeCount }}<small>个</small></div>
          <div class="kpi__f">共 {{ hero.nodeCount }} 个形象进度节点</div>
        </div>
        <div class="kpi">
          <div class="kpi__k">质量/安全合格率</div>
          <div class="kpi__v">{{ num(rates?.qualityRate) }}<small>%</small></div>
          <div class="kpi__f">安全 {{ num(rates?.safetyRate) }}% · 整改闭环 {{ num(rates?.qualityClosedRate) }}%</div>
        </div>
      </section>

      <!-- ============ 总体进度（结构柱进度仪） ============ -->
      <section class="panel hero mt20" aria-label="总体进度">
        <div class="hero__main">
          <div class="hero__eyebrow">OVERALL PROGRESS · 总体进度</div>
          <div class="hero__value">{{ num(hero.overallProgress) }}<small>%</small></div>
          <div class="hero__delta">
            进度偏差
            <b :class="deltaClass(hero.progressDelta)">{{ signed(hero.progressDelta) }}</b>
            <span class="hero__cmp">
              实际 {{ num(hero.overallProgress) }}% / 计划 {{ num(hero.planProgress) }}%<template
                v-if="hero.expectedProgress !== null && hero.expectedProgress !== undefined"
              > / 期望 {{ num(hero.expectedProgress) }}%</template>
            </span>
          </div>
          <div class="hero__meta">
            <div class="hero__meta-item"><div class="v">{{ hero.ongoingProjects }}</div><div class="k">在建项目</div></div>
            <div class="hero__meta-item"><div class="v">{{ hero.overdueNodeCount }}</div><div class="k">逾期节点</div></div>
            <div class="hero__meta-item"><div class="v">{{ rates?.qualityMonthCount ?? 0 }}</div><div class="k">本月质检</div></div>
            <div class="hero__meta-item"><div class="v">{{ rates?.safetyMonthCount ?? 0 }}</div><div class="k">本月安检</div></div>
          </div>
        </div>
        <div class="hero__gauge" v-html="columnGauge(hero.overallProgress, hero.planProgress)"></div>
      </section>

      <!-- ============ 趋势 + 状态分布 ============ -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">PLAN vs ACTUAL</div>
              <h2 class="panel__title">进度趋势</h2>
            </div>
            <div class="seg" role="group" aria-label="时间粒度">
              <button :class="{ 'is-active': gran === 'day' }" @click="switchGran('day')">自然日</button>
              <button :class="{ 'is-active': gran === 'week' }" @click="switchGran('week')">自然周</button>
              <button :class="{ 'is-active': gran === 'month' }" @click="switchGran('month')">自然月</button>
            </div>
          </div>
          <div class="panel__body">
            <div class="chart__legend">
              <span><span class="lg-dot" style="background: var(--amber)"></span>计划进度</span>
              <span><span class="lg-dot" style="background: var(--blue)"></span>实际进度</span>
            </div>
            <div class="chart" v-html="lineChart(trend?.points)"></div>
            <p class="chart__note">{{ trend?.description }}</p>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">PROJECT STATUS</div>
              <h2 class="panel__title">项目状态分布</h2>
            </div>
            <router-link to="/project" class="panel__link">项目列表 →</router-link>
          </div>
          <div class="panel__body donut-wrap">
            <div class="donut">
              <div v-html="donut(hero.statusDistribution)"></div>
              <div class="donut__center">
                <div class="t">{{ hero.totalProjects }}</div>
                <div class="k">个项目</div>
              </div>
            </div>
            <div class="legend">
              <div v-for="(s, i) in hero.statusDistribution" :key="s.statusCode" class="legend__row">
                <span class="legend__swatch" :style="{ background: statusColor(s.statusCode, i) }"></span>
                <span class="legend__name">{{ s.statusName }}</span>
                <span class="legend__count">{{ s.count }}</span>
                <span class="legend__pct">{{ num(s.percent) }}%</span>
              </div>
              <div v-if="!hero.statusDistribution.length" class="legend__empty">暂无项目数据</div>
            </div>
          </div>
        </section>
      </div>

      <!-- ============ 分项目进度对比 ============ -->
      <section class="panel mt20">
        <div class="panel__head">
          <div>
            <div class="panel__eyebrow">PROGRESS BY PROJECT</div>
            <h2 class="panel__title">分项目进度对比</h2>
          </div>
          <div class="legend-inline">
            <span><i class="lg-plan"></i>计划进度</span>
            <span><i class="lg-actual"></i>实际进度</span>
          </div>
        </div>
        <div class="panel__body">
          <div v-if="!topProjects.length" class="empty">暂无项目进度数据</div>
          <div v-else class="cmp">
            <div class="cmp__head">
              <span>项目</span><span>进度对比</span><span>完成率</span><span>节点</span>
            </div>
            <div v-for="p in topProjects" :key="p.projectId" class="cmp__row">
              <div class="cmp__name">
                <div class="cmp__title">{{ p.projectName }}</div>
                <div class="cmp__sub">{{ p.projectCode || "无编号" }} · {{ p.projectStatusName }} · 负责人 {{ p.projectLeader || "未指定" }}</div>
              </div>
              <div class="cmp__bars" :title="`实际 ${num(p.actualProgress)}% · 计划 ${num(p.planProgress)}%`">
                <div class="cmp__track">
                  <i class="cmp__plan" :style="{ width: pct(p.planProgress) }"></i>
                  <i class="cmp__actual" :class="deltaClass(p.progressDelta)" :style="{ width: pct(p.actualProgress) }"></i>
                </div>
              </div>
              <div class="cmp__val">
                <span class="cmp__num">{{ num(p.actualProgress) }}%</span>
                <span class="cmp__delta" :class="deltaClass(p.progressDelta)">{{ signed(p.progressDelta) }}</span>
              </div>
              <div class="cmp__node">
                <span v-if="p.overdueNodeCount" class="tag tag--red">逾期 {{ p.overdueNodeCount }}</span>
                <span v-else class="tag tag--gray">{{ p.completedNodeCount }}/{{ p.nodeCount }}</span>
              </div>
            </div>
            <p v-if="hero.projectProgress.length > topProjects.length" class="cmp__more">
              仅显示进度靠前的 {{ topProjects.length }} 个项目，共 {{ hero.projectProgress.length }} 个
            </p>
          </div>
        </div>
      </section>

      <!-- ============ 合格率与闭环率 + 关键节点 ============ -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">INSPECTION & RECTIFICATION</div>
              <h2 class="panel__title">检查合格率与整改闭环率</h2>
            </div>
          </div>
          <div class="panel__body rate-list">
            <div class="rate-item">
              <div class="rate-item__head">
                <span class="rate-item__name">质量检查合格率</span>
                <span class="rate-item__val">{{ num(rates?.qualityRate) }}%</span>
              </div>
              <div class="rate-track">
                <div class="rate-track__fill" :style="{ width: pct(rates?.qualityRate), background: 'var(--blue)' }"></div>
                <div class="rate-track__ticks"></div>
              </div>
              <div class="rate-item__foot">
                <span>本月 <b>{{ rates?.qualityMonthCount ?? 0 }}</b> 次</span>
                <span>累计 <b>{{ rates?.qualityInspectionCount ?? 0 }}</b> 次</span>
                <span>不合格 <b>{{ rates?.qualityUnqualifiedCount ?? 0 }}</b> 次</span>
              </div>
            </div>

            <div class="rate-item">
              <div class="rate-item__head">
                <span class="rate-item__name">质量整改闭环率</span>
                <span class="rate-item__val">{{ num(rates?.qualityClosedRate) }}%</span>
              </div>
              <div class="rate-track">
                <div class="rate-track__fill" :style="{ width: pct(rates?.qualityClosedRate), background: 'var(--green)' }"></div>
                <div class="rate-track__ticks"></div>
              </div>
              <div class="rate-item__foot">
                <span>已整改 <b>{{ rates?.qualityRectificationDone ?? 0 }}</b> / {{ rates?.qualityRectificationTotal ?? 0 }} 条</span>
              </div>
            </div>

            <div class="rate-item">
              <div class="rate-item__head">
                <span class="rate-item__name">安全检查合格率</span>
                <span class="rate-item__val">{{ num(rates?.safetyRate) }}%</span>
              </div>
              <div class="rate-track">
                <div class="rate-track__fill" :style="{ width: pct(rates?.safetyRate), background: 'var(--blue)' }"></div>
                <div class="rate-track__ticks"></div>
              </div>
              <div class="rate-item__foot">
                <span>本月 <b>{{ rates?.safetyMonthCount ?? 0 }}</b> 次</span>
                <span>累计 <b>{{ rates?.safetyInspectionCount ?? 0 }}</b> 次</span>
                <span>不合格 <b>{{ rates?.safetyUnqualifiedCount ?? 0 }}</b> 次</span>
              </div>
            </div>

            <div class="rate-item">
              <div class="rate-item__head">
                <span class="rate-item__name">安全整改闭环率</span>
                <span class="rate-item__val">{{ num(rates?.safetyClosedRate) }}%</span>
              </div>
              <div class="rate-track">
                <div class="rate-track__fill" :style="{ width: pct(rates?.safetyClosedRate), background: 'var(--green)' }"></div>
                <div class="rate-track__ticks"></div>
              </div>
              <div class="rate-item__foot">
                <span>已整改 <b>{{ rates?.safetyRectificationDone ?? 0 }}</b> / {{ rates?.safetyRectificationTotal ?? 0 }} 条</span>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">KEY MILESTONES</div>
              <h2 class="panel__title">关键节点时间线</h2>
            </div>
            <select v-model="nodeFilter" class="vz-select" aria-label="按项目筛选关键节点">
              <option value="">全部项目</option>
              <option v-for="p in nodeOptions" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </div>
          <div v-if="hasTree" class="tl-tools">
            <button class="btn btn--sm" @click="expandAll">展开全部</button>
            <button class="btn btn--sm" @click="collapseAll">收起全部</button>
          </div>
          <div class="panel__body">
            <div v-if="nodeLoading" class="loading"><span class="spinner"></span>加载节点…</div>
            <div v-else-if="!visibleNodes.length" class="empty">该项目下暂无形象进度节点</div>
            <div v-else class="timeline">
              <div
                v-for="n in visibleNodes"
                :key="n.id"
                class="tl"
                :class="[`tl--${n.nodeStatus}`, { 'tl--child': n.__depth > 0 }]"
                :style="{ marginLeft: n.__depth * 18 + 'px' }"
              >
                <div class="tl__head">
                  <button
                    v-if="n.__hasChildren"
                    class="tree-toggle"
                    :aria-expanded="n.__expanded ? 'true' : 'false'"
                    :title="n.__expanded ? '收起子节点' : '展开子节点'"
                    @click="toggleExpand(n)"
                  >{{ n.__expanded ? "▾" : "▸" }}</button>
                  <span class="tl__name">{{ n.nodeName }}</span>
                  <span class="tag" :class="`tag--${(NODE_TAG[n.nodeStatus] || ['gray', '—'])[0]}`">{{ (NODE_TAG[n.nodeStatus] || ["gray", "—"])[1] }}</span>
                  <span v-if="n.overdue" class="tag tag--red">逾期 {{ n.overdueDays }} 天</span>
                  <span v-if="n.weightAuto" class="tag tag--auto" title="由子节点汇总 / 按剩余权重平分">自动</span>
                </div>
                <div class="tl__sub">
                  {{ n.projectName }}<template v-if="n.responsiblePerson"> · 负责人 {{ n.responsiblePerson }}</template>
                  <template v-if="n.childCount"> · 含 {{ n.childCount }} 个子节点</template>
                </div>
                <div class="tl__info">
                  <span class="tl__dates">
                    计划 {{ fmtDate(n.planStartDate) }} ~ {{ fmtDate(n.planEndDate) }}
                    <template v-if="n.actualEndDate"> · 实际完成 {{ fmtDate(n.actualEndDate) }}</template>
                  </span>
                  <span class="tl__pct">{{ num(n.completionPercent) }}%</span>
                </div>
                <div class="tl__bar"><i :style="{ width: pct(n.completionPercent) }" :class="`is-${n.nodeStatus}`"></i></div>
                <div class="tl__meta">
                  <span v-if="n.effectiveWeight !== null && n.effectiveWeight !== undefined">
                    占总进度 <b>{{ num(n.effectiveWeight) }}%</b>
                  </span>
                  <span v-if="n.expectedPercent !== null && n.expectedPercent !== undefined">
                    期望值 <b>{{ num(n.expectedPercent) }}%</b>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <p class="vz-note">{{ hero.description }}</p>
    </template>

    <div v-else class="empty">可视化数据加载失败，请稍后重试</div>
  </div>
</template>

<style scoped>
.vz-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.vz-head__right { display: flex; align-items: center; gap: 14px; }
.vz-head__stamp { font-family: var(--mono); font-size: 11px; letter-spacing: 1.5px; color: var(--muted); text-transform: uppercase; }
.mt20 { margin-top: 20px; }

/* 指标卡 */
.kpis { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.kpi {
  background: var(--card); border: 1px solid var(--line); border-radius: var(--radius);
  padding: 15px 17px 13px; box-shadow: var(--shadow-sm); position: relative; overflow: hidden;
}
.kpi::before {
  content: ""; position: absolute; left: 0; top: 14px; bottom: 14px; width: 3px;
  background: var(--line-strong); border-radius: 0 3px 3px 0;
}
.kpi.is-blue::before { background: var(--blue); }
.kpi.is-alert::before { background: var(--amber); }
.kpi__k { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.kpi__v { font-family: var(--num); font-size: 28px; font-weight: 600; color: var(--ink); line-height: 1.1; }
.kpi__v small { font-size: 12.5px; color: var(--muted); font-weight: 500; margin-left: 3px; }
.kpi__f { font-size: 11.5px; color: var(--muted); margin-top: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 英雄区 */
.hero {
  background:
    linear-gradient(rgba(29, 111, 209, .07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(29, 111, 209, .07) 1px, transparent 1px),
    #fbfcfe;
  background-size: 22px 22px;
  color: var(--ink);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  display: grid; grid-template-columns: 1.15fr 1fr; gap: 26px;
  position: relative; overflow: hidden; box-shadow: var(--shadow);
}
.hero::after {
  content: ""; position: absolute; top: 14px; right: 14px; width: 22px; height: 22px;
  background:
    linear-gradient(var(--blue), var(--blue)) center/1px 100% no-repeat,
    linear-gradient(var(--blue), var(--blue)) center/100% 1px no-repeat,
    radial-gradient(circle at center, transparent 3.5px, var(--blue) 3.5px 4.5px, transparent 4.5px);
  opacity: .4; pointer-events: none;
}
.hero__main { padding: 26px 30px 28px; display: flex; flex-direction: column; justify-content: center; }
.hero__eyebrow { font-family: var(--mono); font-size: 11px; letter-spacing: 3px; color: var(--blue); text-transform: uppercase; margin-bottom: 10px; }
.hero__value { font-family: var(--num); font-weight: 600; font-size: 58px; line-height: 1; color: var(--ink); }
.hero__value small { font-size: 23px; color: var(--muted); font-weight: 500; }
.hero__delta { margin-top: 12px; font-size: 13px; color: var(--muted); }
.hero__delta b { font-family: var(--mono); font-size: 15px; margin-left: 2px; }
.hero__delta b.is-ok { color: var(--green); }
.hero__delta b.is-warn { color: var(--amber); }
.hero__delta b.is-bad { color: var(--red); }
.hero__cmp { margin-left: 12px; font-family: var(--mono); font-size: 12px; color: var(--muted); }
.hero__meta { display: flex; gap: 32px; margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--line); }
.hero__meta-item .v { font-family: var(--num); font-size: 22px; font-weight: 600; color: var(--ink); }
.hero__meta-item .k { font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.hero__gauge { padding: 18px 22px 20px; display: flex; align-items: center; justify-content: center; border-left: 1px solid var(--line); }
.hero__gauge :deep(svg) { width: 100%; max-width: 300px; height: auto; }

/* 环形图 */
.donut-wrap { display: flex; align-items: center; gap: 22px; flex-wrap: wrap; }
.donut { position: relative; width: 168px; height: 168px; flex-shrink: 0; }
.donut :deep(svg) { width: 100%; height: 100%; }
.donut__center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; pointer-events: none; }
.donut__center .t { font-family: var(--num); font-size: 26px; font-weight: 600; color: var(--ink); }
.donut__center .k { font-size: 11px; color: var(--muted); margin-top: 2px; }
.legend { display: flex; flex-direction: column; gap: 9px; flex: 1; min-width: 170px; }
.legend__row { display: flex; align-items: center; gap: 9px; font-size: 13px; }
.legend__swatch { width: 9px; height: 9px; border-radius: 2px; flex-shrink: 0; }
.legend__name { color: var(--slate); flex: 1; }
.legend__count { font-family: var(--mono); font-size: 12px; color: var(--muted); }
.legend__pct { font-family: var(--mono); color: var(--ink); font-weight: 600; min-width: 52px; text-align: right; }
.legend__empty { font-size: 12.5px; color: var(--muted); }

/* 趋势图 */
.seg { display: inline-flex; background: var(--paper); border: 1px solid var(--line); border-radius: 7px; padding: 2px; }
.seg button { border: none; background: transparent; padding: 4px 12px; border-radius: 5px; font-size: 12.5px; color: var(--muted); cursor: pointer; transition: all .15s; }
.seg button.is-active { background: var(--ink); color: #fff; }
.chart__legend { display: flex; gap: 16px; margin-bottom: 6px; font-size: 12.5px; color: var(--slate); }
.chart__legend .lg-dot { width: 8px; height: 8px; border-radius: 2px; display: inline-block; margin-right: 6px; }
.chart :deep(svg) { width: 100%; height: auto; display: block; }
.chart__note { margin: 8px 0 0; font-size: 11.5px; color: var(--muted); line-height: 1.7; }

/* 分项目对比 */
.legend-inline { display: flex; gap: 16px; font-size: 12px; color: var(--muted); }
.legend-inline i { display: inline-block; vertical-align: middle; margin-right: 6px; }
.lg-plan { width: 14px; height: 8px; border-radius: 2px; background: #d7dfe9; }
.lg-actual { width: 14px; height: 8px; border-radius: 2px; background: var(--blue); }
.cmp__head, .cmp__row {
  display: grid; grid-template-columns: minmax(220px, 1.5fr) minmax(180px, 2.2fr) 104px 92px;
  align-items: center; gap: 16px;
}
.cmp__head {
  font-size: 11px; color: var(--muted); font-family: var(--mono); letter-spacing: 1px;
  padding-bottom: 8px; border-bottom: 1px solid var(--line); text-transform: uppercase;
}
.cmp__row { padding: 11px 0; border-bottom: 1px solid var(--line); }
.cmp__row:last-of-type { border-bottom: none; }
.cmp__title { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.cmp__sub { font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.cmp__track { position: relative; height: 14px; border-radius: 4px; background: var(--grid); overflow: hidden; }
.cmp__plan, .cmp__actual { position: absolute; left: 0; border-radius: 4px; transition: width .6s cubic-bezier(.2,.8,.2,1); }
.cmp__plan { top: 0; height: 100%; background: #d7dfe9; }
.cmp__actual { bottom: 0; height: 6px; }
.cmp__actual.is-ok { background: var(--green); }
.cmp__actual.is-warn { background: var(--amber); }
.cmp__actual.is-bad { background: var(--red); }
.cmp__val { display: flex; align-items: baseline; gap: 8px; justify-content: flex-end; }
.cmp__num { font-family: var(--num); font-size: 16px; font-weight: 600; color: var(--ink); }
.cmp__delta { font-family: var(--mono); font-size: 11.5px; }
.cmp__delta.is-ok { color: var(--green); }
.cmp__delta.is-warn { color: var(--amber); }
.cmp__delta.is-bad { color: var(--red); }
.cmp__node { text-align: right; }
.cmp__more { margin: 10px 0 0; font-size: 11.5px; color: var(--muted); }

/* 合格率 */
.rate-list { display: flex; flex-direction: column; gap: 20px; }
.rate-item__head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }
.rate-item__name { font-size: 13px; color: var(--slate); }
.rate-item__val { font-family: var(--num); font-size: 20px; font-weight: 600; color: var(--ink); }
.rate-track { position: relative; height: 12px; border-radius: 6px; background: var(--grid); overflow: hidden; }
.rate-track__fill { height: 100%; border-radius: 6px; transition: width .6s cubic-bezier(.2,.8,.2,1); }
.rate-track__ticks { position: absolute; inset: 0; background: repeating-linear-gradient(90deg, transparent 0 19%, rgba(255,255,255,.55) 19% 20%); }
.rate-item__foot { display: flex; gap: 18px; margin-top: 8px; font-size: 12px; color: var(--muted); }
.rate-item__foot b { font-family: var(--num); color: var(--ink); }

/* 关键节点 */
.vz-select {
  border: 1px solid var(--line); border-radius: 7px; background: var(--card); color: var(--ink);
  font-size: 12.5px; padding: 5px 8px; max-width: 200px;
}
.timeline { position: relative; padding-left: 24px; max-height: 470px; overflow-y: auto; }
.timeline::before { content: ""; position: absolute; left: 6px; top: 8px; bottom: 8px; width: 2px; background: var(--grid); }
.tl { position: relative; padding-bottom: 16px; }
.tl:last-child { padding-bottom: 0; }
.tl::before { content: ""; position: absolute; left: -24px; top: 4px; width: 12px; height: 12px; border-radius: 50%; background: var(--card); border: 3px solid var(--line); }
.tl--completed::before { border-color: var(--green); background: var(--green); }
.tl--current::before { border-color: var(--blue); background: var(--blue); box-shadow: 0 0 0 4px rgba(29,111,209,.16); }
.tl--pending::before { border-color: var(--amber); }
/* 子节点：圆点变小一圈，名称降一级，视觉上收在总节点之下 */
.tl--child::before { width: 8px; height: 8px; border-width: 2px; left: -22px; top: 6px; }
.tl--child .tl__name { font-size: 12.5px; font-weight: 500; color: var(--slate); }
.tl__head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tl__name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.tl__sub { font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.tl__info { display: flex; justify-content: space-between; gap: 12px; margin-top: 6px; font-size: 11.5px; color: var(--muted); font-family: var(--mono); }
.tl__pct { color: var(--ink); font-weight: 600; }
.tl__bar { margin-top: 6px; height: 5px; border-radius: 3px; background: var(--grid); overflow: hidden; }
.tl__bar i { display: block; height: 100%; border-radius: 3px; background: var(--blue); }
.tl__bar i.is-completed { background: var(--green); }
.tl__bar i.is-pending { background: var(--amber); }
.tl__meta { display: flex; gap: 16px; margin-top: 6px; font-size: 11.5px; color: var(--muted); font-family: var(--mono); }
.tl__meta b { color: var(--ink); font-weight: 600; }
/* 展开/折叠控制条 */
.tl-tools { display: flex; justify-content: flex-end; gap: 8px; padding: 10px 22px 0; }
.tree-toggle {
  width: 16px; height: 16px; padding: 0; flex-shrink: 0;
  border: 1px solid var(--line); border-radius: 4px;
  background: var(--card); color: var(--steel);
  font-size: 10px; line-height: 1; cursor: pointer;
}
.tree-toggle:hover { border-color: var(--blue); color: var(--blue); background: var(--blue-soft); }
.tag--auto { margin-left: 0; padding: 1px 5px; font-size: 10.5px; background: rgba(138, 153, 171, .14); color: var(--muted); }
.tag--auto::before { display: none; }

.panel__link { font-size: 12.5px; color: var(--blue); }
.vz-note { margin: 18px 2px 0; font-size: 11.5px; color: var(--muted); line-height: 1.7; font-family: var(--mono); }

@media (max-width: 1360px) {
  .kpis { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1080px) {
  .grid-2 { grid-template-columns: 1fr; }
  .hero { grid-template-columns: 1fr; }
  .hero__gauge { border-left: none; border-top: 1px solid var(--line); }
  .cmp__head { display: none; }
  .cmp__row { grid-template-columns: 1fr; gap: 8px; }
  .cmp__val, .cmp__node { justify-content: flex-start; text-align: left; }
}
</style>
