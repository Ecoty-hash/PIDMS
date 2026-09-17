-- =====================================================================
-- PIDMS · 12 基础管理域种子数据
-- 依赖：upgrade/11_basic_management.sql（建表）
--       init/03_seed.sql（sys_org id 1~5、sys_role id 1~4）
--       init/04_test_data.sql + init/05_test_data_all.sql（pm_project id 1~18）
--
-- 说明：
--   1) 本脚本为「可选」脚本，仅用于让基础管理域 10 个页面开箱有数据可看；
--   2) 全部使用 INSERT IGNORE + 显式主键，可重复执行，不会覆盖你改过的行；
--   3) 枚举值统一为英文 code（方向A），与后端实体/前端 modules.js 一致；
--   4) 示例内容参照前端原 mock 数据，便于对照。
-- =====================================================================

USE `pidms`;

-- ---------------------------------------------------------------------
-- 0. 字典登记：新增基础管理域用到的字典类型与字典项
--    遵循 02_schema.sql「枚举/字典字段统一存 code 值，中文标签见 sys_dict_data」。
--    id 从 14 起（03_seed.sql 已用 1~13）。INSERT IGNORE 保证幂等。
--    注：审批状态沿用既有 approval_status 字典（code 已是 pending/approved/rejected）；
--        通用状态沿用既有 status 字典（enabled/disabled/sealed）；gender 字典的
--        code 已在 upgrade/11_basic_management.sql 中由中文归一为 male/female。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`) VALUES
  (14, 'info_category',        '基础资料分类',  'enabled', 'material材料分类 / equipment设备分类 / worktype工种分类'),
  (15, 'basic_info_status',    '基础资料状态',  'enabled', 'active在用 / archived已归档'),
  (16, 'customer_type',        '客户类型',      'enabled', 'government政府单位 / enterprise企业客户 / state-owned国企客户'),
  (17, 'supplier_type',        '供应商类型',    'enabled', 'material材料供应商 / equipment设备供应商 / labor劳务分包'),
  (18, 'unit_type',            '内部单位类型',  'enabled', 'branch分公司 / project项目部 / department部门'),
  (19, 'budget_type',          '预算类型',      'enabled', 'labor人工费 / material材料费 / equipment设备费 / expense费用 / subcontract分包费 / other其他费用'),
  (20, 'budget_type_status',   '预算类型状态',  'enabled', 'enabled启用 / sealed封存'),
  (21, 'company_doc_category', '公司文档分类',  'enabled', 'regulation公司制度 / specification技术规范 / management管理文件'),
  (22, 'leave_type',           '请假类型',      'enabled', 'annual年假 / personal事假 / sick病假 / marriage婚假 / maternity产假 / compensatory调休'),
  (23, 'card_miss_type',       '缺卡类型',      'enabled', 'on-duty上班 / off-duty下班 / whole-day全天');

INSERT IGNORE INTO `sys_dict_data` (`dict_type`, `dict_code`, `dict_label`, `sort_order`, `status`) VALUES
  -- 14. 基础资料分类
  ('info_category',        'material',        '材料分类',   1, 'enabled'),
  ('info_category',        'equipment',       '设备分类',   2, 'enabled'),
  ('info_category',        'worktype',        '工种分类',   3, 'enabled'),
  -- 15. 基础资料状态
  ('basic_info_status',    'active',          '在用',       1, 'enabled'),
  ('basic_info_status',    'archived',        '已归档',     2, 'enabled'),
  -- 16. 客户类型
  ('customer_type',        'government',      '政府单位',   1, 'enabled'),
  ('customer_type',        'enterprise',      '企业客户',   2, 'enabled'),
  ('customer_type',        'state-owned',     '国企客户',   3, 'enabled'),
  -- 17. 供应商类型
  ('supplier_type',        'material',        '材料供应商', 1, 'enabled'),
  ('supplier_type',        'equipment',       '设备供应商', 2, 'enabled'),
  ('supplier_type',        'labor',           '劳务分包',   3, 'enabled'),
  -- 18. 内部单位类型
  ('unit_type',            'branch',          '分公司',     1, 'enabled'),
  ('unit_type',            'project',         '项目部',     2, 'enabled'),
  ('unit_type',            'department',      '部门',       3, 'enabled'),
  -- 19. 预算类型
  ('budget_type',          'labor',           '人工费',     1, 'enabled'),
  ('budget_type',          'material',        '材料费',     2, 'enabled'),
  ('budget_type',          'equipment',       '设备费',     3, 'enabled'),
  ('budget_type',          'expense',         '费用',       4, 'enabled'),
  ('budget_type',          'subcontract',     '分包费',     5, 'enabled'),
  ('budget_type',          'other',           '其他费用',   6, 'enabled'),
  -- 20. 预算类型状态
  ('budget_type_status',   'enabled',         '启用',       1, 'enabled'),
  ('budget_type_status',   'sealed',          '封存',       2, 'enabled'),
  -- 21. 公司文档分类
  ('company_doc_category', 'regulation',      '公司制度',   1, 'enabled'),
  ('company_doc_category', 'specification',   '技术规范',   2, 'enabled'),
  ('company_doc_category', 'management',      '管理文件',   3, 'enabled'),
  -- 22. 请假类型
  ('leave_type',           'annual',          '年假',       1, 'enabled'),
  ('leave_type',           'personal',        '事假',       2, 'enabled'),
  ('leave_type',           'sick',            '病假',       3, 'enabled'),
  ('leave_type',           'marriage',        '婚假',       4, 'enabled'),
  ('leave_type',           'maternity',       '产假',       5, 'enabled'),
  ('leave_type',           'compensatory',    '调休',       6, 'enabled'),
  -- 23. 缺卡类型
  ('card_miss_type',       'on-duty',         '上班',       1, 'enabled'),
  ('card_miss_type',       'off-duty',        '下班',       2, 'enabled'),
  ('card_miss_type',       'whole-day',       '全天',       3, 'enabled');

-- ---------------------------------------------------------------------
-- 1. 基础资料 bas_basic_info
--    info_category: material材料分类 / equipment设备分类 / worktype工种分类
--    status:        active在用 / archived已归档
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_basic_info`
  (`id`, `info_code`, `info_name`, `info_category`, `specification`, `unit`, `reference_price`, `status`, `remark`, `create_by`) VALUES
  (1, 'ZL-001', 'HRB400 钢筋',    'material',  'Φ25',      '吨',  4800.00,    'active',   '主体结构用钢',   '系统管理员'),
  (2, 'ZL-002', 'C35 商品混凝土', 'material',  'C35',      'm³',  520.00,     'active',   '',               '系统管理员'),
  (3, 'SB-001', '盾构机',         'equipment', 'Φ6.28m',   '台',  35000000.00,'active',   '区间掘进主设备', '系统管理员'),
  (4, 'SB-002', '塔式起重机',     'equipment', 'QTZ80',    '台',  380000.00,  'active',   '',               '系统管理员'),
  (5, 'GZ-001', '钢筋工',         'worktype',  '—',        '人日', 420.00,    'active',   '',               '系统管理员'),
  (6, 'GZ-002', '电焊工',         'worktype',  '—',        '人日', 480.00,    'archived', '证书到期待换',   '系统管理员');

-- ---------------------------------------------------------------------
-- 2. 客户 bas_customer
--    customer_type: government政府单位 / enterprise企业客户 / state-owned国企客户
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_customer`
  (`id`, `customer_code`, `customer_name`, `customer_type`, `contact_person`, `contact_phone`, `address`, `office_address`,
   `invoice_title`, `tax_no`, `phone`, `bank_name`, `bank_account`, `status`, `remark`, `create_by`) VALUES
  (1, 'KH-001', '蒙特雷城轨项目管理有限公司', 'enterprise',  'Carlos Ruiz', '52-81-1234-5678', '墨西哥新莱昂州蒙特雷市', 'Av. Constitución 1000', '蒙特雷城轨项目管理有限公司', 'RFC-20250001', '52-81-1234-0000', 'BBVA México', '0123456789', 'enabled', '墨西哥6号线业主方', '系统管理员'),
  (2, 'KH-002', '江南轨道装备制造有限公司', 'state-owned', '刘工',       '025-8888-0001',   '江苏省南京市浦口区',     '浦镇大道1号',           '江南轨道装备制造有限公司', '91320100MA001', '025-8888-0000', '中国银行南京浦口支行', '621700000001', 'enabled', '集团内部单位', '系统管理员'),
  (3, 'KH-003', '南京市轨道交通建设指挥部', 'government', '陈处',       '025-6666-0002',   '江苏省南京市玄武区',     '中山路100号',           '南京市轨道交通建设指挥部', '—',            '025-6666-0000', '中国人民银行', '100000000001', 'enabled', '政府监管单位', '系统管理员');

-- ---------------------------------------------------------------------
-- 3. 供应商 bas_supplier
--    supplier_type: material材料供应商 / equipment设备供应商 / labor劳务分包
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_supplier`
  (`id`, `supplier_code`, `supplier_name`, `supplier_type`, `contact_person`, `contact_phone`, `contact_address`, `office_address`,
   `quoter`, `quoter_phone`, `account_name`, `bank_name`, `bank_account`, `status`, `remark`, `create_by`) VALUES
  (1, 'GYS-001', '南京钢铁贸易有限公司',   'material',  '赵经理', '13900000001', '江苏省南京市江宁区', '江宁经济开发区5号', '赵明', '13900000001', '南京钢铁贸易有限公司',   '工商银行南京分行', '622200001111', 'enabled', '钢筋主供方',   '系统管理员'),
  (2, 'GYS-002', '江苏建华混凝土集团',     'material',  '孙经理', '13900000002', '江苏省镇江市句容市', '句容市开发区8号',   '孙涛', '13900000002', '江苏建华混凝土集团',     '建设银行镇江分行', '622200002222', 'enabled', '商品混凝土',   '系统管理员'),
  (3, 'GYS-003', '徐州重工设备租赁公司',   'equipment', '周主管', '13900000003', '江苏省徐州市泉山区', '徐工路18号',        '周斌', '13900000003', '徐州重工设备租赁公司',   '农业银行徐州分行', '622200003333', 'enabled', '盾构/吊装设备', '系统管理员'),
  (4, 'GYS-004', '四川华建劳务分包公司',   'labor',     '吴队长', '13900000004', '四川省成都市武侯区', '天府大道200号',     '吴强', '13900000004', '四川华建劳务分包公司',   '交通银行成都分行', '622200004444', 'enabled', '钢筋/模板班组', '系统管理员');

-- ---------------------------------------------------------------------
-- 4. 内部单位 bas_internal_unit
--    unit_type: branch分公司 / project项目部 / department部门
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_internal_unit`
  (`id`, `unit_code`, `unit_name`, `unit_type`, `manager`, `contact_person`, `contact_phone`, `office_address`, `status`, `remark`, `create_by`) VALUES
  (1, 'NB-001', '海外事业部',         'branch',     '张总',   '张伟', '13800000010', '南京市浦口区浦镇大道1号', 'enabled', '海外项目管理归口', '系统管理员'),
  (2, 'NB-002', '蒙特雷项目经理部',   'project',    '李经理', '李强', '13800000011', '墨西哥蒙特雷市项目现场', 'enabled', '6号线延长线项目部', '系统管理员'),
  (3, 'NB-003', '技术部',             'department', '王主任', '王芳', '13800000012', '南京市浦口区浦镇大道1号', 'enabled', '施工技术管理', '系统管理员'),
  (4, 'NB-004', '综合管理部',         'department', '赵主任', '赵敏', '13800000013', '南京市浦口区浦镇大道1号', 'enabled', '行政/人事/后勤', '系统管理员');

-- ---------------------------------------------------------------------
-- 5. 工种类型 bas_work_type
--    status: enabled启用 / disabled停用 / sealed封存
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_work_type` (`id`, `type_name`, `status`, `create_by`) VALUES
  (1, '钢筋工', 'enabled',  '系统管理员'),
  (2, '木工',   'enabled',  '系统管理员'),
  (3, '电焊工', 'enabled',  '系统管理员'),
  (4, '架子工', 'enabled',  '系统管理员'),
  (5, '盾构司机', 'enabled', '系统管理员'),
  (6, '特种作业-爆破工', 'sealed', '系统管理员');

-- ---------------------------------------------------------------------
-- 6. 预算类型 bas_budget_type
--    budget_type 与 pm_project_budget 的六类分项金额字段一一对应
--    status: enabled启用 / sealed封存
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_budget_type`
  (`id`, `budget_type`, `secondary_budget_type`, `secondary_budget_type_code`, `status`, `remark`, `create_by`) VALUES
  (1,  'labor',       '人工费-基本工资', 'RGF-JB-001', 'enabled', '一线班组基本工资', '系统管理员'),
  (2,  'labor',       '人工费-加班工资', 'RGF-JB-002', 'enabled', '',                 '系统管理员'),
  (3,  'material',    '材料费-钢筋',     'CLF-GJ-001', 'enabled', '含钢筋、套筒',     '系统管理员'),
  (4,  'material',    '材料费-混凝土',   'CLF-HNT-001','enabled', '',                 '系统管理员'),
  (5,  'equipment',   '设备费-设备租赁', 'SBF-ZL-001', 'enabled', '盾构/吊装设备',    '系统管理员'),
  (6,  'equipment',   '设备费-设备折旧', 'SBF-ZJ-001', 'enabled', '',                 '系统管理员'),
  (7,  'expense',     '费用-差旅费',     'FYF-CL-001', 'enabled', '含海外差旅',       '系统管理员'),
  (8,  'expense',     '费用-办公费',     'FYF-BG-001', 'enabled', '',                 '系统管理员'),
  (9,  'subcontract', '分包费-专业分包', 'FBF-ZY-001', 'enabled', '盾构专业分包',     '系统管理员'),
  (10, 'subcontract', '分包费-劳务分包', 'FBF-LW-001', 'enabled', '',                 '系统管理员'),
  (11, 'other',       '其他费用-其他',   'QTF-QT-001', 'sealed',  '已封存，不再选用', '系统管理员');

-- ---------------------------------------------------------------------
-- 7. 公司文档 bas_company_doc
--    doc_category: regulation公司制度 / specification技术规范 / management管理文件
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_company_doc`
  (`id`, `doc_code`, `doc_name`, `doc_title`, `doc_category`, `version`, `publish_dept`, `publish_date`,
   `doc_description`, `upload_by`, `upload_time`, `create_by`) VALUES
  (1, 'WD-001', '蒙特雷项目管理规范', '蒙特雷项目管理规范',   'regulation',    'V1.0', '综合管理部', '2025-01-01', '项目管理规范化要求。',       '系统管理员', '2025-01-01 09:00:00', '系统管理员'),
  (2, 'WD-002', '海外项目安全技术规范', '海外项目安全技术规范', 'specification', 'V2.0', '安全部',     '2025-03-01', '海外项目安全管理技术标准。', '系统管理员', '2025-03-01 09:00:00', '系统管理员'),
  (3, 'WD-003', '盾构施工管理文件',   '盾构施工管理文件',     'management',    'V1.2', '技术部',     '2025-05-10', '盾构区间施工管理流程。',     '系统管理员', '2025-05-10 09:00:00', '系统管理员');

-- ---------------------------------------------------------------------
-- 8. 出差申请 bas_business_trip
--    approval_status: pending未审批 / approved已通过 / rejected已驳回
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_business_trip`
  (`id`, `apply_no`, `project_id`, `applicant`, `trip_destination`, `start_date`, `end_date`, `trip_reason`,
   `apply_time`, `approval_status`, `approver`, `approval_opinion`, `approval_time`, `create_by`) VALUES
  (1, 'CC-2025-001', (SELECT id FROM `pm_project` WHERE id = 1), '吴十',   '墨西哥城',     '2025-09-01', '2025-09-05', '参加业主协调会',       '2025-08-20 09:00:00', 'approved', '系统管理员', '同意，注意行程安全', '2025-08-21 10:00:00', '系统管理员'),
  (2, 'CC-2025-002', (SELECT id FROM `pm_project` WHERE id = 2), '张文琦', '墨西哥蒙特雷', '2025-09-12', '2025-09-20', '6号线现场技术交底',   '2025-09-05 09:00:00', 'pending',  NULL,         NULL,                 NULL,                  '系统管理员'),
  (3, 'CC-2025-003', (SELECT id FROM `pm_project` WHERE id = 3), '王五',   '江苏省南京市', '2025-08-01', '2025-08-03', '设备厂家验收',         '2025-07-25 09:00:00', 'rejected', '系统管理员', '与安全培训冲突，改期', '2025-07-26 10:00:00', '系统管理员');

-- ---------------------------------------------------------------------
-- 9. 请假申请 bas_leave_application
--    leave_type: annual年假 / personal事假 / sick病假 / marriage婚假 / maternity产假 / compensatory调休
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_leave_application`
  (`id`, `apply_no`, `applicant`, `leave_type`, `start_date`, `end_date`, `leave_days`, `leave_reason`,
   `apply_time`, `approval_status`, `approver`, `approval_opinion`, `approval_time`, `create_by`) VALUES
  (1, 'QJ-2025-001', '张文琦', 'annual',       '2025-09-10', '2025-09-12', 3.0, '家庭事务',       '2025-08-21 09:00:00', 'approved', '系统管理员', '同意',        '2025-08-22 10:00:00', '系统管理员'),
  (2, 'QJ-2025-002', '王五',   'sick',         '2025-09-15', '2025-09-16', 2.0, '感冒发热就医',   '2025-09-14 09:00:00', 'pending',  NULL,         NULL,          NULL,                  '系统管理员'),
  (3, 'QJ-2025-003', '吴十',   'compensatory', '2025-08-20', '2025-08-20', 1.0, '海外出差调休',   '2025-08-15 09:00:00', 'approved', '系统管理员', '同意',        '2025-08-16 10:00:00', '系统管理员');

-- ---------------------------------------------------------------------
-- 10. 补卡申请 bas_makeup_application
--     card_miss_type: on-duty上班 / off-duty下班 / whole-day全天
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `bas_makeup_application`
  (`id`, `apply_no`, `applicant`, `card_miss_date`, `card_miss_type`, `card_miss_time`, `makeup_reason`,
   `witness`, `apply_time`, `approval_status`, `approver`, `approval_opinion`, `approval_time`, `create_by`) VALUES
  (1, 'BK-2025-001', '王五',   '2025-08-18', 'on-duty',   '08:30', '地铁故障迟到',   '李四', '2025-08-18 09:00:00', 'approved', '系统管理员', '情况属实',   '2025-08-19 10:00:00', '系统管理员'),
  (2, 'BK-2025-002', '张文琦', '2025-09-08', 'off-duty',  '18:00', '加班未打卡',     '吴十', '2025-09-09 09:00:00', 'pending',  NULL,         NULL,         NULL,                  '系统管理员'),
  (3, 'BK-2025-003', '吴十',   '2025-07-15', 'whole-day', '全天',  '现场无打卡设备', '王五', '2025-07-16 09:00:00', 'rejected', '系统管理员', '需项目部出具证明', '2025-07-17 10:00:00', '系统管理员');

-- ---------------------------------------------------------------------
-- 11. 回执
-- ---------------------------------------------------------------------
SELECT 'bas_basic_info' AS 表, COUNT(*) AS 行数 FROM `bas_basic_info`
UNION ALL SELECT 'bas_customer',           COUNT(*) FROM `bas_customer`
UNION ALL SELECT 'bas_supplier',           COUNT(*) FROM `bas_supplier`
UNION ALL SELECT 'bas_internal_unit',      COUNT(*) FROM `bas_internal_unit`
UNION ALL SELECT 'bas_work_type',          COUNT(*) FROM `bas_work_type`
UNION ALL SELECT 'bas_budget_type',        COUNT(*) FROM `bas_budget_type`
UNION ALL SELECT 'bas_company_doc',        COUNT(*) FROM `bas_company_doc`
UNION ALL SELECT 'bas_business_trip',      COUNT(*) FROM `bas_business_trip`
UNION ALL SELECT 'bas_leave_application',  COUNT(*) FROM `bas_leave_application`
UNION ALL SELECT 'bas_makeup_application', COUNT(*) FROM `bas_makeup_application`;
