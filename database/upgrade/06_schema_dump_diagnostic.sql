-- =====================================================================
-- PIDMS · 数据库结构快照（只读诊断，不改任何数据）
-- 用途：拿到 pidms 库每张表的真实列定义与外键，用于精确编写迁移脚本。
-- 用法：在 Navicat / IDEA Database / mysql 命令行对 pidms 库执行。
--       把下面 ①② 两个查询的【结果网格】原样贴回给 Claude 即可。
-- =====================================================================

USE `pidms`;

-- ① 每张表的列定义（一行一张表，含类型/是否非空/主键或唯一键标记）
SELECT
  table_name AS 表,
  GROUP_CONCAT(
    CONCAT(column_name, ' ', column_type,
           IF(is_nullable = 'NO', ' NOT NULL', ''),
           IF(column_key = 'PRI', ' [PK]',
              IF(column_key = 'UNI', ' [UK]', ''))
    )
    ORDER BY ordinal_position SEPARATOR ' | '
  ) AS 列定义
FROM information_schema.columns
WHERE table_schema = 'pidms'
GROUP BY table_name
ORDER BY table_name;

-- ② 全部外键约束（子表 -> 父表）
SELECT
  table_name      AS 子表,
  constraint_name AS 约束名,
  column_name     AS 列,
  referenced_table_name AS 父表,
  referenced_column_name AS 父表列
FROM information_schema.key_column_usage
WHERE table_schema = 'pidms' AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name;
