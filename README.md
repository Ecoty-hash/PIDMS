# PIDMS 企业管理系统

面向工程施工企业的项目管理系统。覆盖**项目管理、施工管理、基础管理、系统管理**四大域共 28 个业务模块，前端采用「配置驱动的通用 CRUD」架构 —— 各模块共用一套页面组件，模块配置即页面。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3.4 + Vite 5 + Vue Router 4（无 UI 框架，自建组件库） |
| 后端 | Java 21 + Spring Boot 3.1 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.x（InnoDB / utf8mb4） |
| 其他 | SpringDoc OpenAPI 3、JJWT、MapStruct、Hutool、Lombok、Aliyun OSS SDK |

## 目录结构

```
PIDMS/
├── PIDMS前端/           Vue3 + Vite 前端工程
│   ├── src/api/         接口封装（request.js）与本地 mock 数据
│   ├── src/components/  通用组件：DataTable / ModalForm / DetailPanel / SearchPanel ...
│   ├── src/config/      modules.js —— 28 个模块的配置（由原型图自动生成）
│   ├── src/layout/      主框架：侧边栏 / 顶栏
│   └── src/views/       页面：Login / Dashboard / ModulePage
├── PIDMS后端/           Spring Boot 后端工程（Maven）
│   └── pidms-backend/
│       └── src/main/java/com/pidms/pidmsbackend/
│           ├── controller/  29 个 REST 控制器
│           ├── service/     业务层
│           ├── mapper/      MyBatis-Plus Mapper
│           ├── entity/      实体与查询参数
│           ├── dto/ vo/     数据传输与视图对象
│           └── convert/     MapStruct 转换器
├── PIDMS主页面/         登录后工作台单页（自包含 HTML，双击即可打开）
├── PIDMS原型图/         DLS 原型图（设计基准）
├── database/            数据库脚本与工具
│   ├── init/            建库 + 建表 + 初始数据
│   ├── upgrade/         增量演进脚本
│   ├── tools/           文档生成与一致性校验脚本
│   └── 数据库设计文档.md
└── PIDMS接口文档.docx   接口文档（与前端 mock 同构）
```

## 快速开始

### 1. 初始化数据库

需要 MySQL 8.x。按顺序执行：

```bash
mysql -u root -p < database/init/01_create_database.sql   # 建库（会 DROP 同名库）
mysql -u root -p < database/init/02_schema.sql            # 建表
mysql -u root -p < database/init/03_seed.sql              # 字典等基础数据
mysql -u root -p < database/init/04_test_data.sql         # 测试数据（可选）
```

> `01_create_database.sql` 含 `DROP DATABASE IF EXISTS pidms`，仅用于初始化演示环境，**请勿在生产库执行**。

若要更新一个已有的旧库，改为按序号执行 `database/upgrade/` 下的脚本。注意 `04_align_database_to_design.sql` 会重建 `cm_` 施工域的表并清空其数据。

### 2. 启动后端

```bash
cd PIDMS后端/pidms-backend
mvn spring-boot:run          # 默认端口 8080
```

数据库连接改配置文件：把 `src/main/resources/application.yml.example` 复制为同目录下的 `application.yml` 后填写，或用环境变量注入 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`。

> `application.yml` 已列入 `.gitignore`，不会提交到版本库。

接口文档（Swagger UI）启动后访问 `http://localhost:8080/swagger-ui/index.html`。

### 3. 启动前端

```bash
cd PIDMS前端
npm install
npm run dev                  # → http://localhost:5173
```

开发服务器会把 `/api` 代理到 `http://localhost:8080`，联调时后端需同时运行。

> 本项目是 ES Module 工程，**不能直接双击 `index.html`**（`file://` 协议下模块无法加载，页面会空白），必须通过开发服务器或静态服务器访问。

初始账号为 `admin`，密码 `123456`（由 `database/upgrade/05_add_login_columns.sql` 写入）。后端对 `/api/**` 做了登录拦截，除登录接口外都需要先登录。

## 业务模块

| 域 | 模块 |
|---|---|
| 项目管理（6） | 项目、项目预算、项目文档、项目状态、用印申请、印章类型 |
| 施工管理（9） | 进度管理、质量检查、质量检查项、质量整改单、安全检查、安全检查项、安全整改单、施工日志、预警规则 |
| 基础管理（10） | 基础资料、客户、供应商、内部单位、工种类型、预算类型、公司文档、出差申请、请假申请、补卡申请 |
| 系统管理（3） | 人员管理、机构管理、角色管理 |

## 配套文档

| 文档 | 位置 | 说明 |
|---|---|---|
| 接口文档 | `PIDMS接口文档.docx` | 全量接口定义，前端 mock 与之同构 |
| 数据库设计文档 | `database/数据库设计文档.md` / `.docx` | 模块 ↔ 表映射、表结构明细、字典枚举 |
| ER 关系图 | `database/ER关系图.png` | 全库实体关系 |
| DLS 原型图 | `PIDMS原型图/DLS原型图/` | 页面与字段的设计基准 |
| 前端说明 | `PIDMS前端/README.md` | 前端架构与组件说明 |
| 主页面说明 | `PIDMS主页面/README.md` | 工作台单页说明 |

## 一致性校验

`database/tools/` 下提供若干校验脚本，用于检查数据库、后端、前端配置与接口文档是否对齐：

```bash
python database/tools/verify_consistency.py   # 库表 / 后端 / 前端 modules.js 三方一致性
python database/tools/verify_doc.py           # 接口文档与实现的一致性
node   database/tools/mock_selftest.js        # 前端 mock 自检
```

## 约定

- `PIDMS前端/src/config/modules.js` 由脚本依据 DLS 原型图生成，**请勿手工修改**；调整字段或枚举应先改原型再重新生成。
- 页面需求与字段以 DLS 原型图为准，数据层与接口文档保持对齐。
