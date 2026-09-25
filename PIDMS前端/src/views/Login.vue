<template>
  <div class="login">
    <!-- 左侧：蓝图舞台 -->
    <aside class="login__stage">
      <div class="stage-grid" aria-hidden="true"></div>
      <span class="stage-corner tl" aria-hidden="true"></span>
      <span class="stage-corner tr" aria-hidden="true"></span>
      <span class="stage-corner bl" aria-hidden="true"></span>
      <span class="stage-corner br" aria-hidden="true"></span>

      <div class="plate rise">
        <header class="plate__head">
          <div class="plate__logo">
            <span class="plate__mark">P</span>
            <div class="plate__brand">
              <div class="plate__name">PIDMS</div>
              <div class="plate__sub">PROJECT · INFRASTRUCTURE · DATA MANAGEMENT</div>
            </div>
          </div>
          <div class="plate__code">DOC № PM-2026-001</div>
        </header>

        <div class="plate__body">
          <div class="plate__kicker">工程数据管理平台</div>
          <h1 class="plate__title">项目全生命周期<br />数据管理系统</h1>
          <p class="plate__desc">从立项、预算、文档到进度与验收，<br />一份蓝图贯穿项目始终。</p>
        </div>

        <footer class="plate__tblock">
          <div class="tblock__cell"><span>PROJECT</span><b>PIDMS 2026</b></div>
          <div class="tblock__cell"><span>REV</span><b>V1.0</b></div>
          <div class="tblock__cell"><span>SCALE</span><b>—</b></div>
          <div class="tblock__cell"><span>DATE</span><b>2026-09-01</b></div>
        </footer>
      </div>

      <p class="stage__foot">© 2026 PIDMS · 项目与基础设施数据管理</p>
    </aside>

    <!-- 右侧：登录表单 -->
    <main class="login__panel">
      <div class="login__card rise">
        <div class="login__eyebrow">PIDMS · 系统入口</div>
        <h1 class="login__title">欢迎回来</h1>
        <p class="login__hint">登录后进入项目数据管理工作台</p>

        <form class="login__form" @submit.prevent="submit" novalidate>
          <div class="login__field">
            <label for="username">用户名</label>
            <input
              id="username"
              v-model.trim="form.username"
              type="text"
              autocomplete="username"
              placeholder="请输入用户名"
              autofocus
            />
          </div>

          <div class="login__field">
            <label for="password">密码</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
          </div>

          <div class="login__row">
            <label class="login__remember">
              <input type="checkbox" v-model="remember" />
              记住我
            </label>
            <a class="login__forgot" href="#" @click.prevent="openForgot">忘记密码？</a>
          </div>

          <div v-if="error" class="login__err" role="alert">{{ error }}</div>

          <button class="btn btn--primary login__submit" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner" aria-hidden="true"></span>{{ loading ? "登录中…" : "登 录" }}
          </button>
        </form>

        <p class="login__foot">没有账号？请联系系统管理员开通</p>
      </div>
    </main>

    <!-- ================= 忘记密码：两步核验 → 重置 ================= -->
    <div v-if="forgot.visible" class="modal-overlay forgot-overlay" @mousedown.self="closeForgot">
      <div class="modal forgot-modal" role="dialog" aria-modal="true" aria-labelledby="forgotTitle">
        <div class="modal__head">
          <div id="forgotTitle" class="modal__title">重置登录密码</div>
          <button class="modal__close" type="button" aria-label="关闭" @click="closeForgot">✕</button>
        </div>

        <div class="modal__body">
          <div class="fp__steps" aria-hidden="true">
            <span class="fp__step" :class="{ 'is-on': forgot.step >= 1 }">01</span>
            <i class="fp__bar"></i>
            <span class="fp__step" :class="{ 'is-on': forgot.step >= 2 }">02</span>
          </div>

          <!-- 第一步：用户名 + 手机号核验 -->
          <template v-if="forgot.step === 1">
            <p class="fp__lead">
              请输入该账号绑定的<strong>用户名</strong>与<strong>手机号</strong>，
              二者与系统记录一致才能重置密码。
            </p>

            <form class="fp__form" @submit.prevent="verifyIdentity">
              <div class="login__field">
                <label for="fp-username">用户名</label>
                <input
                  id="fp-username"
                  ref="fpUserEl"
                  v-model.trim="forgot.username"
                  type="text"
                  autocomplete="username"
                  placeholder="请输入用户名"
                />
              </div>

              <div class="login__field">
                <label for="fp-phone">手机号</label>
                <input
                  id="fp-phone"
                  v-model.trim="forgot.phone"
                  type="tel"
                  inputmode="numeric"
                  maxlength="20"
                  autocomplete="tel"
                  placeholder="请输入账号绑定的手机号"
                />
              </div>

              <div v-if="forgot.error" class="login__err" role="alert">{{ forgot.error }}</div>

              <div class="fp__foot">
                <button class="btn" type="button" @click="closeForgot">取消</button>
                <button class="btn btn--primary" type="submit" :disabled="forgot.loading">
                  <span v-if="forgot.loading" class="spinner" aria-hidden="true"></span>
                  {{ forgot.loading ? "核验中…" : "验证身份" }}
                </button>
              </div>
            </form>
          </template>

          <!-- 第二步：设置新密码 -->
          <template v-else-if="forgot.step === 2">
            <div class="fp__ok">
              <span class="fp__ok-dot" aria-hidden="true"></span>
              <div>
                <b>{{ forgot.username }}</b> 身份核验通过
                <span class="fp__ttl">票据有效期剩余 {{ ttlText }}</span>
              </div>
            </div>

            <form class="fp__form" @submit.prevent="submitReset">
              <div class="login__field">
                <label for="fp-new">新密码</label>
                <input
                  id="fp-new"
                  ref="fpPwdEl"
                  v-model="forgot.newPassword"
                  type="password"
                  autocomplete="new-password"
                  :placeholder="`请输入新密码（${PWD_MIN}-${PWD_MAX} 位）`"
                />
              </div>

              <div class="login__field">
                <label for="fp-confirm">确认新密码</label>
                <input
                  id="fp-confirm"
                  v-model="forgot.confirmPassword"
                  type="password"
                  autocomplete="new-password"
                  placeholder="请再次输入新密码"
                />
              </div>

              <div v-if="forgot.error" class="login__err" role="alert">{{ forgot.error }}</div>

              <div class="fp__foot">
                <button class="btn" type="button" :disabled="forgot.loading" @click="backToStep1">返回上一步</button>
                <button class="btn btn--primary" type="submit" :disabled="forgot.loading">
                  <span v-if="forgot.loading" class="spinner" aria-hidden="true"></span>
                  {{ forgot.loading ? "提交中…" : "确认重置" }}
                </button>
              </div>
            </form>
          </template>

          <!-- 第三步：重置完成 -->
          <template v-else>
            <div class="fp__done">
              <div class="fp__tick" aria-hidden="true">✓</div>
              <div class="fp__done-title">密码重置成功</div>
              <p class="fp__done-desc">请使用新密码重新登录，原密码已失效。</p>
            </div>

            <div class="fp__foot fp__foot--center">
              <button class="btn btn--primary" type="button" @click="backToLogin">去登录</button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi, setToken, setUser } from "@/api/request.js";

const route = useRoute();
const router = useRouter();

const form = reactive({ username: "", password: "" });
const remember = ref(true);
const loading = ref(false);
const error = ref("");

// =========================================================================
// 忘记密码：两步走。
//   第一步 POST /api/auth/forgot-password/verify { username, phone }
//          → { resetTicket, expiresIn }，票据 5 分钟有效、只能用一次
//   第二步 POST /api/auth/reset-password { resetTicket, newPassword }
// 无短信验证码，靠「用户名 + 手机号」与库中记录一一对应来核验。
// =========================================================================
const PWD_MIN = 4;
const PWD_MAX = 32;

const forgot = reactive({
  visible: false,
  step: 1, // 1 核验身份 / 2 设置新密码 / 3 完成
  username: "",
  phone: "",
  newPassword: "",
  confirmPassword: "",
  ticket: "",
  error: "",
  loading: false,
  remain: 0, // 票据剩余秒数
});

const fpUserEl = ref(null);
const fpPwdEl = ref(null);
let ttlTimer = null;

/** 票据倒计时 mm:ss */
const ttlText = computed(() => {
  const s = Math.max(0, forgot.remain);
  return `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
});

function openForgot() {
  Object.assign(forgot, {
    visible: true,
    step: 1,
    username: form.username || "", // 登录框已填的用户名带过来，少打一次
    phone: "",
    newPassword: "",
    confirmPassword: "",
    ticket: "",
    error: "",
    loading: false,
    remain: 0,
  });
  focusForgot();
}

function closeForgot() {
  if (forgot.loading) return; // 请求中不让关，避免票据和界面状态错位
  stopTtl();
  forgot.visible = false;
}

function focusForgot() {
  nextTick(() => {
    if (forgot.step === 1) fpUserEl.value?.focus();
    else if (forgot.step === 2) fpPwdEl.value?.focus();
  });
}

function startTtl(seconds) {
  stopTtl();
  const n = Number(seconds);
  forgot.remain = Number.isFinite(n) && n > 0 ? n : 300;
  ttlTimer = setInterval(() => {
    forgot.remain -= 1;
    if (forgot.remain <= 0) {
      stopTtl();
      backToStep1();
      forgot.error = "身份核验已超时（票据 5 分钟有效），请重新验证";
    }
  }, 1000);
}

function stopTtl() {
  if (ttlTimer) {
    clearInterval(ttlTimer);
    ttlTimer = null;
  }
}

/** 第一步：核验用户名 + 手机号 */
async function verifyIdentity() {
  forgot.error = "";
  if (!forgot.username) {
    forgot.error = "请输入用户名";
    return;
  }
  if (!forgot.phone) {
    forgot.error = "请输入手机号";
    return;
  }

  forgot.loading = true;
  try {
    const data = await authApi.verifyForgotPassword({ username: forgot.username, phone: forgot.phone });
    const ticket = data?.resetTicket || "";
    if (!ticket) {
      throw new Error("核验成功但响应缺少 resetTicket 字段，请检查后端 /api/auth/forgot-password/verify 的 data 结构");
    }
    forgot.ticket = ticket;
    forgot.newPassword = "";
    forgot.confirmPassword = "";
    forgot.step = 2;
    startTtl(data?.expiresIn);
    focusForgot();
  } catch (e) {
    forgot.error = e.message || "核验失败，请稍后重试";
  } finally {
    forgot.loading = false;
  }
}

/** 第二步：凭票据设置新密码 */
async function submitReset() {
  forgot.error = "";
  if (forgot.newPassword.length < PWD_MIN) {
    forgot.error = `新密码长度不能少于 ${PWD_MIN} 位`;
    return;
  }
  if (forgot.newPassword.length > PWD_MAX) {
    forgot.error = `新密码长度不能超过 ${PWD_MAX} 位`;
    return;
  }
  if (forgot.newPassword !== forgot.confirmPassword) {
    forgot.error = "两次输入的新密码不一致";
    return;
  }

  forgot.loading = true;
  try {
    await authApi.resetPassword({ resetTicket: forgot.ticket, newPassword: forgot.newPassword });
    stopTtl();
    forgot.step = 3;
  } catch (e) {
    const msg = e.message || "重置失败，请稍后重试";
    // 票据过期 / 已用过：退回第一步重新核验
    if (msg.includes("票据")) backToStep1();
    forgot.error = msg;
  } finally {
    forgot.loading = false;
  }
}

/** 退回第一步，清掉票据与已输入的密码 */
function backToStep1() {
  stopTtl();
  forgot.step = 1;
  forgot.ticket = "";
  forgot.newPassword = "";
  forgot.confirmPassword = "";
  forgot.error = "";
  focusForgot();
}

/** 重置成功 → 回登录表单，带过去用户名，把焦点落在密码框 */
function backToLogin() {
  stopTtl();
  forgot.visible = false;
  if (forgot.username) form.username = forgot.username;
  form.password = "";
  error.value = "";
  nextTick(() => document.getElementById("password")?.focus());
}

function onKeydown(e) {
  if (e.key === "Escape" && forgot.visible) closeForgot();
}

onMounted(() => window.addEventListener("keydown", onKeydown));
onBeforeUnmount(() => {
  window.removeEventListener("keydown", onKeydown);
  stopTtl();
});

async function submit() {
  error.value = "";
  if (!form.username) {
    error.value = "请输入用户名";
    return;
  }
  if (!form.password) {
    error.value = "请输入密码";
    return;
  }

  loading.value = true;
  try {
    const data = await authApi.login({ username: form.username, password: form.password });
    const token = data?.token || data?.accessToken || "";
    if (!token) {
      throw new Error("登录成功但响应缺少 token 字段，请检查后端 /api/auth/login 的 data 结构");
    }
    setToken(token, remember.value);
    if (data?.user) setUser(data.user, remember.value);
    router.replace(route.query.redirect || "/");
  } catch (e) {
    error.value = e.message || "登录失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login {
  display: flex;
  min-height: 100vh;
}

/* ================= 左侧：蓝图舞台 ================= */
.login__stage {
  position: relative;
  flex: 1.15;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px clamp(36px, 6vw, 96px);
  overflow: hidden;
  background: linear-gradient(152deg, #1a3854 0%, #122c47 46%, #0b1f33 100%);
  color: #dfe7f1;
}
.stage-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(130, 170, 205, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(130, 170, 205, 0.08) 1px, transparent 1px);
  background-size: 34px 34px;
}
.stage-corner {
  position: absolute;
  width: 26px;
  height: 26px;
  opacity: 0.85;
  pointer-events: none;
}
.stage-corner::before,
.stage-corner::after {
  content: "";
  position: absolute;
  background: rgba(147, 191, 226, 0.55);
}
.stage-corner::before { left: 0; right: 0; top: 50%; height: 1px; }
.stage-corner::after  { top: 0; bottom: 0; left: 50%; width: 1px; }
.stage-corner.tl { top: 26px; left: 26px; }
.stage-corner.tr { top: 26px; right: 26px; }
.stage-corner.bl { bottom: 26px; left: 26px; }
.stage-corner.br { bottom: 26px; right: 26px; }

/* —— 图纸 —— */
.plate {
  position: relative;
  max-width: 520px;
  background: #eef2f7;
  border: 1px solid rgba(9, 27, 46, 0.9);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.06),
    0 30px 70px rgba(4, 14, 26, 0.5);
  color: #16324f;
}
.plate__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid #c9d5e2;
}
.plate__logo { display: flex; align-items: center; gap: 13px; }
.plate__mark {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: #16324f;
  color: #e8eef5;
  font-family: var(--num);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.5px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.14);
}
.plate__name {
  font-family: var(--num);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #16324f;
  line-height: 1;
}
.plate__sub {
  margin-top: 5px;
  font-family: var(--mono);
  font-size: 8.5px;
  letter-spacing: 1.2px;
  color: #6b7f92;
}
.plate__code {
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: 0.8px;
  color: #5a6b7c;
  white-space: nowrap;
  padding-top: 4px;
}
.plate__body { padding: 34px 26px 38px; }
.plate__kicker {
  font-family: var(--mono);
  font-size: 11px;
  letter-spacing: 3px;
  color: #1d6fd1;
  text-transform: uppercase;
  margin-bottom: 14px;
}
.plate__title {
  font-family: var(--num);
  font-size: clamp(26px, 3.2vw, 34px);
  font-weight: 700;
  line-height: 1.25;
  color: #16324f;
  margin: 0 0 16px;
  letter-spacing: -0.3px;
}
.plate__desc {
  font-size: 13.5px;
  line-height: 1.8;
  color: #3f5a75;
  margin: 0;
}

/* 图签栏（工程图纸标题栏） */
.plate__tblock {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid #c9d5e2;
}
.tblock__cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px 13px;
  border-left: 1px solid #c9d5e2;
}
.tblock__cell:first-child { border-left: none; }
.tblock__cell span {
  font-family: var(--mono);
  font-size: 9px;
  letter-spacing: 1.4px;
  color: #8a99ab;
}
.tblock__cell b {
  font-family: var(--num);
  font-size: 12.5px;
  font-weight: 600;
  color: #1e3f61;
  letter-spacing: 0.3px;
}

.stage__foot {
  position: absolute;
  left: clamp(36px, 6vw, 96px);
  bottom: 26px;
  margin: 0;
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: 1.4px;
  color: rgba(190, 212, 231, 0.55);
}

/* ================= 右侧：登录表单 ================= */
.login__panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--paper);
  padding: 40px 24px;
}
.login__card {
  width: 100%;
  max-width: 360px;
}
.login__eyebrow {
  font-family: var(--mono);
  font-size: 11px;
  letter-spacing: 2.6px;
  text-transform: uppercase;
  color: var(--blue);
  margin-bottom: 10px;
}
.login__title {
  font-family: var(--num);
  font-size: 27px;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}
.login__hint { font-size: 13.5px; color: var(--muted); margin: 0; }

.login__form { margin-top: 30px; display: flex; flex-direction: column; gap: 18px; }
.login__field { display: flex; flex-direction: column; gap: 7px; }
.login__field label { font-size: 12.5px; color: var(--steel); font-weight: 600; letter-spacing: 0.2px; }
.login__field input {
  width: 100%;
  padding: 11px 13px;
  border: 1px solid var(--line-strong);
  border-radius: 9px;
  font-size: 14px;
  color: var(--ink);
  background: #fff;
  transition: border-color 0.16s, box-shadow 0.16s, background 0.16s;
}
.login__field input::placeholder { color: #aab7c6; }
.login__field input:hover { border-color: #a9b8c8; }
.login__field input:focus { outline: none; border-color: var(--blue); box-shadow: var(--ring); background: #fff; }

.login__row { display: flex; align-items: center; justify-content: space-between; margin-top: -4px; }
.login__remember {
  display: inline-flex; align-items: center; gap: 7px;
  font-size: 12.5px; color: var(--slate); cursor: pointer; user-select: none;
}
.login__remember input { accent-color: var(--blue); width: 14px; height: 14px; cursor: pointer; }
.login__forgot { font-size: 12.5px; color: var(--blue); font-weight: 500; }
.login__forgot:hover { text-decoration: underline; }

.login__err {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--red-soft);
  color: var(--red);
  font-size: 12.5px;
  line-height: 1.55;
}

.login__submit {
  width: 100%;
  justify-content: center;
  padding: 11px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 2px;
}

.login__foot {
  margin-top: 26px;
  text-align: center;
  font-size: 12px;
  color: var(--muted);
}

/* ================= 忘记密码弹窗 ================= */
/* 登录页右半是浅色纸面、左半是深色舞台，这里补一层轻遮罩把注意力收到弹窗上 */
.forgot-overlay {
  z-index: 200;
  background: rgba(11, 31, 51, 0.44);
}
.forgot-modal {
  width: min(420px, 92vw);
}
.forgot-modal .modal__body {
  background: var(--card);
  padding: 20px 24px 22px;
}

/* 步骤指示：01 —— 02 */
.fp__steps {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.fp__step {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  font-family: var(--mono);
  font-size: 11px;
  color: var(--muted);
  border: 1px solid var(--line-strong);
  border-radius: 50%;
  transition: all 0.18s;
}
.fp__step.is-on {
  color: #fff;
  background: var(--blue);
  border-color: var(--blue);
}
.fp__bar {
  flex: 1;
  height: 1px;
  background: var(--line-strong);
}

.fp__lead {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--steel);
}
.fp__lead strong {
  color: var(--ink);
  font-weight: 600;
}

.fp__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.fp__foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 2px;
}
.fp__foot--center {
  justify-content: center;
}

/* 核验通过 + 票据倒计时 */
.fp__ok {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 13px;
  margin-bottom: 18px;
  font-size: 12.5px;
  line-height: 1.5;
  color: #176a46;
  background: var(--green-soft);
  border: 1px solid rgba(30, 142, 94, 0.28);
  border-radius: 9px;
}
.fp__ok b {
  font-family: var(--mono);
  font-weight: 600;
  color: #10583a;
}
.fp__ok-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--green);
}
.fp__ttl {
  display: block;
  margin-top: 2px;
  font-family: var(--mono);
  font-size: 11px;
  letter-spacing: 0.6px;
  color: #3d7f61;
}

/* 完成态 */
.fp__done {
  padding: 14px 0 22px;
  text-align: center;
}
.fp__tick {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin: 0 auto 14px;
  font-size: 22px;
  font-weight: 700;
  color: var(--green);
  background: var(--green-soft);
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(30, 142, 94, 0.3);
}
.fp__done-title {
  font-family: var(--num);
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
}
.fp__done-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--muted);
}

/* ================= 响应式 ================= */
@media (max-width: 900px) {
  .login__stage { display: none; }
  .login__panel { padding: 48px 24px; }
}
@media (prefers-reduced-motion: reduce) {
  .plate, .login__card { animation: none; }
}
</style>
