-- =====================================================================
-- 10_project_status_backfill_code.sql
-- 存量「项目状态」字典行缺失 状态编码(status_code) 的补齐脚本
--
-- 背景：早期「项目状态」新增/编辑表单没有“状态编码”输入，历史新增的
--       字典行 status_code 可能为 NULL。而 项目(pm_project.project_status)
--       及“项目”模块的下拉按 状态编码(statusCode) 动态引用字典，
--       编码缺失会导致下拉取值无意义、新增行不在下拉中出现。
--       本脚本把已知标准状态按中文名回填编码，幂等可重复执行。
-- =====================================================================

-- ---------- 1. 按中文名回填标准编码（只处理缺失编码的行） ----------
UPDATE `pm_project_status`
   SET `status_code` = 'planning'
 WHERE (`status_code` IS NULL OR `status_code` = '')
   AND `status_name` = '规划中';

UPDATE `pm_project_status`
   SET `status_code` = 'in-progress'
 WHERE (`status_code` IS NULL OR `status_code` = '')
   AND `status_name` = '在建';

UPDATE `pm_project_status`
   SET `status_code` = 'completed'
 WHERE (`status_code` IS NULL OR `status_code` = '')
   AND `status_name` = '已完工';

UPDATE `pm_project_status`
   SET `status_code` = 'suspended'
 WHERE (`status_code` IS NULL OR `status_code` = '')
   AND `status_name` = '已暂停';

UPDATE `pm_project_status`
   SET `status_code` = 'terminated'
 WHERE (`status_code` IS NULL OR `status_code` = '')
   AND `status_name` = '已终止';

-- ---------- 2. 自检：仍缺编码的字典行（需在“项目状态”编辑中手工补编码） ----------
SELECT `id`, `status_name`, `status_code`, `status`
  FROM `pm_project_status`
 WHERE `status_code` IS NULL OR `status_code` = ''
 ORDER BY `id`;

-- ---------- 3. 自检：标准字典行应具备一一对应关系 ----------
SELECT `id`, `status_name`, `status_code`, `status`
  FROM `pm_project_status`
 WHERE `status_name` IN ('规划中','在建','已完工','已暂停','已终止')
 ORDER BY `id`;
