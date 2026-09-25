-- =====================================================================
-- PIDMS · 14 进度节点支持子节点（多层）与「占总进度百分比」
-- 数据库：MySQL 8.x   引擎：InnoDB
--
-- 背景：进度节点原先只有一层（项目 → 节点）。实际施工中一个总节点下面还分
--       若干子节点（例如「车站土建施工」下设「围护结构 / 主体结构 / 附属结构」），
--       而且各子节点对项目总进度的贡献并不相同——用简单的算术平均会让一个
--       小节点和整个主体结构等价，算出来的项目进度失真。
--
-- 新增两列：
--   parent_id  自关联父节点，NULL = 顶层节点。层级不设上限（支持多层嵌套）。
--              不建外键：删除父节点时由应用层一并删除整棵子树，若加外键反而
--              需要先手动清子节点，容易把「删不掉」误当成权限问题。
--   weight     该节点占项目总进度的百分比（0-100），只有叶子节点需要手填。
--              父节点的权重由子节点汇总得出（读取时计算，不落库）。
--              未填（NULL）时按「同组未填节点平分剩余份额」处理，整组都未填
--              即为均分——这样存量数据不改也是原来的平均口径。
--
-- 计算口径（与后端 ProjectProgressCalculator 一致）：
--   叶子节点完成度 = 手填的 completion_percent
--   父节点完成度   = Σ(子节点权重 × 子节点完成度) ÷ Σ子节点权重
--   项目实际进度   = Σ(顶层节点权重 × 顶层节点完成度) ÷ Σ顶层节点权重
--   期望值（理论进度）= 按计划工期线性推进：(今天 − 计划开始) ÷ (计划结束 − 计划开始)，封顶 100%
--
-- 存量数据：parent_id 全部为 NULL（都是顶层节点），weight 全部为 NULL
--           （按均分处理），因此本次升级不改变任何现有项目的进度数字。
--
-- 执行：在 Navicat / IDEA Database / mysql 命令行对 pidms 库执行一次。
--       新库直接跑 02_schema.sql 已含这两列，无需执行本脚本。
-- =====================================================================

USE `pidms`;

-- 1) 父节点（自关联，支持多层）
ALTER TABLE `cm_progress`
  ADD COLUMN `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父节点ID → cm_progress.id，NULL为顶层节点' AFTER `project_id`;

-- 2) 占总进度百分比（仅叶子节点手填；父节点由子节点汇总）
ALTER TABLE `cm_progress`
  ADD COLUMN `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '占总进度百分比(0-100)，仅叶子节点填写' AFTER `completion_percent`;

-- 3) 父节点查询索引（自关联不做外键，见文件头说明）
ALTER TABLE `cm_progress`
  ADD INDEX `idx_progress_parent` (`parent_id`);

-- 自检：列结构与存量情况
SHOW COLUMNS FROM `cm_progress` LIKE 'parent_id';
SHOW COLUMNS FROM `cm_progress` LIKE 'weight';

SELECT COUNT(*) AS 顶层节点数,
       SUM(CASE WHEN `parent_id` IS NOT NULL THEN 1 ELSE 0 END) AS 子节点数,
       SUM(CASE WHEN `weight` IS NOT NULL THEN 1 ELSE 0 END) AS 已填权重节点数
FROM `cm_progress`;
