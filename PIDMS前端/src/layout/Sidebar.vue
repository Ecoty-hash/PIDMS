<template>
  <aside class="sidebar" :class="{ 'is-open': open }">
    <div class="brand">
      <svg class="brand__mark" viewBox="0 0 32 32" fill="none" aria-hidden="true">
        <circle cx="16" cy="16" r="10" stroke="#de8a0b" stroke-width="1.6"/>
        <path d="M16 2v6M16 24v6M2 16h6M24 16h6" stroke="#7b90a5" stroke-width="1.4"/>
        <circle cx="16" cy="16" r="2.4" fill="#de8a0b"/>
      </svg>
      <div>
        <div class="brand__name">企业管理系统</div>
        <div class="brand__sub">PIDMS · CONSOLE</div>
      </div>
    </div>

    <nav class="nav" aria-label="主导航">
      <div class="nav-group">
        <div class="nav-group__title">总览</div>
        <ul class="nav-group__list">
          <li class="nav-item" :class="{ 'is-active': isActive('/') }">
            <router-link to="/" @click="emitClose"><span class="nav-item__dot"></span>工作台</router-link>
          </li>
          <li class="nav-item" :class="{ 'is-active': isActive('/visualization') }">
            <router-link to="/visualization" @click="emitClose"><span class="nav-item__dot"></span>可视化管理</router-link>
          </li>
        </ul>
      </div>

      <div v-for="g in groups" :key="g.name" class="nav-group">
        <div class="nav-group__title">{{ g.name }}</div>
        <ul class="nav-group__list">
          <li
            v-for="m in g.items"
            :key="m.key"
            class="nav-item"
            :class="{ 'is-active': isActive('/' + m.key) }"
          >
            <router-link :to="`/${m.key}`" @click="emitClose">
              <span class="nav-item__dot"></span>{{ m.name }}
            </router-link>
          </li>
        </ul>
      </div>
    </nav>

    <div class="sidebar__foot">
      <span>SITE-2026 / v1.0</span>
      <span class="dot-live" title="系统在线"></span>
    </div>
  </aside>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { MODULES } from "@/config/modules.js";

const props = defineProps({
  open: { type: Boolean, default: false },
});
const emit = defineEmits(["close"]);

const route = useRoute();
const groups = computed(() => {
  const map = new Map();
  MODULES.forEach((m) => {
    if (!map.has(m.group)) map.set(m.group, []);
    map.get(m.group).push(m);
  });
  return Array.from(map.entries()).map(([name, items]) => ({ name, items }));
});

function isActive(path) {
  return route.path === path;
}
function emitClose() {
  emit("close");
}
</script>
