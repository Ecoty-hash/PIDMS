-- =====================================================================
-- PIDMS 企业管理系统 - 种子数据（字典 + 初始账号 + 基础样例）
-- 依赖：01_create_database.sql、02_schema.sql 已执行
-- 说明：枚举字段在业务表中存 dict_code，展示时关联 sys_dict_data 取 label
-- =====================================================================

USE `pidms`;

-- ---------------------------------------------------------------------
-- 一、字典类型
-- ---------------------------------------------------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`) VALUES
  (1,  'status',               '通用状态',       'enabled', 'enabled启用 / disabled停用 / sealed封存'),
  (2,  'project_status',       '项目状态',       'enabled', '项目主表 project_status 字段'),
  (3,  'archive_status',       '归档状态',       'enabled', 'archived已归档 / unarchived未归档'),
  (4,  'participation_status', '参与状态',       'enabled', 'participated参与 / not-participated未参与'),
  (5,  'approval_status',      '审批状态',       'enabled', 'pending待审批 / approved已审批 / rejected已拒绝'),
  (6,  'inspection_result',    '检查结果',       'enabled', 'pending待检查 / qualified合格 / unqualified不合格'),
  (7,  'inspection_type',      '检查类型',       'enabled', 'quality质量检查 / safety安全检查'),
  (8,  'rectification_status', '整改状态',       'enabled', 'pending待整改 / processing整改中 / completed已整改'),
  (9,  'weather',              '天气',           'enabled', '施工日志天气'),
  (10, 'gender',               '性别',           'enabled', '男 / 女'),
  (11, 'seal_method',          '用印方式',       'enabled', '原件盖章 / 复印件盖章'),
  (12, 'warning_type',         '预警类型',       'enabled', '进度/质量/安全预警'),
  (13, 'doc_category',         '文档分类',       'enabled', '施工图纸 / 技术资料 / 验收资料');

-- ---------------------------------------------------------------------
-- 二、字典数据
-- ---------------------------------------------------------------------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_code`, `dict_label`, `sort_order`, `status`) VALUES
  -- 1. 通用状态
  ('status',            'enabled',            '启用',     1, 'enabled'),
  ('status',            'disabled',           '停用',     2, 'enabled'),
  ('status',            'sealed',             '封存',     3, 'enabled'),
  -- 2. 项目状态
  ('project_status',    'planning',           '规划中',   1, 'enabled'),
  ('project_status',    'in-progress',        '在建',     2, 'enabled'),
  ('project_status',    'completed',          '已完工',   3, 'enabled'),
  ('project_status',    'suspended',          '已暂停',   4, 'enabled'),
  -- 3. 归档状态
  ('archive_status',    'archived',           '已归档',   1, 'enabled'),
  ('archive_status',    'unarchived',         '未归档',   2, 'enabled'),
  -- 4. 参与状态
  ('participation_status', 'participated',    '参与',     1, 'enabled'),
  ('participation_status', 'not-participated','未参与',   2, 'enabled'),
  -- 5. 审批状态
  ('approval_status',   'pending',            '待审批',   1, 'enabled'),
  ('approval_status',   'approved',           '已审批',   2, 'enabled'),
  ('approval_status',   'rejected',           '已拒绝',   3, 'enabled'),
  -- 6. 检查结果
  ('inspection_result', 'pending',            '待检查',   1, 'enabled'),
  ('inspection_result', 'qualified',          '合格',     2, 'enabled'),
  ('inspection_result', 'unqualified',        '不合格',   3, 'enabled'),
  -- 7. 检查类型
  ('inspection_type',   'quality',            '质量检查', 1, 'enabled'),
  ('inspection_type',   'safety',             '安全检查', 2, 'enabled'),
  -- 8. 整改状态
  ('rectification_status', 'pending',         '待整改',   1, 'enabled'),
  ('rectification_status', 'processing',      '整改中',   2, 'enabled'),
  ('rectification_status', 'completed',       '已整改',   3, 'enabled'),
  -- 9. 天气
  ('weather',           'sunny',              '晴',       1, 'enabled'),
  ('weather',           'cloudy',             '多云',     2, 'enabled'),
  ('weather',           'rainy',              '雨',       3, 'enabled'),
  ('weather',           'snowy',              '雪',       4, 'enabled'),
  -- 10. 性别
  ('gender',            '男',                 '男',       1, 'enabled'),
  ('gender',            '女',                 '女',       2, 'enabled'),
  -- 11. 用印方式
  ('seal_method',       '原件盖章',           '原件盖章', 1, 'enabled'),
  ('seal_method',       '复印件盖章',         '复印件盖章',2, 'enabled'),
  -- 12. 预警类型
  ('warning_type',      '进度停滞预警',       '进度停滞预警', 1, 'enabled'),
  ('warning_type',      '质量问题预警',       '质量问题预警', 2, 'enabled'),
  ('warning_type',      '安全预警',           '安全预警',  3, 'enabled'),
  -- 13. 文档分类
  ('doc_category',      '施工图纸',           '施工图纸', 1, 'enabled'),
  ('doc_category',      '技术资料',           '技术资料', 2, 'enabled'),
  ('doc_category',      '验收资料',           '验收资料', 3, 'enabled');

-- ---------------------------------------------------------------------
-- 三、机构（参照 mock：总公司 → 项目三部 → 蒙特雷项目组 / 技术部 / 海外事业部）
-- ---------------------------------------------------------------------
INSERT INTO `sys_org` (`id`, `parent_id`, `org_code`, `org_name`, `sort_order`, `description`, `status`) VALUES
  (1, NULL, 'CRRC-PZ',   '江南轨道装备制造有限公司', 1, '总公司', 'enabled'),
  (2, 1,    'XM-003',    '项目三部',                  1, '',       'enabled'),
  (3, 2,    'XM-003-01', '蒙特雷项目组',              1, '',       'enabled'),
  (4, 1,    'JS-001',    '技术部',                    2, '',       'enabled'),
  (5, 1,    'HW-001',    '海外事业部',                3, '',       'enabled');

-- ---------------------------------------------------------------------
-- 四、角色
-- ---------------------------------------------------------------------
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `role_description`, `status`) VALUES
  (1, 'ADMIN',    '系统管理员', '拥有全部权限',               'enabled'),
  (2, 'PM',       '项目经理',   '项目级管理权限',             'enabled'),
  (3, 'ENGINEER', '工程师',     '施工/进度/日志维护',         'enabled'),
  (4, 'STAFF',    '普通员工',   '查看与个人申请',             'enabled');

-- ---------------------------------------------------------------------
-- 五、用户（人员管理）
-- ---------------------------------------------------------------------
INSERT INTO `sys_user` (`id`, `name`, `gender`, `employee_no`, `phone`, `org_id`, `status`, `remark`) VALUES
  (1, '系统管理员', '男', 'P001', '13800138000', 1, 'enabled', '初始管理员'),
  (2, '吴十',       '男', 'P002', '13800138001', 2, 'enabled', '项目经理'),
  (3, '张文琦',     '女', 'P003', '13800138002', 4, 'enabled', '工程师'),
  (4, '王五',       '男', 'P004', '13800138003', 5, 'enabled', '项目经理');

-- ---------------------------------------------------------------------
-- 六、用户-角色
-- ---------------------------------------------------------------------
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
  (1, 1),
  (2, 2),
  (3, 3),
  (4, 2);

-- ---------------------------------------------------------------------
-- 七、角色权限（权限标识按 模块:操作 约定）
-- ---------------------------------------------------------------------
INSERT INTO `sys_role_permission` (`role_id`, `permission_code`, `permission_name`) VALUES
  -- 系统管理员：全量
  (1, 'project:all',            '项目-全部'),
  (1, 'progress:all',           '进度-全部'),
  (1, 'quality:all',            '质量-全部'),
  (1, 'safety:all',             '安全-全部'),
  (1, 'seal:all',               '用印-全部'),
  (1, 'construction-log:all',   '施工日志-全部'),
  (1, 'warning-rule:all',       '预警规则-全部'),
  (1, 'system:all',             '系统-全部'),
  -- 项目经理
  (2, 'project:view',           '项目-查看'),
  (2, 'project:edit',           '项目-编辑'),
  (2, 'progress:all',           '进度-全部'),
  (2, 'quality:all',            '质量-全部'),
  (2, 'safety:all',             '安全-全部'),
  (2, 'seal:all',               '用印-全部'),
  (2, 'construction-log:all',   '施工日志-全部'),
  (2, 'warning-rule:all',       '预警规则-全部'),
  -- 工程师
  (3, 'progress:all',           '进度-全部'),
  (3, 'quality:all',            '质量-全部'),
  (3, 'safety:all',             '安全-全部'),
  (3, 'construction-log:all',   '施工日志-全部'),
  -- 普通员工
  (4, 'progress:view',          '进度-查看'),
  (4, 'construction-log:view',  '施工日志-查看'),
  (4, 'seal:apply',             '用印-申请'),
  (4, 'leave:apply',            '请假-申请'),
  (4, 'trip:apply',             '出差-申请'),
  (4, 'makeup:apply',           '补卡-申请');

-- ---------------------------------------------------------------------
-- 八、印章类型（用印申请可选类型）
-- ---------------------------------------------------------------------
INSERT INTO `pm_seal_type` (`id`, `type_name`, `status`) VALUES
  (1, '公章',       'enabled'),
  (2, '合同章',     'enabled'),
  (3, '财务章',     'enabled'),
  (4, '项目部印章', 'enabled'),
  (5, '分公司印章', 'enabled'),
  (6, '总公司印章', 'enabled');

-- ---------------------------------------------------------------------
-- 九、项目状态定义（项目状态模块样例）
-- ---------------------------------------------------------------------
INSERT INTO `pm_project_status` (`id`, `status_name`, `status_code`, `status`) VALUES
  (1, '规划中', 'planning',    'enabled'),
  (2, '在建',   'in-progress', 'enabled'),
  (3, '已完工', 'completed',   'enabled'),
  (4, '已暂停', 'suspended',   'enabled');

-- =====================================================================
-- 种子数据结束
-- =====================================================================
