import { createRouter, createWebHistory } from "vue-router";
import MainLayout from "@/layout/MainLayout.vue";
import Dashboard from "@/views/Dashboard.vue";
import Visualization from "@/views/Visualization.vue";
import ProjectProgress from "@/views/ProjectProgress.vue";
import ModulePage from "@/views/ModulePage.vue";
import Login from "@/views/Login.vue";
import { MODULES } from "@/config/modules.js";
import { getToken } from "@/api/request.js";

// 每个模块生成一条路由：/project、/customer 等
const moduleRoutes = MODULES.map((m) => ({
  path: `/${m.key}`,
  name: m.key,
  component: ModulePage,
  props: { moduleKey: m.key },
  meta: { title: m.name, group: m.group },
}));

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: Login,
      meta: { title: "登录", public: true },
    },
    {
      path: "/",
      component: MainLayout,
      children: [
        { path: "", name: "dashboard", component: Dashboard, meta: { title: "工作台" } },
        // 可视化管理：全局统计大屏（工作台是个人视角，两页分工不同）
        { path: "visualization", name: "visualization", component: Visualization, meta: { title: "可视化管理" } },
        // 项目进度详情：单个项目的进度总览 + 节点管理
        { path: "project/:projectId/progress", name: "project-progress", component: ProjectProgress, meta: { title: "项目进度" } },
        ...moduleRoutes,
      ],
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
});

// 全局登录守卫：未登录跳转 /login，已登录访问 /login 跳回首页
router.beforeEach((to) => {
  const loggedIn = !!getToken();
  if (to.meta.public) {
    return loggedIn ? "/" : true;
  }
  if (!loggedIn) {
    return { path: "/login", query: to.fullPath === "/" ? {} : { redirect: to.fullPath } };
  }
  return true;
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · PIDMS` : "PIDMS";
});

export default router;
