-- =====================================================================
-- PIDMS · 08 项目预算改「一期平面预算」结构
-- 数据库：MySQL 8.x   引擎：InnoDB
--
-- 背景：一期项目预算从「一主多子」改为「单行平面」：
--   1) pm_project_budget 主表新增六类分项金额列（人工/材料/设备/费用/分包/其他），
--      分项未录入时存 NULL；budget_total 由保存逻辑按六类汇总。
--   2) pm_project_budget_item 明细表【保留表结构、一期业务不再读写】，预留二期
--      「多行明细录入」再启用。upgrade/07 仅用于旧模型补数，已被本脚本取代。
--
-- 执行：在 Navicat / IDEA Database / mysql 命令行对 pidms 库执行一次。
--       新库直接跑 02_schema.sql 已含新列，无需执行本脚本。
-- =====================================================================

USE `pidms`;

-- 1) pm_project_budget 增加六类分项列（可空，未录入存 NULL）
ALTER TABLE `pm_project_budget`
  ADD COLUMN `labor_budget`       DECIMAL(18,2) DEFAULT NULL COMMENT '人工预算（未录入存 NULL）' AFTER `budget_total`,
  ADD COLUMN `material_budget`    DECIMAL(18,2) DEFAULT NULL COMMENT '材料预算（未录入存 NULL）' AFTER `labor_budget`,
  ADD COLUMN `equipment_budget`   DECIMAL(18,2) DEFAULT NULL COMMENT '设备预算（未录入存 NULL）' AFTER `material_budget`,
  ADD COLUMN `expense_budget`     DECIMAL(18,2) DEFAULT NULL COMMENT '费用预算（未录入存 NULL）' AFTER `equipment_budget`,
  ADD COLUMN `subcontract_budget` DECIMAL(18,2) DEFAULT NULL COMMENT '分包预算（未录入存 NULL）' AFTER `expense_budget`,
  ADD COLUMN `other_budget`       DECIMAL(18,2) DEFAULT NULL COMMENT '其他预算（未录入存 NULL）' AFTER `subcontract_budget`;

-- 2) 从明细表回填历史数据：仅当该类别存在明细行时才回填，缺类别保持 NULL
UPDATE `pm_project_budget` b
   SET b.`labor_budget` = (SELECT SUM(i.`amount`) FROM `pm_project_budget_item` i
                            WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'labor')
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i
                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'labor');

UPDATE `pm_project_budget` b
   SET b.`material_budget` = (SELECT SUM(i.`amount`) FROM `pm_project_budget_item` i
                               WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'material')
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i
                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'material');

UPDATE `pm_project_budget` b
   SET b.`equipment_budget` = (SELECT SUM(i.`amount`) FROM `pm_project_budget_item` i
                                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'equipment')
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i
                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'equipment');

UPDATE `pm_project_budget` b
   SET b.`expense_budget` = (SELECT SUM(i.`amount`) FROM `pm_project_budget_item` i
                              WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'expense')
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i
                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'expense');

UPDATE `pm_project_budget` b
   SET b.`subcontract_budget` = (SELECT SUM(i.`amount`) FROM `pm_project_budget_item` i
                                  WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'subcontract')
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i
                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'subcontract');

UPDATE `pm_project_budget` b
   SET b.`other_budget` = (SELECT SUM(i.`amount`) FROM `pm_project_budget_item` i
                            WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'other')
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i
                WHERE i.`budget_id` = b.`id` AND i.`budget_category` = 'other');

-- 3) budget_total 与明细合计对齐（仅有明细行时覆盖，无明细行保持原值）
UPDATE `pm_project_budget` b
   SET b.`budget_total` = (SELECT COALESCE(SUM(i.`amount`), 0) FROM `pm_project_budget_item` i
                            WHERE i.`budget_id` = b.`id`)
 WHERE EXISTS (SELECT 1 FROM `pm_project_budget_item` i WHERE i.`budget_id` = b.`id`);

-- 4) 明细表注释改为「一期预留不读写，二期启用」（结构保留）
ALTER TABLE `pm_project_budget_item`
  COMMENT = '项目预算明细表（一期预留不读写，二期启用）';

-- 5) 校验结果
SELECT `id`, `budget_total`,
       `labor_budget`, `material_budget`, `equipment_budget`,
       `expense_budget`, `subcontract_budget`, `other_budget`
  FROM `pm_project_budget`;
