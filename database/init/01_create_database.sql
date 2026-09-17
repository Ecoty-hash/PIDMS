-- =====================================================================
-- PIDMS 企业管理系统 - 建库脚本
-- 数据库：MySQL 8.x    引擎：InnoDB    字符集：utf8mb4
-- 适用范围：核心域（系统管理 / 项目管理 / 施工管理）
-- 说明：基础管理域（客户/供应商/内部单位/工种/预算类型/公司文档/考勤申请等）
--       由后续 database/upgrade/ 增量脚本补齐，见《数据库设计文档.md》
-- =====================================================================

-- 删除已存在的库（生产环境请勿执行，仅用于初始化演示环境）
DROP DATABASE IF EXISTS `pidms`;

-- 创建数据库（MySQL 8 的 CREATE DATABASE 不支持 COMMENT 子句）
CREATE DATABASE `pidms`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `pidms`;
