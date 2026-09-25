import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    // 监听所有网卡：本机仍可用 localhost，同网段其他设备用 http://10.19.0.12:5173 打开。
    // 这里不写死成 "10.19.0.12"：绑单个网卡后 localhost 会失效，且换网络/DHCP 变 IP 就得改代码。
    host: "0.0.0.0",
    port: 5173,
    open: false,
    proxy: {
      // 前后端联调：开发服务器将 /api 转发到后端。
      // 代理是开发服务器自己发起的请求，跟浏览器里填 localhost 还是 10.19.0.12 无关，
      // 所以后端与前端同机时保持 localhost 即可（后端也可不必对外暴露端口）。
      // 若后端在另一台机器，把 target 改成那台地址，例如 http://10.19.0.12:8080
      "/api": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
  // 生产构建本机预览（npm run preview）同样开放给局域网
  preview: {
    host: "0.0.0.0",
    port: 4173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
  build: {
    outDir: "dist",
    chunkSizeWarningLimit: 1600,
  },
});
