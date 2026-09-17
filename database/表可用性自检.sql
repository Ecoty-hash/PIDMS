-- =====================================================================
-- PIDMS · 数据库表可用性自检脚本
-- 适用：本地 MySQL 8.x（root@localhost / schema=pidms）
-- 用法：在 Navicat / IDEA Database / mysql 命令行中直接执行本脚本
-- =====================================================================

USE `pidms`;

-- 1) 连接与库可用性 ------------------------------------------------
SELECT '连接正常，当前库 = pidms' AS 检查项,
       @@version AS 版本,
       @@character_set_database AS 字符集,
       @@collation_database AS 排序规则;

-- 2) 表数量与清单 ---------------------------------------------------
SELECT COUNT(*) AS 业务表总数
FROM information_schema.tables
WHERE table_schema = 'pidms' AND table_type = 'BASE TABLE';

SELECT table_name AS 表名, table_rows AS 估算行数, create_time AS 建表时间
FROM information_schema.tables
WHERE table_schema = 'pidms' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 3) 关键表 SELECT 冒烟（能查到即说明表可读）----------------------
SELECT COUNT(*) AS pm_project_rows FROM pm_project;
SELECT COUNT(*) AS pm_seal_application_rows FROM pm_seal_application;
SELECT COUNT(*) AS sys_user_rows FROM sys_user;
SELECT COUNT(*) AS sys_org_rows FROM sys_org;
SELECT COUNT(*) AS sys_dict_data_rows FROM sys_dict_data;
SELECT COUNT(*) AS cm_progress_rows FROM cm_progress;
SELECT COUNT(*) AS cm_construction_log_rows FROM cm_construction_log;
SELECT COUNT(*) AS cm_quality_inspection_rows FROM cm_quality_inspection;
SELECT COUNT(*) AS cm_safety_inspection_rows FROM cm_safety_inspection;
SELECT COUNT(*) AS cm_warning_rule_rows FROM cm_warning_rule;

-- 4) 外键引用完整性抽查（结果为 0 表示无孤儿数据）------------------
SELECT
  (SELECT COUNT(*) FROM cm_progress p LEFT JOIN pm_project pj ON p.project_id = pj.id WHERE pj.id IS NULL) AS orphan_cm_progress,
  (SELECT COUNT(*) FROM cm_construction_log l LEFT JOIN pm_project pj ON l.project_id = pj.id WHERE pj.id IS NULL) AS orphan_cm_construction_log,
  (SELECT COUNT(*) FROM cm_quality_inspection i LEFT JOIN pm_project pj ON i.project_id = pj.id WHERE pj.id IS NULL) AS orphan_cm_quality_inspection,
  (SELECT COUNT(*) FROM cm_safety_inspection i LEFT JOIN pm_project pj ON i.project_id = pj.id WHERE pj.id IS NULL) AS orphan_cm_safety_inspection,
  (SELECT COUNT(*) FROM cm_quality_rectification r LEFT JOIN pm_project pj ON r.project_id = pj.id WHERE pj.id IS NULL) AS orphan_cm_quality_rectification,
  (SELECT COUNT(*) FROM cm_safety_rectification r LEFT JOIN pm_project pj ON r.project_id = pj.id WHERE pj.id IS NULL) AS orphan_cm_safety_rectification,
  (SELECT COUNT(*) FROM pm_seal_application s LEFT JOIN pm_project pj ON s.project_id = pj.id WHERE pj.id IS NULL) AS orphan_pm_seal_application,
  (SELECT COUNT(*) FROM sys_user u LEFT JOIN sys_org o ON u.org_id = o.id WHERE o.id IS NULL) AS orphan_sys_user;

-- 5) 字典表读写 -----------------------------------------------------
SELECT dt.dict_type, dt.dict_name, COUNT(dd.id) AS 字典项数
FROM sys_dict_type dt LEFT JOIN sys_dict_data dd ON dd.dict_type = dt.dict_type
GROUP BY dt.id, dt.dict_type, dt.dict_name
ORDER BY dt.id;

-- 6) 写操作冒烟（事务内，自动回滚，不会留下数据）-------------------
START TRANSACTION;
INSERT INTO sys_operation_log (user_name, module, action, detail, create_time)
VALUES ('selftest', 'db-check', 'insert', '可用性自检写入', NOW());
SELECT ROW_COUNT() AS 写入行数;
ROLLBACK;
SELECT '事务写入已回滚，未残留数据' AS 检查项;
