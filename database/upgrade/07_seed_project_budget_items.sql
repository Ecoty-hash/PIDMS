-- =====================================================================
-- PIDMS · 07 项目预算明细补数（示例数据）【已废弃】
-- ⚠ 已被 upgrade/08_budget_flatten.sql 取代：一期项目预算已改为「主表单行平面」，
--   pm_project_budget_item 不再读写。请直接执行 08，无需再执行本脚本。
--   本文件仅存档，供二期「多行明细录入」恢复时参考。
-- 用途（历史）：为现有库中 pm_project_budget 预算 id=2~5 补上四类明细
--       （labor人工 / material材料 / equipment设备 / expense费用），
--       使项目预算列表能显示出分类金额（前端列表只展示这四类）。
-- =====================================================================

USE `pidms`;

DELETE FROM `pm_project_budget_item` WHERE `budget_id` IN (2, 3, 4, 5);

INSERT INTO `pm_project_budget_item`
(`budget_id`, `budget_category`, `budget_type_id`, `amount`, `remark`) VALUES
  (2, 'labor',     NULL,  82000000.00, '人工费'),
  (2, 'material',  NULL, 110000000.00, '主要材料费'),
  (2, 'equipment', NULL,  42000000.00, '机械设备费'),
  (2, 'expense',   NULL,  39500000.00, '现场管理费'),
  (3, 'labor',     NULL,  26000000.00, '人工费'),
  (3, 'material',  NULL,  38000000.00, '主要材料费'),
  (3, 'equipment', NULL,  19000000.00, '机械设备费'),
  (3, 'expense',   NULL,  13500000.00, '现场管理费'),
  (4, 'labor',     NULL,  11800000.00, '人工费'),
  (4, 'material',  NULL,  16000000.00, '主要材料费'),
  (4, 'equipment', NULL,   9000000.00, '机械设备费'),
  (4, 'expense',   NULL,   6000000.00, '现场管理费'),
  (5, 'labor',     NULL, 330000000.00, '人工费'),
  (5, 'material',  NULL, 640000000.00, '主要材料费'),
  (5, 'equipment', NULL, 380000000.00, '机械设备费'),
  (5, 'expense',   NULL, 210000000.00, '现场管理费');
