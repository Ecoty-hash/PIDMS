# PIDMS 工程文档管理系统
> 基于 SpringBoot3 + Vue3 开发的工程文档与项目进度管理后台系统，个人全栈实战项目

## 📚 项目介绍
PIDMS（Project Information Document Management System）工程文档管理系统，面向工程项目场景，实现项目基础信息维护、施工进度管理、文档归档管理等核心业务。
后端采用分层架构设计，严格遵循 Controller / Service / Mapper / Entity / VO/DTO 分层，使用 MapStruct 做对象转换，集成 Redis 做缓存，全局异常捕获、请求参数校验，适合中小型工程项目的资料线上管理。

## 使用方式
1. Github仓库主页，点击README.md右侧铅笔图标（编辑）
2. 全选删除原有内容，粘贴上面整段文本
3. 拉到最下面，填写提交信息，点Commit changes保存

## 🛠 技术栈
### 后端
- 核心框架：SpringBoot 3
- ORM：MyBatis-Plus
- 数据库：MySQL 8.0
- 缓存：Redis
- 工具：Hutool、MapStruct、Lombok
- 构建工具：Maven
- JDK：JDK 21

### 前端
- Vue3 + Vite + Element Plus

## ✨ v1.0.0 已实现功能
- 项目模块：新增、编辑、删除、条件分页查询
- 进度模块：进度维护、条件分页查询、详情查询
- 统一返回结果封装，全局异常处理器
- 请求参数校验，VO/DTO分层设计，MapStruct实体转换
- 分页封装、多条件模糊查询、关联表查询

## 📦 环境准备
1. JDK 21
2. MySQL 8.0
4. Maven 3.8+

## 🚀 后端启动步骤
```bash
# 1. 克隆仓库
git clone https://github.com/Ecoty-hash/PIDMS.git
cd PIDMS

# 2. 修改application.yml数据库、Redis连接配置

# 3. Maven打包/运行
mvn clean install
# 直接启动主类，或 java -jar target/pidms-backend.jar


pidms-backend
├── controller      # 接口控制器层
├── service         # 业务逻辑层
│   └── impl        # service实现类
├── mapper          # MyBatis Mapper接口
├── entity          # 数据库实体
├── dto             # 请求入参DTO
├── vo              # 返回视图VO
├── convert         # MapStruct对象转换器
├── common          # 公共类：统一返回、全局异常、常量
└── config          # 配置类
