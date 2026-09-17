<template>
  <div class="app">
    <Sidebar :open="menuOpen" @close="menuOpen = false" />
    <div v-if="menuOpen" class="menu-scrim" @click="menuOpen = false"></div>

    <div class="shell">
      <Topbar @toggle="menuOpen = !menuOpen" />
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <ToastHost />
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import Sidebar from "./Sidebar.vue";
import Topbar from "./Topbar.vue";
import ToastHost from "@/components/ToastHost.vue";

const menuOpen = ref(false);
const router = useRouter();

// 路由切换时关闭移动端菜单
const stop = router.afterEach(() => {
  menuOpen.value = false;
});
onBeforeUnmount(() => stop());
</script>

<style scoped>
/* 页面过渡样式已在全局 main.css 中定义 (fade-page-*) */
</style>
