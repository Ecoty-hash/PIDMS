-- =====================================================================
-- PIDMS · 13 进度节点增加「节点负责人」
-- 数据库：MySQL 8.x   引擎：InnoDB
--
-- 背景：项目进度详情页需要按节点指定负责人——同一个项目下的多个进度节点
--       往往分属不同人（勘察/设计/施工/验收），而 cm_progress 原先只通过
--       project_id 关联到项目，节点级责任人无处存放，页面上只能退而展示
--       pm_project.project_leader，无法区分节点。
--
-- 取值：存真实姓名（与 project_leader / project_members 一致）；
--       候选来源为在职内部人员（sys_user.status = 'enabled'）全量名单，
--       该项目的负责人 / 成员排在名单最前，便于优先选中。
--       可为空（不强制指定）。
--       存量节点保持 NULL，页面显示「未指定」，由业务自行补齐。
--       本脚本不做数据回填，避免把项目负责人静默当成节点负责人。
--
-- 执行：在 Navicat / IDEA Database / mysql 命令行对 pidms 库执行一次。
--       新库直接跑 02_schema.sql 已含该列，无需执行本脚本。
-- =====================================================================

USE `pidms`;

-- cm_progress 增加节点负责人列（可空）
ALTER TABLE `cm_progress`
  ADD COLUMN `responsible_person` VARCHAR(50) DEFAULT NULL COMMENT '节点负责人（真实姓名）' AFTER `progress_status`;

-- 自检：列出列结构与存量节点负责人填充情况
SHOW COLUMNS FROM `cm_progress` LIKE 'responsible_person';

SELECT COUNT(*) AS total,
       SUM(CASE WHEN `responsible_person` IS NULL OR `responsible_person` = '' THEN 1 ELSE 0 END) AS unassigned
FROM `cm_progress`;
