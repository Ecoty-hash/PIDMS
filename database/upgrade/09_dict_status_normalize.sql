-- =====================================================================
-- 09_dict_status_normalize.sql
-- 字典表状态码归一：旧数据 legacy 编码 active/inactive → 标准 enabled/disabled
--
-- 背景：项目状态/印章类型表早期使用 active(启用) / inactive(停用)，
--       现统一定义为 enabled启用 / disabled停用 / sealed封存。
--       后端 Service 已在保存时做兼容归一，这里把存量数据一次性归位。
-- 幂等：可重复执行。
-- =====================================================================

-- ---------- 项目状态：pm_project_status ----------
UPDATE `pm_project_status` SET `status` = 'enabled'  WHERE `status` = 'active';
UPDATE `pm_project_status` SET `status` = 'disabled' WHERE `status` = 'inactive';

-- ---------- 印章类型：pm_seal_type ----------
UPDATE `pm_seal_type` SET `status` = 'enabled'  WHERE `status` = 'active';
UPDATE `pm_seal_type` SET `status` = 'disabled' WHERE `status` = 'inactive';

-- ---------- 自检：应仅剩 enabled / disabled / sealed ----------
SELECT 'pm_project_status' AS tbl, `status`, COUNT(*) AS cnt FROM `pm_project_status` GROUP BY `status`
UNION ALL
SELECT 'pm_seal_type' AS tbl, `status`, COUNT(*) AS cnt FROM `pm_seal_type` GROUP BY `status`
ORDER BY tbl, `status`;
