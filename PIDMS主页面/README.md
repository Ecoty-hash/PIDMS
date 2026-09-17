# PIDMS · 工作台主页面

基于《DLS原型图》与《PIDMS接口文档》生成的企业管理系统主页面(登录后工作台)。

## 快速开始

直接双击 `index.html` 即可在浏览器中打开,无需任何构建工具或依赖。页面使用本地模拟数据展示完整效果。

## 页面结构

```
PIDMS主页面/
└── index.html      # 自包含单页:样式 + 结构 + 脚本
```

页面为自包含单文件,但内部按职责清晰分层,便于后续拆分为工程化代码:

| 区块 | 位置 | 说明 |
|---|---|---|
| 设计令牌 | `<style>:root` | 色板 / 字体 / 间距等 CSS 变量 |
| 侧边栏菜单 | JS `MENU` 数组 | 数据驱动,与原型导航一致,接入路由只改 `href` |
| 配置 | JS `CONFIG` | `useMock` 开关、`baseUrl` |
| 数据层 | JS `Api` 对象 + `mock` | 每个方法对应接口文档中的一个接口 |
| 渲染层 | JS `renderXxx()` | 负责把数据渲染到各面板 |
| SVG 图表 | JS `columnGauge / donut / lineChart` | 无第三方依赖,纯内联 SVG |

## 接入真实后端

1. 打开 `index.html`,在 `CONFIG` 中将 `useMock` 改为 `false`:

   ```js
   const CONFIG = { useMock: false, baseUrl: "https://{host}/api" };
   ```

2. 数据层已按接口文档写好请求路径与参数,`Api` 中各方法即对应的后端接口:

   | Api 方法 | 接口文档对应 |
   |---|---|
   | `Api.getDashboard()` | `GET /api/visualization/dashboard` |
   | `Api.getProgressTrend(g)` | `GET /api/visualization/progress-trend?granularity=` |
   | `Api.getInspectionStats()` | `GET /api/visualization/inspection-stats` |
   | `Api.getKeyNodes()` | `GET /api/visualization/key-nodes` |
   | `Api.getActiveProjects(kw)` | `GET /api/projects?projectStatus=in-progress&keyword=` |
   | `Api.getWarningRules()` | `GET /api/warning-rules` |

3. 认证:在 `Api.http()` 中为请求头补充 `Authorization: Bearer {token}`(登录接口获取)。

> 当前 `mock` 返回的字段结构与接口文档中的 `data` 完全同构,后端就绪后替换 `mock` 分支即可,页面渲染层无需改动。

## 与原型图的对应关系

- 侧边栏菜单分组与条目、面包屑、用户区与 DLS原型图 `index.html` 导航一致;
- 总进度、进行中项目、质量/安全合格率、状态分布、趋势图、关键节点来自原型 `visualization.html`(可视化管理);
- 进行中项目列表与搜索对应原型 `project.html` 的列表与搜索;
- 预警规则列表对应原型 `warning-rule.html` 的规则卡片。

## 设计说明

视觉方向为「工程勘测仪表台」:蓝图墨蓝 + 草图纸冷白 + 安全琥珀色;大数字使用工程字体(Bahnschrift / DIN),数据与小标使用等宽字体;签名元素为总进度「结构柱进度仪」(浇筑进度柱 + 勘测尺寸标注)。响应式适配桌面 / 平板 / 手机,支持键盘焦点可见与 `prefers-reduced-motion`。
