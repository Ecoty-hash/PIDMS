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
            <a class="login__forgot" href="#" @click.prevent="forgot">忘记密码？</a>
          </div>

          <div v-if="error" class="login__err" role="alert">{{ error }}</div>

          <button class="btn btn--primary login__submit" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner" aria-hidden="true"></span>{{ loading ? "登录中…" : "登 录" }}
          </button>
        </form>

        <p class="login__foot">没有账号？请联系系统管理员开通</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi, setToken, setUser } from "@/api/request.js";

const route = useRoute();
const router = useRouter();

const form = reactive({ username: "", password: "" });
const remember = ref(true);
const loading = ref(false);
const error = ref("");

function forgot() {
  error.value = "请联系系统管理员重置密码";
}

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

/* ================= 响应式 ================= */
@media (max-width: 900px) {
  .login__stage { display: none; }
  .login__panel { padding: 48px 24px; }
}
@media (prefers-reduced-motion: reduce) {
  .plate, .login__card { animation: none; }
}
</style>
