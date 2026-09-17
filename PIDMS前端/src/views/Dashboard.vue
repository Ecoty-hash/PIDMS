<script setup>
import { ref, onMounted } from "vue";
import { dashboardApi } from "@/api/request.js";
import { useToast } from "@/composables/useToast.js";
import StatusBadge from "@/components/StatusBadge.vue";

const toast = useToast();

const hero = ref(null);
const rates = ref(null);
const nodes = ref(null);
const projects = ref([]);
const warnings = ref([]);
const trend = ref({ series: [] });
const gran = ref("week");
const loading = ref(true);

/* ---------- SVG 生成 ---------- */
function esc(s) {
  return String(s == null ? "" : s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function columnGauge(percent) {
  const H = 170, W = 210, top = 8, bottom = H - 8, colX = 82, colW = 44;
  const fill = Math.max(0, Math.min(100, percent));
  const fillH = ((bottom - top) * fill) / 100;
  const fillY = bottom - fillH;
  const ticks = [0, 25, 50, 75, 100];
  let svg = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="总体进度 ${fill}%">`;
  svg += `<g stroke="rgba(29,111,209,.09)" stroke-width="1">`;
  for (let y = top; y <= bottom; y += (bottom - top) / 4) svg += `<line x1="6" y1="${y}" x2="${W - 6}" y2="${y}"/>`;
  svg += `</g>`;
  svg += `<g font-family="Consolas, monospace" font-size="9" fill="#8a99ab" text-anchor="end">`;
  ticks.forEach((t) => {
    const y = bottom - ((bottom - top) * t) / 100;
    svg += `<line x1="${colX - 12}" y1="${y}" x2="${colX}" y2="${y}" stroke="#8a99ab" stroke-width="1"/>`;
    svg += `<text x="${colX - 16}" y="${y + 3}">${t}</text>`;
  });
  svg += `</g>`;
  svg += `<rect x="${colX}" y="${top}" width="${colW}" height="${bottom - top}" fill="none" stroke="#1d6fd1" stroke-opacity=".55" stroke-width="1.2"/>`;
  if (fillH > 0) {
    svg += `<defs><linearGradient id="colFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#de8a0b"/><stop offset="1" stop-color="#b8740a"/></linearGradient></defs>`;
    svg += `<rect x="${colX + 1.5}" y="${fillY}" width="${colW - 3}" height="${fillH}" fill="url(#colFill)" opacity=".85"/>`;
  }
  svg += `<line x1="${colX - 4}" y1="${fillY}" x2="${colX + colW + 4}" y2="${fillY}" stroke="#de8a0b" stroke-width="1.6"/>`;
  const dimX = colX + colW + 26;
  svg += `<line x1="${dimX}" y1="${top}" x2="${dimX}" y2="${bottom}" stroke="#8a99ab" stroke-width="1" stroke-dasharray="3 3"/>`;
  svg += `<path d="M ${dimX - 4} ${top + 6} l 4 -6 l 4 6 M ${dimX - 4} ${bottom - 6} l 4 6 l 4 -6" fill="none" stroke="#8a99ab" stroke-width="1"/>`;
  svg += `<text x="${dimX + 8}" y="${fillY + 3}" fill="#16324f" font-family="Bahnschrift, Segoe UI, sans-serif" font-size="16" font-weight="600">${fill.toFixed(1)}%</text>`;
  svg += `<g stroke="#1d6fd1" stroke-opacity=".55" stroke-width="1"><circle cx="${colX + colW / 2}" cy="${top - 6}" r="5" fill="none"/><path d="M ${colX + colW / 2} ${top - 13} v 14 M ${colX + colW / 2 - 7} ${top - 6} h 14"/></g>`;
  svg += `</svg>`;
  return svg;
}

function donut(segments) {
  const R = 62, C = 2 * Math.PI * R, cx = 84, cy = 84;
  const colors = { 在建: "#1d6fd1", 已完工: "#1e8e5e", 规划中: "#d98c0b", 延期: "#c2402e" };
  let svg = `<svg viewBox="0 0 168 168">`;
  svg += `<circle cx="${cx}" cy="${cy}" r="${R}" fill="none" stroke="#e6eaee" stroke-width="22"/>`;
  let offset = 0;
  segments.forEach((s) => {
    const len = (C * (s.percent || 0)) / 100;
    svg += `<circle cx="${cx}" cy="${cy}" r="${R}" fill="none" stroke="${colors[s.status] || "#7b8794"}" stroke-width="22" stroke-dasharray="${len} ${C - len}" stroke-dashoffset="${-offset}" transform="rotate(-90 ${cx} ${cy})"/>`;
    offset += len;
  });
  svg += `</svg>`;
  return svg;
}

function lineChart(series) {
  const W = 600, H = 240, padL = 34, padR = 14, padT = 12, padB = 26;
  const iw = W - padL - padR, ih = H - padT - padB;
  const X = (i) => padL + (series.length === 1 ? iw / 2 : (i * iw) / (series.length - 1));
  const Y = (v) => padT + ih - (v / 100) * ih;
  let svg = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="进度趋势折线图">`;
  svg += `<g stroke="#e6eaee" stroke-width="1">`;
  [0, 25, 50, 75, 100].forEach((v) => {
    svg += `<line x1="${padL}" y1="${Y(v)}" x2="${W - padR}" y2="${Y(v)}"/>`;
    svg += `<text x="${padL - 7}" y="${Y(v) + 3}" fill="#7b8794" font-family="Consolas,monospace" font-size="9.5" text-anchor="end">${v}</text>`;
  });
  svg += `</g>`;
  series.forEach((s, i) => {
    svg += `<text x="${X(i)}" y="${H - 8}" fill="#7b8794" font-family="Consolas,monospace" font-size="10" text-anchor="middle">${esc(s.label)}</text>`;
  });
  const poly = (key, color) => {
    const pts = series.map((s, i) => `${X(i).toFixed(1)},${Y(s[key]).toFixed(1)}`).join(" ");
    svg += `<polyline points="${pts}" fill="none" stroke="${color}" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round"/>`;
    series.forEach((s, i) => svg += `<circle cx="${X(i).toFixed(1)}" cy="${Y(s[key]).toFixed(1)}" r="3.2" fill="${color}"/>`);
  };
  poly("plan", "#1d6fd1");
  poly("actual", "#1e8e5e");
  svg += `</svg>`;
  return svg;
}

/* ---------- 数据加载 ---------- */
async function load() {
  loading.value = true;
  try {
    const [d, r, n, t] = await Promise.all([
      dashboardApi.overview(),
      dashboardApi.inspectionStats(),
      dashboardApi.keyNodes(),
      dashboardApi.progressTrend(gran.value),
    ]);
    hero.value = d;
    rates.value = r;
    nodes.value = n;
    trend.value = t;
    const [p, w] = await Promise.all([
      dashboardApi.activeProjects(""),
      dashboardApi.warningRules(),
    ]);
    projects.value = p.list || p;
    warnings.value = w.list || w;
  } catch (e) {
    toast.error(e.message || "看板数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function switchGran(g) {
  gran.value = g;
  trend.value = await dashboardApi.progressTrend(g);
}

async function searchProjects(kw) {
  const p = await dashboardApi.activeProjects(kw);
  projects.value = p.list || p;
}

const nodeTag = { completed: ["done", "已完成"], current: ["now", "进行中"], pending: ["todo", "未开始"] };

onMounted(load);
</script>

<template>
  <div class="rise">
    <div class="page-head">
      <div class="page-head__eyebrow">PIDMS · CONSOLE</div>
      <h1 class="page-head__title">工作台</h1>
    </div>

    <div v-if="loading" class="loading" style="padding: 60px 0;"><span class="spinner"></span>加载看板数据…</div>

    <template v-else>
      <!-- 英雄区：结构柱进度仪 -->
      <section class="panel hero" aria-label="项目总体进度">
        <div class="hero__main">
          <div class="hero__eyebrow">OVERALL PROGRESS · 项目总进度</div>
          <div class="hero__value">{{ hero?.overallProgress?.toFixed(2) }}<small>%</small></div>
          <div class="hero__delta"><span class="up">▲ {{ hero?.progressDelta }}</span></div>
          <div class="hero__meta">
            <div class="hero__meta-item"><div class="v">{{ hero?.ongoingProjects }}</div><div class="k">进行中项目</div></div>
            <div class="hero__meta-item"><div class="v">{{ hero?.qualityCount }}</div><div class="k">本月质检次数</div></div>
            <div class="hero__meta-item"><div class="v">{{ hero?.safetyCount }}</div><div class="k">本月安检次数</div></div>
          </div>
        </div>
        <div class="hero__gauge" v-html="columnGauge(hero?.overallProgress || 0)"></div>
      </section>

      <!-- 状态分布 + 合格率 -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">PROJECT STATUS</div>
              <h2 class="panel__title">项目状态分布</h2>
            </div>
          </div>
          <div class="panel__body donut-wrap">
            <div class="donut">
              <div v-html="donut(hero?.projectStatusDistribution || [])"></div>
              <div class="donut__center">
                <div class="t">{{ (hero?.projectStatusDistribution || []).reduce((a, s) => a + s.count, 0) }}</div>
                <div class="k">总项目数</div>
              </div>
            </div>
            <div class="legend">
              <div v-for="s in hero?.projectStatusDistribution || []" :key="s.status" class="legend__row">
                <span class="legend__swatch" :style="{ background: ({ 在建: '#1d6fd1', 已完工: '#1e8e5e', 规划中: '#d98c0b', 延期: '#c2402e' })[s.status] || '#8a99ab' }"></span>
                <span class="legend__name">{{ s.status }}</span>
                <span class="legend__pct">{{ s.percent }}%</span>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">INSPECTION RATE</div>
              <h2 class="panel__title">检查合格率</h2>
            </div>
          </div>
          <div class="panel__body rate-list">
            <div class="rate-item">
              <div class="rate-item__head">
                <span class="rate-item__name">质量检查合格率</span>
                <span class="rate-item__val">{{ rates?.qualityRate }}%</span>
              </div>
              <div class="rate-track">
                <div class="rate-track__fill" :style="{ width: (rates?.qualityRate || 0) + '%', background: 'var(--blue)' }"></div>
                <div class="rate-track__ticks"></div>
              </div>
              <div class="rate-item__foot"><span>本月质检 <b>{{ rates?.qualityCount }}</b> 次</span></div>
            </div>
            <div class="rate-item">
              <div class="rate-item__head">
                <span class="rate-item__name">安全检查合格率</span>
                <span class="rate-item__val">{{ rates?.safetyRate }}%</span>
              </div>
              <div class="rate-track">
                <div class="rate-track__fill" :style="{ width: (rates?.safetyRate || 0) + '%', background: 'var(--green)' }"></div>
                <div class="rate-track__ticks"></div>
              </div>
              <div class="rate-item__foot"><span>本月安检 <b>{{ rates?.safetyCount }}</b> 次</span></div>
            </div>
          </div>
        </section>
      </div>

      <!-- 趋势 + 关键节点 -->
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
              <span><span class="lg-dot" style="background: var(--blue)"></span>计划进度</span>
              <span><span class="lg-dot" style="background: var(--green)"></span>实际进度</span>
            </div>
            <div class="chart" v-html="lineChart(trend?.series || [])"></div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">KEY MILESTONES</div>
              <h2 class="panel__title">项目关键节点</h2>
            </div>
          </div>
          <div class="panel__body timeline">
            <div v-for="n in nodes" :key="n.name" class="tl-item" :class="`is-${nodeTag[n.status]?.[0] || 'todo'}`">
              <div class="tl-item__head">
                <span class="tl-item__name">{{ n.name }}</span>
                <span class="tl-tag" :class="nodeTag[n.status]?.[0] || 'todo'">{{ nodeTag[n.status]?.[1] || '未开始' }}</span>
              </div>
              <div class="tl-item__info">
                <span>计划：<b>{{ n.planStart }}</b> ~ <b>{{ n.planEnd }}</b></span>
                <span v-if="n.owner">负责人：<b>{{ n.owner }}</b></span>
                <span>进度：<b>{{ n.progress }}%</b></span>
              </div>
              <div class="tl-bar"><i :style="{ width: Math.max(2, n.progress) + '%' }"></i></div>
            </div>
          </div>
        </section>
      </div>

      <!-- 进行中项目 + 预警 -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">ACTIVE PROJECTS</div>
              <h2 class="panel__title">进行中项目</h2>
            </div>
            <router-link to="/project" style="font-size: 12.5px; color: var(--blue)">查看全部 →</router-link>
          </div>
          <div class="panel__body">
            <div class="search" style="margin-bottom: 10px; position: relative;">
              <span class="search__icon">⌕</span>
              <input
                type="search"
                placeholder="搜索进行中项目…"
                aria-label="搜索项目"
                @input="searchProjects($event.target.value)"
              />
            </div>
            <div v-if="!projects.length" class="empty">未找到进行中的项目</div>
            <div v-else class="list">
              <div v-for="p in projects" :key="p.id" class="list-item">
                <div class="list-item__main">
                  <div class="list-item__name">{{ p.projectName }}</div>
                  <div class="list-item__sub">{{ p.projectCode }} · 开工 {{ p.startDate }}</div>
                  <div class="mini-bar"><i :style="{ width: (p.progress || 0) + '%' }"></i></div>
                </div>
                <StatusBadge :value="p.projectStatus" />
                <span class="badge gray" style="font-family: var(--mono)">{{ p.progress || 0 }}%</span>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">WARNING RULES</div>
              <h2 class="panel__title">预警规则</h2>
            </div>
            <router-link to="/warning-rule" style="font-size: 12.5px; color: var(--blue)">管理 →</router-link>
          </div>
          <div class="panel__body list">
            <div v-for="w in warnings" :key="w.id" class="warn">
              <span class="warn__flag" :class="w.level === 'red' ? 'red' : 'yellow'"></span>
              <div class="warn__main">
                <div class="warn__name">{{ w.ruleName }}</div>
                <div class="warn__sub">{{ w.warningType }} · {{ w.projectName }}</div>
              </div>
              <StatusBadge :value="w.status" />
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 英雄区（工作台专属，浅色蓝图纸） */
.hero {
  background:
    linear-gradient(rgba(29, 111, 209, .07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(29, 111, 209, .07) 1px, transparent 1px),
    #fbfcfe;
  background-size: 22px 22px;
  color: var(--ink);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  display: grid; grid-template-columns: 1.05fr 1fr; gap: 26px;
  position: relative; overflow: hidden;
  box-shadow: var(--shadow);
}
.hero::after {
  content: ""; position: absolute; top: 14px; right: 14px; width: 22px; height: 22px;
  background:
    linear-gradient(var(--blue), var(--blue)) center/1px 100% no-repeat,
    linear-gradient(var(--blue), var(--blue)) center/100% 1px no-repeat,
    radial-gradient(circle at center, transparent 3.5px, var(--blue) 3.5px 4.5px, transparent 4.5px);
  opacity: .4; pointer-events: none;
}
.hero__main { padding: 28px 30px 30px; display: flex; flex-direction: column; justify-content: center; }
.hero__eyebrow { font-family: var(--mono); font-size: 11px; letter-spacing: 3px; color: var(--blue); text-transform: uppercase; margin-bottom: 10px; }
.hero__value { font-family: var(--num); font-weight: 600; font-size: 60px; line-height: 1; color: var(--ink); }
.hero__value small { font-size: 24px; color: var(--muted); font-weight: 500; }
.hero__delta { margin-top: 12px; font-size: 13px; color: var(--muted); }
.hero__delta .up { color: var(--green); font-weight: 600; }
.hero__meta { display: flex; gap: 28px; margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--line); }
.hero__meta-item .v { font-family: var(--num); font-size: 22px; font-weight: 600; color: var(--ink); }
.hero__meta-item .k { font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.hero__gauge { padding: 20px 24px 22px; display: flex; align-items: center; justify-content: center; border-left: 1px solid var(--line); }

.donut-wrap { display: flex; align-items: center; gap: 22px; flex-wrap: wrap; }
.donut { position: relative; width: 168px; height: 168px; flex-shrink: 0; }
.donut svg { width: 100%; height: 100%; }
.donut__center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; pointer-events: none; }
.donut__center .t { font-family: var(--num); font-size: 26px; font-weight: 600; color: var(--ink); }
.donut__center .k { font-size: 11px; color: var(--muted); margin-top: 2px; }
.legend { display: flex; flex-direction: column; gap: 9px; flex: 1; min-width: 150px; }
.legend__row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.legend__swatch { width: 9px; height: 9px; border-radius: 2px; flex-shrink: 0; }
.legend__name { color: var(--slate); flex: 1; }
.legend__pct { font-family: var(--mono); color: var(--ink); font-weight: 600; }

.rate-list { display: flex; flex-direction: column; gap: 22px; }
.rate-item__head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }
.rate-item__name { font-size: 13px; color: var(--slate); }
.rate-item__val { font-family: var(--num); font-size: 20px; font-weight: 600; color: var(--ink); }
.rate-track { position: relative; height: 12px; border-radius: 6px; background: var(--grid); overflow: hidden; }
.rate-track__fill { height: 100%; border-radius: 6px; transition: width .6s cubic-bezier(.2,.8,.2,1); }
.rate-track__ticks { position: absolute; inset: 0; background: repeating-linear-gradient(90deg, transparent 0 19%, rgba(255,255,255,.55) 19% 20%); }
.rate-item__foot { display: flex; gap: 18px; margin-top: 8px; font-size: 12px; color: var(--muted); }
.rate-item__foot b { font-family: var(--num); color: var(--ink); }

.seg { display: inline-flex; background: var(--paper); border: 1px solid var(--line); border-radius: 7px; padding: 2px; }
.seg button { border: none; background: transparent; padding: 4px 12px; border-radius: 5px; font-size: 12.5px; color: var(--muted); cursor: pointer; transition: all .15s; }
.seg button.is-active { background: var(--ink); color: #fff; }
.chart__legend { display: flex; gap: 16px; margin-bottom: 8px; font-size: 12.5px; color: var(--slate); }
.chart__legend .lg-dot { width: 8px; height: 8px; border-radius: 2px; display: inline-block; margin-right: 6px; }

.timeline { position: relative; padding-left: 26px; }
.timeline::before { content: ""; position: absolute; left: 6px; top: 6px; bottom: 6px; width: 2px; background: var(--grid); }
.tl-item { position: relative; padding-bottom: 18px; }
.tl-item::before { content: ""; position: absolute; left: -26px; top: 3px; width: 12px; height: 12px; border-radius: 50%; background: var(--card); border: 3px solid var(--line); }
.tl-item.is-done::before { border-color: var(--green); background: var(--green); }
.tl-item.is-now::before { border-color: var(--blue); background: var(--blue); box-shadow: 0 0 0 4px rgba(29,111,184,.16); }
.tl-item.is-todo::before { border-color: var(--amber); }
.tl-item__head { display: flex; align-items: center; gap: 8px; }
.tl-item__name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.tl-tag { font-size: 11px; padding: 1px 8px; border-radius: 10px; }
.tl-tag.done { background: var(--green-soft); color: var(--green); }
.tl-tag.now { background: var(--blue-soft); color: var(--blue); }
.tl-tag.todo { background: var(--amber-soft); color: var(--amber); }
.tl-item__info { font-size: 12px; color: var(--muted); margin-top: 5px; display: flex; gap: 14px; flex-wrap: wrap; }
.tl-item__info b { color: var(--slate); font-weight: 500; }
.tl-bar { margin-top: 8px; height: 5px; border-radius: 3px; background: var(--grid); overflow: hidden; }
.tl-bar i { display: block; height: 100%; border-radius: 3px; background: var(--blue); }

.list { display: flex; flex-direction: column; }
.list-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.list-item:last-child { border-bottom: none; }
.list-item__main { flex: 1; min-width: 0; }
.list-item__name { font-size: 13.5px; font-weight: 600; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.list-item__sub { font-size: 11.5px; color: var(--muted); margin-top: 3px; font-family: var(--mono); }
.mini-bar { margin-top: 7px; height: 4px; border-radius: 2px; background: var(--grid); overflow: hidden; max-width: 220px; }
.mini-bar i { display: block; height: 100%; background: var(--blue); }
.badge { font-size: 11px; padding: 2px 9px; border-radius: 10px; white-space: nowrap; }
.badge.gray { background: rgba(123,135,148,.12); color: var(--muted); }
.warn { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.warn:last-child { border-bottom: none; }
.warn__flag { width: 4px; height: 34px; border-radius: 2px; flex-shrink: 0; }
.warn__flag.yellow { background: var(--amber); }
.warn__flag.red { background: var(--red); }
.warn__main { flex: 1; min-width: 0; }
.warn__name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.warn__sub { font-size: 11.5px; color: var(--muted); margin-top: 3px; }

@media (max-width: 1080px) {
  .hero { grid-template-columns: 1fr; }
  .hero__gauge { border-left: none; border-top: 1px solid var(--line); }
}
</style>
