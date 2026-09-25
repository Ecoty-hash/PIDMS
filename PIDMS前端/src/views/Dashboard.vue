<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { workbenchApi } from "@/api/request.js";
import { useToast } from "@/composables/useToast.js";

/**
 * 工作台（个人视角）：只看「与我有关」的事。
 * 数据来自 GET /api/workbench/overview，全部以当前登录用户为中心：
 * 待我审批 → 我的项目 → 逾期节点预警 → 最近施工日志 → 我发起的申请。
 * 全局统计看板在「可视化管理」页。
 *
 * 刷新时机：进入页面时拉一次（路由无 keep-alive，从别的模块点回「工作台」会重新挂载）；
 * 另外在窗口重新获得焦点、浏览器标签页切回来时静默刷新一次——
 * 否则在别的模块改了数据、切回已打开的工作台，看到的还是旧数字。
 */
const router = useRouter();
const toast = useToast();

const data = ref(null);
const loading = ref(true);
const error = ref("");
const fetchedAt = ref(null);

const WEATHER = { sunny: "晴", cloudy: "多云", rainy: "雨", snowy: "雪" };
const APPROVAL = {
  pending: ["amber", "待审批"],
  approved: ["green", "已通过"],
  rejected: ["red", "已驳回"],
};

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return "夜深了";
  if (h < 12) return "早上好";
  if (h < 14) return "中午好";
  if (h < 19) return "下午好";
  return "晚上好";
});

const todayText = computed(() => {
  const d = new Date();
  const week = "日一二三四五六"[d.getDay()];
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 星期${week}`;
});

const who = computed(() => data.value?.realName || data.value?.username || "");

/** 数据时间：优先用后端返回的生成时间，取不到就用本地拉到数据的时刻 */
const updatedAt = computed(() => {
  const t = data.value?.generatedAt || fetchedAt.value;
  if (!t) return "";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t).replace("T", " ").slice(11, 16);
  const p2 = (n) => String(n).padStart(2, "0");
  const now = new Date();
  const hm = `${p2(d.getHours())}:${p2(d.getMinutes())}`;
  const sameDay =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate();
  return sameDay ? hm : `${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${hm}`;
});

const updatedTitle = computed(() => {
  const t = data.value?.generatedAt || fetchedAt.value;
  return t ? "数据截至 " + String(t).replace("T", " ").slice(0, 19) : "尚未取到数据";
});

/** 数字格式化：后端返回的 BigDecimal 序列化为数字或字符串，统一处理 */
function num(v, digits = 1) {
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(digits) : (0).toFixed(digits);
}

function pct(v) {
  return Math.max(0, Math.min(100, Number(v) || 0)) + "%";
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

function tagOf(map, key) {
  return map[key] || ["gray", key || "—"];
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : "—";
}

function fmtDateTime(v) {
  return v ? String(v).replace("T", " ").slice(0, 16) : "—";
}

function go(moduleKey) {
  if (moduleKey) router.push(`/${moduleKey}`);
}

let inflight = false;

/**
 * silent = 后台静默刷新：不显示骨架、失败也不弹提示（保留上一次的数据即可），
 * 用于「切回标签页自动刷新」，避免用户正在看的时候整页闪一下。
 */
async function load({ silent = false } = {}) {
  if (inflight) return;
  inflight = true;
  if (!silent) loading.value = true;
  try {
    data.value = await workbenchApi.overview();
    fetchedAt.value = new Date().toISOString();
    error.value = "";
  } catch (e) {
    error.value = e.message || "工作台数据加载失败";
    if (!silent || !data.value) toast.error(error.value);
  } finally {
    inflight = false;
    loading.value = false;
  }
}

/** 窗口重新获得焦点 / 标签页切回来时，如果页面可见就后台刷新一次 */
function refreshIfVisible() {
  if (document.visibilityState !== "visible") return;
  load({ silent: true });
}

onMounted(() => {
  load();
  document.addEventListener("visibilitychange", refreshIfVisible);
  window.addEventListener("focus", refreshIfVisible);
});

onUnmounted(() => {
  document.removeEventListener("visibilitychange", refreshIfVisible);
  window.removeEventListener("focus", refreshIfVisible);
});
</script>

<template>
  <div class="rise">
    <div class="page-head ws-head">
      <div>
        <div class="page-head__eyebrow">PIDMS · MY WORKSPACE</div>
        <h1 class="page-head__title">工作台</h1>
      </div>
      <div class="ws-head__right">
        <div class="ws-head__hi">
          {{ greeting }}<template v-if="who">，{{ who }}</template>
          <small>{{ todayText }}</small>
        </div>
        <div class="ws-head__acts">
          <span class="ws-stamp" :title="updatedTitle">
            <template v-if="loading">加载中…</template>
            <template v-else-if="updatedAt">数据截至 {{ updatedAt }}</template>
            <template v-else>未取到数据</template>
          </span>
          <button class="btn btn--sm" :disabled="loading" @click="load">刷新</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading"><span class="spinner"></span>加载工作台…</div>

    <template v-else-if="data">
      <!-- ============ 个人指标 ============ -->
      <section class="kpis">
        <div class="kpi" :class="{ 'is-alert': data.todoTotal > 0 }">
          <div class="kpi__k">待我审批</div>
          <div class="kpi__v">{{ data.todoTotal }}<small>项</small></div>
          <div class="kpi__f">{{ data.todos.length ? data.todos.map((t) => `${t.moduleName} ${t.count}`).join(" · ") : "暂无待办" }}</div>
        </div>
        <div class="kpi">
          <div class="kpi__k">我的项目</div>
          <div class="kpi__v">{{ data.myProjectCount }}<small>个</small></div>
          <div class="kpi__f">平均实际进度 {{ num(data.myAvgProgress) }}%</div>
        </div>
        <div class="kpi" :class="{ 'is-alert': data.overdueNodeCount > 0 }">
          <div class="kpi__k">逾期节点</div>
          <div class="kpi__v">{{ data.overdueNodeCount }}<small>个</small></div>
          <div class="kpi__f">{{ data.overdueNodeCount ? "需要重点盯办" : "节点均按计划推进" }}</div>
        </div>
        <div class="kpi">
          <div class="kpi__k">我发起的申请</div>
          <div class="kpi__v">{{ data.myApplications.length }}<small>条</small></div>
          <div class="kpi__f">最近 {{ data.myApplications.length ? fmtDate(data.myApplications[0].applyTime) : "—" }}</div>
        </div>
      </section>

      <!-- ============ 待办 + 预警 ============ -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">TO APPROVE</div>
              <h2 class="panel__title">待我审批</h2>
            </div>
            <span class="tag" :class="data.todoTotal ? 'tag--amber' : 'tag--gray'">共 {{ data.todoTotal }} 项</span>
          </div>
          <div class="panel__body">
            <div v-if="!data.todos.length" class="empty">没有需要我审批的单据</div>
            <div v-else class="todo-list">
              <button v-for="t in data.todos" :key="t.moduleKey" class="todo" @click="go(t.moduleKey)">
                <span class="todo__ico"></span>
                <span class="todo__name">{{ t.moduleName }}</span>
                <span v-if="t.unassignedCount" class="todo__hint">含 {{ t.unassignedCount }} 条未指派</span>
                <span class="todo__count">{{ t.count }}</span>
                <span class="todo__arrow">→</span>
              </button>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">OVERDUE NODES</div>
              <h2 class="panel__title">逾期节点预警</h2>
            </div>
            <router-link to="/progress" class="panel__link">进度管理 →</router-link>
          </div>
          <div class="panel__body">
            <div v-if="!data.risks.length" class="empty">我的项目没有逾期节点</div>
            <div v-else class="risk-list">
              <div v-for="r in data.risks" :key="r.nodeId" class="risk" :class="`risk--${r.severity}`">
                <div class="risk__main">
                  <div class="risk__name">{{ r.nodeName }}</div>
                  <div class="risk__sub">{{ r.projectName }}</div>
                </div>
                <div class="risk__meta">
                  <span class="risk__days">逾期 {{ r.overdueDays }} 天</span>
                  <span class="risk__plan">计划 {{ fmtDate(r.planEndDate) }}</span>
                </div>
              </div>
              <div v-if="data.overdueNodeCount > data.risks.length" class="risk-more">
                另有 {{ data.overdueNodeCount - data.risks.length }} 个逾期节点，去<router-link to="/progress">进度管理</router-link>查看
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- ============ 我的项目 ============ -->
      <section class="panel mt20">
        <div class="panel__head">
          <div>
            <div class="panel__eyebrow">MY PROJECTS</div>
            <h2 class="panel__title">我的项目</h2>
            <div class="panel__hint">口径：我担任负责人、项目成员，或由我创建的项目</div>
          </div>
          <div class="legend-inline">
            <span><i class="lg-fill"></i>实际进度</span>
            <span><i class="lg-tick"></i>计划进度</span>
          </div>
        </div>
        <div class="panel__body">
          <div v-if="!data.myProjects.length" class="empty">还没有与我相关的项目（负责人 / 成员 / 创建人）</div>
          <div v-else class="proj">
            <div v-for="p in data.myProjects" :key="p.projectId" class="proj-row">
              <div class="proj-row__name">
                <router-link :to="`/project/${p.projectId}/progress`">{{ p.projectName }}</router-link>
                <div class="proj-row__sub">
                  {{ p.projectCode || "无编号" }} · 负责人 {{ p.projectLeader || "未指定" }} ·
                  {{ p.projectStatusName }}
                </div>
              </div>
              <div class="proj-row__bar">
                <div class="bar" :title="`实际 ${num(p.actualProgress)}% · 计划 ${num(p.planProgress)}%`">
                  <i class="bar__fill" :style="{ width: pct(p.actualProgress) }"></i>
                  <span class="bar__tick" :style="{ left: pct(p.planProgress) }"></span>
                </div>
              </div>
              <div class="proj-row__val">
                <span class="proj-row__num">{{ num(p.actualProgress) }}%</span>
                <span class="proj-row__delta" :class="deltaClass(p.progressDelta)">{{ signed(p.progressDelta) }}</span>
              </div>
              <div class="proj-row__node">
                <span v-if="p.overdueNodeCount" class="tag tag--red">逾期 {{ p.overdueNodeCount }}</span>
                <span v-else class="tag tag--gray">{{ p.completedNodeCount }}/{{ p.nodeCount }} 节点</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ 动态 + 申请 ============ -->
      <div class="grid-2">
        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">RECENT LOGS</div>
              <h2 class="panel__title">最近施工日志</h2>
            </div>
            <router-link to="/construction-log" class="panel__link">全部日志 →</router-link>
          </div>
          <div class="panel__body">
            <div v-if="!data.recentLogs.length" class="empty">我的项目还没有施工日志</div>
            <div v-else class="timeline">
              <div v-for="l in data.recentLogs" :key="l.id" class="tl">
                <div class="tl__head">
                  <span class="tl__date">{{ fmtDate(l.logDate) }}</span>
                  <span class="tag tag--blue">{{ WEATHER[l.weather] || l.weather || "—" }}</span>
                  <span class="tl__who">{{ l.createBy }}</span>
                </div>
                <div class="tl__title">{{ l.projectName }}<template v-if="l.planName"> · {{ l.planName }}</template></div>
                <div class="tl__text">{{ l.constructionContent || l.constructionLocation || "—" }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">MY REQUESTS</div>
              <h2 class="panel__title">我发起的申请</h2>
            </div>
          </div>
          <div class="panel__body">
            <div v-if="!data.myApplications.length" class="empty">我还没有发起过申请</div>
            <div v-else class="apps">
              <div v-for="a in data.myApplications" :key="a.moduleKey + a.billNo" class="app">
                <div class="app__main">
                  <div class="app__name">{{ a.title || a.moduleName }}</div>
                  <div class="app__sub">{{ a.billNo || "—" }} · {{ a.moduleName }}</div>
                </div>
                <div class="app__meta">
                  <span class="tag" :class="`tag--${tagOf(APPROVAL, a.approvalStatus)[0]}`">{{ tagOf(APPROVAL, a.approvalStatus)[1] }}</span>
                  <span class="app__time">{{ fmtDateTime(a.applyTime) }}</span>
                  <span class="app__approver">审批人 {{ a.approver || "未指派" }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <p class="ws-note">{{ data.description }}</p>
    </template>

    <div v-else class="empty">
      <div>工作台数据加载失败</div>
      <div v-if="error" class="empty__err">{{ error }}</div>
      <button class="btn btn--sm" style="margin-top: 12px" @click="load">重试</button>
    </div>
  </div>
</template>

<style scoped>
.ws-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.ws-head__right { display: flex; align-items: center; gap: 16px; }
.ws-head__acts { display: flex; align-items: center; gap: 10px; }
.ws-stamp { font-size: 11.5px; color: var(--muted); font-family: var(--mono); white-space: nowrap; }
.empty__err {
  margin-top: 8px; font-size: 12px; color: var(--red); font-family: var(--mono);
  max-width: 560px; word-break: break-all;
}
.panel__hint { font-size: 11.5px; color: var(--muted); margin-top: 4px; }
.ws-head__hi { font-size: 13.5px; color: var(--slate); text-align: right; }
.ws-head__hi small { display: block; font-size: 11.5px; color: var(--muted); font-family: var(--mono); margin-top: 2px; }
.mt20 { margin-top: 20px; }

/* 指标卡 */
.kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.kpi {
  background: var(--card); border: 1px solid var(--line); border-radius: var(--radius);
  padding: 16px 18px 14px; box-shadow: var(--shadow-sm); position: relative; overflow: hidden;
}
.kpi::before {
  content: ""; position: absolute; left: 0; top: 14px; bottom: 14px; width: 3px;
  background: var(--line-strong); border-radius: 0 3px 3px 0;
}
.kpi.is-alert::before { background: var(--amber); }
.kpi__k { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.kpi__v { font-family: var(--num); font-size: 30px; font-weight: 600; color: var(--ink); line-height: 1.1; }
.kpi__v small { font-size: 13px; color: var(--muted); font-weight: 500; margin-left: 3px; }
.kpi__f {
  font-size: 11.5px; color: var(--muted); margin-top: 7px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* 待我审批 */
.todo-list { display: flex; flex-direction: column; gap: 8px; }
.todo {
  display: flex; align-items: center; gap: 10px; width: 100%; text-align: left;
  padding: 11px 13px; border: 1px solid var(--line); border-radius: var(--radius-sm);
  background: var(--card); cursor: pointer; transition: all .16s;
}
.todo:hover { border-color: var(--blue); background: var(--blue-soft); }
.todo__ico {
  width: 7px; height: 7px; border-radius: 50%; background: var(--amber); flex-shrink: 0;
  box-shadow: 0 0 0 3px var(--amber-soft);
}
.todo__name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.todo__hint { font-size: 11.5px; color: var(--muted); margin-left: 2px; }
.todo__count {
  margin-left: auto; font-family: var(--num); font-size: 18px; font-weight: 600; color: var(--amber);
}
.todo__arrow { color: var(--muted); font-size: 13px; }

/* 逾期预警 */
.risk-list { display: flex; flex-direction: column; }
.risk { display: flex; align-items: center; gap: 12px; padding: 10px 0 10px 12px; border-bottom: 1px solid var(--line); position: relative; }
.risk:last-child { border-bottom: none; }
.risk::before { content: ""; position: absolute; left: 0; top: 12px; bottom: 12px; width: 3px; border-radius: 2px; }
.risk--serious::before { background: var(--red); }
.risk--warning::before { background: var(--amber); }
.risk--notice::before { background: var(--blue); }
.risk__main { flex: 1; min-width: 0; }
.risk__name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.risk__sub {
  font-size: 11.5px; color: var(--muted); margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 300px;
}
.risk__meta { text-align: right; flex-shrink: 0; }
.risk__days { display: block; font-family: var(--num); font-weight: 600; font-size: 14px; color: var(--red); }
.risk--warning .risk__days { color: var(--amber); }
.risk--notice .risk__days { color: var(--blue); }
.risk__plan { font-size: 11px; color: var(--muted); font-family: var(--mono); }
.risk-more { font-size: 12px; color: var(--muted); padding-top: 10px; }
.risk-more a { color: var(--blue); }

/* 我的项目 */
.legend-inline { display: flex; gap: 16px; font-size: 12px; color: var(--muted); }
.legend-inline i { display: inline-block; vertical-align: middle; margin-right: 6px; }
.lg-fill { width: 14px; height: 7px; border-radius: 2px; background: var(--blue); }
.lg-tick { width: 2px; height: 12px; background: var(--amber); }
.proj { display: flex; flex-direction: column; }
.proj-row {
  display: grid; grid-template-columns: minmax(200px, 1.4fr) minmax(160px, 2fr) 96px 96px;
  align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--line);
}
.proj-row:last-child { border-bottom: none; }
.proj-row__name { min-width: 0; }
.proj-row__name a { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.proj-row__name a:hover { color: var(--blue); }
.proj-row__sub { font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.bar { position: relative; height: 9px; border-radius: 5px; background: var(--grid); }
.bar__fill {
  position: absolute; left: 0; top: 0; bottom: 0; border-radius: 5px;
  background: linear-gradient(90deg, var(--blue), #3b86e0); transition: width .6s cubic-bezier(.2,.8,.2,1);
}
.bar__tick { position: absolute; top: -3px; bottom: -3px; width: 2px; background: var(--amber); border-radius: 1px; }
.proj-row__val { display: flex; align-items: baseline; gap: 7px; justify-content: flex-end; }
.proj-row__num { font-family: var(--num); font-size: 16px; font-weight: 600; color: var(--ink); }
.proj-row__delta { font-family: var(--mono); font-size: 11.5px; }
.proj-row__delta.is-ok { color: var(--green); }
.proj-row__delta.is-warn { color: var(--amber); }
.proj-row__delta.is-bad { color: var(--red); }
.proj-row__node { text-align: right; }

/* 施工日志 */
.timeline { position: relative; padding-left: 18px; }
.timeline::before { content: ""; position: absolute; left: 3px; top: 8px; bottom: 8px; width: 1px; background: var(--line); }
.tl { position: relative; padding-bottom: 16px; }
.tl:last-child { padding-bottom: 0; }
.tl::before {
  content: ""; position: absolute; left: -18px; top: 6px; width: 7px; height: 7px;
  border-radius: 50%; background: var(--card); border: 2px solid var(--blue);
}
.tl__head { display: flex; align-items: center; gap: 9px; }
.tl__date { font-family: var(--mono); font-size: 12px; color: var(--ink); font-weight: 600; }
.tl__who { font-size: 11.5px; color: var(--muted); margin-left: auto; }
.tl__title { font-size: 13px; color: var(--ink); font-weight: 500; margin-top: 5px; }
.tl__text { font-size: 12px; color: var(--slate); margin-top: 3px; }

/* 我的申请 */
.apps { display: flex; flex-direction: column; }
.app { display: flex; align-items: center; gap: 12px; padding: 11px 0; border-bottom: 1px solid var(--line); }
.app:last-child { border-bottom: none; }
.app__main { flex: 1; min-width: 0; }
.app__name {
  font-size: 13.5px; font-weight: 600; color: var(--ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.app__sub { font-size: 11.5px; color: var(--muted); font-family: var(--mono); margin-top: 3px; }
.app__meta { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.app__time, .app__approver { font-size: 11.5px; color: var(--muted); }
.ws-note {
  margin: 18px 2px 0; font-size: 11.5px; color: var(--muted); line-height: 1.7;
  font-family: var(--mono);
}

.panel__link { font-size: 12.5px; color: var(--blue); }

@media (max-width: 1200px) {
  .kpis { grid-template-columns: repeat(2, 1fr); }
  .proj-row { grid-template-columns: 1fr; gap: 8px; }
  .proj-row__val, .proj-row__node { justify-content: flex-start; text-align: left; }
}
@media (max-width: 1080px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
