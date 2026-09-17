<template>
  <header class="topbar">
    <div style="display: flex; align-items: center; gap: 12px;">
      <button class="menu-toggle" @click="$emit('toggle')" aria-label="打开菜单">☰</button>
      <div class="topbar__crumb">
        <template v-if="group"><b>{{ group }}</b><span class="sep">/</span></template>
        <b>{{ title }}</b>
      </div>
    </div>
    <div class="topbar__right">
      <div class="search" style="position: relative;">
        <span class="search__icon">⌕</span>
        <input
          type="search"
          v-model="kw"
          placeholder="搜索功能模块…"
          aria-label="搜索功能模块"
          @input="onInput"
          @keydown.enter="goFirst"
        />
        <div v-if="results.length" class="search-drop">
          <div
            v-for="r in results"
            :key="r.key"
            class="search-drop__item"
            @click="jump(r.key)"
          >{{ r.group }} / {{ r.name }}</div>
        </div>
      </div>
      <div class="user">
        <div class="user__avatar">{{ avatarText }}</div>
        <div class="user__meta">
          <div class="user__name">{{ userName }}</div>
          <div class="user__role">{{ userRole }}</div>
        </div>
        <button class="user__logout" @click="logout" title="退出登录">退出</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { MODULES } from "@/config/modules.js";
import { getUser, clearAuth } from "@/api/request.js";

defineEmits(["toggle"]);
const route = useRoute();
const router = useRouter();

const user = ref(getUser());
const userName = computed(() => user.value?.realName || user.value?.username || "未登录");
const userRole = computed(() => user.value?.role || "用户");
const avatarText = computed(() => (userName.value || "?").charAt(0).toUpperCase());

function logout() {
  clearAuth();
  router.push("/login");
}

const kw = ref("");
const title = computed(() => route.meta.title || "工作台");
const group = computed(() => route.meta.group || "");

const results = computed(() => {
  const k = kw.value.trim().toLowerCase();
  if (!k) return [];
  return MODULES.filter((m) => m.name.toLowerCase().includes(k) || m.key.toLowerCase().includes(k)).slice(0, 6);
});

function onInput() {
  // 结果实时计算
}
function goFirst() {
  if (results.value.length) jump(results.value[0].key);
}
function jump(key) {
  kw.value = "";
  router.push(`/${key}`);
}
</script>

<style scoped>
.user__logout {
  margin-left: 4px;
  border: 1px solid var(--line);
  background: var(--card);
  color: var(--muted);
  border-radius: 7px;
  padding: 4px 9px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.14s;
}
.user__logout:hover { border-color: var(--red); color: var(--red); background: var(--red-soft); }

.search-drop {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0; z-index: 30;
  background: var(--card); border: 1px solid var(--line); border-radius: 8px;
  box-shadow: var(--shadow-lg); overflow: hidden;
}
.search-drop__item {
  padding: 9px 12px; font-size: 13px; color: var(--slate); cursor: pointer;
}
.search-drop__item:hover { background: var(--paper); color: var(--ink); }
</style>
