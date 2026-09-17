# PIDMS 前端（企业管理系统）

基于 **Vue3 + Vite** 的前端工程。页面需求与字段以《DLS原型图》为准，数据层与《PIDMS接口文档》完全对齐。采用「配置驱动的通用 CRUD」架构：29 个业务模块共用一套页面组件，模块配置即页面。

## 快速开始

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器 → http://localhost:5173
npm run build      # 生产构建，产物输出到 dist/
npm run preview    # 预览生产构建 → http://localhost:4173
```

> ⚠️ **页面空白排查**
> 本项目是 Vue3 + Vite 的 ES Module 工程，**必须通过服务器访问**，不能直接双击 `index.html` 打开（file:// 下模块无法加载，页面会一片空白）。请先 `npm install`，再 `npm run dev`，然后访问终端输出的地址（默认 http://localhost:5173）。若构建后部署，`npm run preview` 或把 `dist/` 放到任意静态服务器即可。

## 项目结构

```
PIDMS前端/
├── index.html                 # SPA 入口
├── vite.config.js             # Vite 配置（含 /api 代理占位）
├── package.json
└── src/
    ├── main.js / App.vue      # 应用入口
    ├── styles/main.css        # 设计系统（工程勘测仪表台：蓝图墨蓝 + 草图纸冷白 + 安全琥珀）
    ├── config/modules.js      # ★ 29个模块配置（由原型字段自动生成）
    ├── api/
    │   ├── request.js         # 统一请求层 + getModuleApi() + 看板接口
    │   └── mock.js            # 内存 mock 数据库（与接口文档同构）
    ├── composables/useToast.js# 轻量提示
    ├── layout/                # MainLayout / Sidebar / Topbar
    ├── components/            # DataTable / ModalForm / FieldControl / DetailPanel / SearchPanel / StatusBadge / ConfirmDialog / ToastHost
    └── views/
        ├── Dashboard.vue      # 工作台（项目总览看板）
        └── ModulePage.vue     # ★ 通用 CRUD 页面（所有模块共用）
```

## 已覆盖模块（28 个业务模块 + 工作台）

- **项目管理**：项目、项目预算、项目文档、项目状态、用印申请、印章类型
- **施工管理**：进度管理、质量检查、质量检查项、质量整改单、安全检查、安全检查项、安全整改单、施工日志、预警规则
- **基础管理**：基础资料、客户、供应商、内部单位、工种类型、预算类型、公司文档、出差申请、请假申请、补卡申请
- **系统管理**：人员管理、机构管理、角色管理
- **工作台**（`/`）：项目总进度、状态分布、质量/安全合格率、进度趋势、关键节点、进行中项目、预警规则

每个模块均支持：关键字搜索、高级搜索、分页、新增、编辑、详情查看、删除、导出/导入；并按模块带审批（用印/预算/出差/请假/补卡）、归档（项目）、封存/解封（印章/工种/项目状态/检查项）、启停（预警规则）等特殊操作。

## 架构说明

### 配置驱动（config/modules.js）

每个模块一份配置，包含 `columns`（列表列）、`search`（高级搜索）、`form`（表单字段）、`detail`（详情字段）、`toolbar`、`rowActions`、`special`（特殊操作）。`ModulePage.vue` 读取配置即可渲染完整 CRUD 页面。字段配置由 `build_modules.py` 从原型数据自动生成，与原型保持一致。

### 数据层（api/request.js）

```js
import { getModuleApi } from "@/api/request.js";
const api = getModuleApi(module);   // 依据模块配置生成 CRUD 方法
api.list(params); api.get(id); api.create(data); api.update(id, data); api.remove(id);
api.special(action, id);           // 审批/归档/封存/启停/下载等
```

### 接入真实后端

1. 修改 `src/api/request.js` 中 `CONFIG.useMock` 为 `false`。
2. 在 `vite.config.js` 配置代理：`'/api': { target: 'http://你的后端地址', changeOrigin: true }`。
3. 登录接口返回令牌后调用 `setToken(token)`（`request.js` 中导出）。
4. mock 响应结构与接口文档 `data` 完全同构，替换后页面无需改动。

## 与《PIDMS接口文档》对应关系

| 前端调用 | 接口文档 |
|---|---|
| `getModuleApi(m).list/create/update/remove` | 各模块 `GET/POST/PUT/DELETE /api/{resource}[/{id}]` |
| `api.special(sp, id)` | 各模块特殊操作 `/api/{resource}/{id}/archive、approve、seal…` |
| `dashboardApi.overview()` | `GET /api/visualization/dashboard` |
| `dashboardApi.progressTrend(g)` | `GET /api/visualization/progress-trend?granularity=` |
| `dashboardApi.inspectionStats()` | `GET /api/visualization/inspection-stats` |
| `dashboardApi.keyNodes()` | `GET /api/visualization/key-nodes` |
| `dashboardApi.activeProjects(kw)` | `GET /api/projects?projectStatus=in-progress&keyword=` |
| `dashboardApi.warningRules()` | `GET /api/warning-rules` |

## 设计说明

视觉方向为「浅色蓝图纸风」：**浅色侧边栏 × 蓝调纸面 × 细线留白**，整体观感像一张现代工程蓝图。

- **色板**：蓝图墨蓝 `#16324F`（标题/主文字）、蓝图蓝 `#1D6FD1`（主强调/激活）、纸面 `#F4F6F9`（页面底色）、细发丝线 `#DBE3EE`；琥珀 `#D98C0B`、合格绿 `#1E8E5E`、警报红 `#C2402E` 仅作状态点缀。
- **侧边栏**：浅色 `#FBFCFE` + 蓝色激活轨道指示，轻量留白。
- **字体**：标题与大数字用 **Bahnschrift / DIN**（Windows 内置工程字体），编号/编码/数据用 **Consolas**（等宽），正文用 Segoe UI / 系统字体。全部为本地字体，**无外部请求、离线可用、渲染不阻塞**。
- **签名元素**：工作台总进度「结构柱进度仪」——浅色蓝图纸底 + 蓝色墨线 + 琥珀浇筑柱 + 勘测尺寸标注与十字准星。
- **细节**：主内容区极淡蓝图网格底纹、顶栏勘测刻度、工程台账式表格（表头等宽小字）、标签带状态圆点。
- **体验**：页面切换过渡、面板浮现、行悬停微交互，均尊重 `prefers-reduced-motion`；响应式适配桌面/平板/手机。
