-- =====================================================================
-- PIDMS · 全表联调测试数据（补齐无数据业务表）
-- 数据库：MySQL 8.x     引擎：InnoDB     字符集：utf8mb4
--
-- 作用：
--   在 03_seed.sql + 04_test_data.sql 之后执行，补齐两件事：
--   ① 将 seed 中不足 5 条的表补齐到 5 条（sys_role / sys_user / sys_user_role / pm_project_status）；
--   ② 为所有尚无数据的业务表补齐可联调跑通的测试数据，每表 5 条。
--   最终实现 29 张表每表 ≥5 条、可全链路联调。
--
-- 执行顺序（完整初始化，全新库）：
--   01_create_database.sql → 02_schema.sql → 03_seed.sql
--   → upgrade/05_add_login_columns.sql（给 sys_user 加 username/password 登录列）
--   → 04_test_data.sql → 05_test_data_all.sql（本文件）
--
-- ⚠ 存量旧库（sys_user_role 没有 id 列 / 无审计字段的库）必须先对齐：
--   先执行 upgrade/04_align_database_to_design.sql 把库升级到设计稿结构，
--   再跑 04 → 05。否则 cm_ 施工域 11 张表仍是旧结构，本文件 cm_ 部分会
--   报 Unknown column（pm_project_budget 的一期六类分项列由 upgrade/08 补齐，
--   存量旧库请先执行 upgrade/08_budget_flatten.sql 再加 04/05）。
--
-- 依赖说明：
--   · pm_project 表已被 04_test_data.sql TRUNCATE 后按顺序插入 18 条，
--     其 id 为 1~18，本文件 project_id 均引用这些 id；
--   · sys_user(id 1~4)、pm_seal_type(id 1~6)、sys_org(id 1~5)
--     由 03_seed.sql 提供；
--   · sys_user 的 username/password 列由 upgrade/05_add_login_columns.sql
--     添加，本文件新增的第 5 个用户(李四/lisi)依赖该脚本已执行；
--   · 种子补齐部分使用 INSERT IGNORE，业务表部分头部 TRUNCATE，
--     因此本文件可重复执行。
--
-- 外键依赖顺序（TRUNCATE 逆序 / INSERT 正序）：
--   检查项 → 检查单 → 检查明细 → 整改单 → 整改明细 → 预算 → 其余业务表
-- =====================================================================

USE `pidms`;

-- ---------------------------------------------------------------------
-- 〇、清空本文件将填充的表
--   注意：这些表相互间存在外键约束（如 cm_quality_rectification 被
--   cm_quality_rectification_item 引用）。MySQL 的 TRUNCATE 只要父表存在
--   子表外键约束就会报 1701（与子表是否有数据无关），因此先关闭外键检查，
--   全部清空后再恢复，保证可重复执行。
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `cm_quality_rectification_item`;
TRUNCATE TABLE `cm_quality_inspection_item`;
TRUNCATE TABLE `cm_quality_rectification`;
TRUNCATE TABLE `cm_quality_inspection`;
TRUNCATE TABLE `cm_safety_rectification_item`;
TRUNCATE TABLE `cm_safety_inspection_item`;
TRUNCATE TABLE `cm_safety_rectification`;
TRUNCATE TABLE `cm_safety_inspection`;
TRUNCATE TABLE `pm_project_budget_item`;
TRUNCATE TABLE `pm_project_budget`;
TRUNCATE TABLE `cm_progress`;
TRUNCATE TABLE `cm_construction_log`;
TRUNCATE TABLE `cm_warning_rule`;
TRUNCATE TABLE `pm_project_doc`;
TRUNCATE TABLE `pm_seal_application`;
TRUNCATE TABLE `sys_operation_log`;
TRUNCATE TABLE `sys_attachment`;
TRUNCATE TABLE `cm_quality_check_item`;
TRUNCATE TABLE `cm_safety_check_item`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 〇.1 种子表补齐：seed 中不足 5 条的表补到 5 条（INSERT IGNORE 可重复执行）
--   sys_org(5)、sys_role(4→5)、sys_user(4→5)、sys_user_role(4→5)、
--   sys_role_permission(多条)、pm_project_status(4→5)、pm_seal_type(6)
-- ---------------------------------------------------------------------

-- 2. 角色表：补充第 5 个角色「安全员」
INSERT IGNORE INTO `sys_role` (`id`, `role_code`, `role_name`, `role_description`, `status`, `create_by`) VALUES
  (5, 'SAFETY_OFFICER', '安全员', '安全与质量检查权限', 'enabled', 'admin');

-- 3. 用户表：补充第 5 个用户「李四」（依赖 upgrade/05_add_login_columns.sql 的 username/password 列）
INSERT IGNORE INTO `sys_user`
(`id`, `name`, `gender`, `employee_no`, `phone`, `org_id`, `status`, `remark`, `username`, `password`) VALUES
  (5, '李四', '男', 'P005', '13800138004', 3, 'enabled', '安全员', 'lisi', '$2a$10$uh.1yykU2A7CnJzvuDroXuQSbci1qHOGC4oYu6HvDbfLlfEGNvqLW');

-- 4. 用户-角色：李四 → 安全员
--   注意：旧库的 sys_user_role 没有 id 列（upgrade/04_align 才新增），
--   因此只插 (user_id, role_id)，新旧两种结构都兼容。
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
  (5, 5);

-- 14. 项目状态定义表：补充第 5 个状态「已终止」
INSERT IGNORE INTO `pm_project_status` (`id`, `status_name`, `status_code`, `status`, `create_by`) VALUES
  (5, '已终止', 'terminated', 'enabled', 'admin');

-- =====================================================================
-- 一、系统管理域（sys_）
-- =====================================================================

-- 8. 附件表（项目文档 / 施工日志图片 / 用印文件 统一落此表）
--   注意：旧库 sys_attachment 无 create_by/create_time（upgrade/04_align 才补），
--   因此不插审计字段，新旧结构都兼容。
INSERT INTO `sys_attachment`
(`id`, `biz_type`, `biz_id`, `file_name`, `file_path`, `file_size`, `file_type`, `upload_by`, `upload_time`) VALUES
  (1, 'project-doc',      1, '6号线延长线总平面布置图.pdf',       '/uploads/project-doc/2026/03/6hx_zpm.pdf',        2097152, 'pdf', '张文琦', '2026-03-10 10:00:00'),
  (2, 'project-doc',      2, '车站主体结构施工方案V2.0.pdf',      '/uploads/project-doc/2026/04/sgfa_v2.pdf',        5242880, 'pdf', '吴十',   '2026-04-15 14:30:00'),
  (3, 'construction-log', 1, '现场照片_20260511_1.jpg',           '/uploads/construction-log/2026/05/img_001.jpg',   1536000, 'jpg', '张文琦', '2026-05-11 18:00:00'),
  (4, 'construction-log', 3, '现场照片_20260708_2.jpg',           '/uploads/construction-log/2026/07/img_001.jpg',   1245184, 'jpg', '吴十',   '2026-07-08 18:00:00'),
  (5, 'seal-application', 1, '土建分包合同.pdf',                  '/uploads/seal-application/2026/05/yj_0001.pdf',   3670016, 'pdf', '张文琦', '2026-05-06 09:30:00');

-- 9. 操作日志表（审计留痕）
INSERT INTO `sys_operation_log`
(`id`, `user_name`, `module`, `action`, `method`, `path`, `request_id`, `ip`, `detail`, `create_time`) VALUES
  (1, '系统管理员', 'project',            '新增',     'POST',   '/api/projects',              'REQ-20260501-001', '127.0.0.1', '新增项目：蒙特雷地铁6号线延长线项目',                 '2026-05-01 09:00:00'),
  (2, '吴十',       'seal-application',   '审批通过', 'PUT',    '/api/seal-applications/1',   'REQ-20260507-002', '127.0.0.1', '审批通过用印申请：YY-2026-0001',                       '2026-05-07 10:30:00'),
  (3, '张文琦',     'progress',           '新增',     'POST',   '/api/progress',              'REQ-20260511-003', '127.0.0.1', '新增进度：车站土建施工',                               '2026-05-11 17:00:00'),
  (4, '王五',       'quality-inspection', '新增',     'POST',   '/api/quality-inspections',   'REQ-20260720-004', '127.0.0.1', '新增质量检查单：QI-2026-0004',                         '2026-07-20 15:00:00'),
  (5, '系统管理员', 'project',            '归档',     'PUT',    '/api/projects/14',           'REQ-20251226-005', '127.0.0.1', '归档项目：蒙特雷地铁6号线延长线广场改造项目',         '2025-12-26 11:00:00');

-- =====================================================================
-- 二、项目管理域（pm_）
-- =====================================================================

-- 11. 项目预算表（一期平面预算：一条预算一行，六类分项金额直接存主表）
INSERT INTO `pm_project_budget`
(`id`, `project_id`, `budget_version`, `budget_total`,
 `labor_budget`, `material_budget`, `equipment_budget`, `expense_budget`, `subcontract_budget`, `other_budget`,
 `secondary_budget_summary`, `approval_status`, `revision_status`, `approver`, `cc_person`, `remark`, `create_by`) VALUES
  (1, 1,  'V1.0', 268000000.00,  65000000.00, 118000000.00,  32000000.00,  15000000.00, 38000000.00, NULL, '土建、轨道、车站',             'approved', 'unrevised', '系统管理员', '吴十,张文琦', '2026年度施工图预算',          'wushi'),
  (2, 1,  'V1.1', 273500000.00,  82000000.00, 110000000.00,  42000000.00,  39500000.00, NULL,        NULL, '土建、轨道、车站、机电',       'approved', 'revised',   '系统管理员', '吴十,张文琦', '根据设计变更调整后版本',      'admin'),
  (3, 2,  'V1.0', 96500000.00,   26000000.00,  38000000.00,  19000000.00,  13500000.00, NULL,        NULL, '高架区间、车站',               'pending',  'unrevised', NULL,         '吴十',        NULL,                          'wushi'),
  (4, 5,  'V1.0', 42800000.00,   11800000.00,  16000000.00,   9000000.00,   6000000.00, NULL,        NULL, '信号系统设备及安装',           'approved', 'unrevised', '系统管理员', '张文琦',      NULL,                          'admin'),
  (5, 11, 'V1.0', 1560000000.00, 330000000.00, 640000000.00, 380000000.00, 210000000.00, NULL,       NULL, '30列新型城轨车辆采购',         'pending',  'unrevised', NULL,         '王五',        '待业主审批采购方案',          'admin');

-- 12. pm_project_budget_item：一期预留不读写，二期「多行明细录入」启用后再补示例数据。

-- 13. 项目文档表
INSERT INTO `pm_project_doc`
(`id`, `project_id`, `doc_name`, `doc_category`, `doc_type`, `version`, `doc_description`, `upload_by`, `upload_time`, `create_by`, `create_time`) VALUES
  (1, 1,  '6号线延长线总平面布置图',      '施工图纸', '施工图纸', 'V1.0', '车站及区间总平面布置',                 '张文琦', '2026-03-10 10:00:00', '张文琦', '2026-03-10 10:00:00'),
  (2, 1,  '车站主体结构施工方案',         '技术资料', '施工方案', 'V2.0', '3号车站主体结构施工组织',             '吴十',   '2026-04-15 14:30:00', '吴十',   '2026-04-15 14:30:00'),
  (3, 2,  '高架区间墩柱施工图',           '施工图纸', '施工图纸', 'V1.0', '2号~5号墩柱结构图纸',                 '张文琦', '2026-05-20 09:00:00', '张文琦', '2026-05-20 09:00:00'),
  (4, 14, '广场改造工程竣工验收报告',     '验收资料', '验收报告', 'V1.0', '竣工验收及移交资料',                   '王五',   '2025-12-25 16:00:00', '王五',   '2025-12-25 16:00:00'),
  (5, 3,  '综合管网迁改技术交底',         '技术资料', '技术交底', 'V1.0', '管线迁改安全技术交底记录',             '吴十',   '2026-06-08 11:00:00', '吴十',   '2026-06-08 11:00:00');

-- 16. 用印申请表
INSERT INTO `pm_seal_application`
(`id`, `bill_no`, `title`, `project_id`, `applicant`, `apply_date`, `seal_department`, `seal_type_id`, `seal_file_name`, `file_copies`, `seal_method`, `seal_description`, `approval_status`, `approver`, `remark`, `create_by`) VALUES
  (1, 'YY-2026-0001', '6号线延长线土建分包合同用印',   1,  '张文琦', '2026-05-06', '项目三部',     2, '土建分包合同.pdf', 3, '原件盖章',   '土建分包合同用印，共3份',         'approved', '系统管理员', NULL,   '张文琦'),
  (2, 'YY-2026-0002', '物资采购合同用印申请',         1,  '吴十',   '2026-06-18', '项目三部',     1, '钢材采购合同.pdf', 2, '原件盖章',   '钢材采购合同用印',               'pending',  NULL,         NULL,   '吴十'),
  (3, 'YY-2026-0003', '财务付款申请用印',             5,  '王五',   '2026-07-02', '海外事业部',   3, '付款审批单.pdf',   1, '原件盖章',   '设备预付款申请',                 'rejected', '系统管理员', '资料不全退回', '王五'),
  (4, 'YY-2026-0004', '高架区间施工协议用印',         2,  '吴十',   '2026-07-15', '项目三部',     4, '施工协议.pdf',     2, '复印件盖章', '施工协议盖章',                   'approved', '系统管理员', NULL,   '吴十'),
  (5, 'YY-2026-0005', '竣工结算文件用印',             14, '王五',   '2026-08-01', '项目三部',     6, '竣工结算书.pdf',   4, '原件盖章',   '竣工结算书用印',                 'pending',  NULL,         NULL,   '王五');

-- =====================================================================
-- 三、施工管理域（cm_）
-- =====================================================================

-- 17. 进度管理表
INSERT INTO `cm_progress`
(`id`, `project_id`, `progress_name`, `progress_code`, `plan_start_date`, `plan_end_date`, `actual_start_date`, `actual_end_date`, `completion_percent`, `progress_status`, `remark`, `create_by`) VALUES
  (1, 1,  '车站土建施工',          'PLAN-ST-001', '2023-04-01', '2026-09-30', '2023-04-10', NULL,         65.50, 'in-progress', '9座车站主体施工中',        'wushi'),
  (2, 1,  '正线轨道铺设',          'PLAN-GD-001', '2024-08-01', '2026-07-31', '2024-08-10', NULL,         42.00, 'in-progress', '已铺轨约5.3公里',          'admin'),
  (3, 2,  '高架区间桩基及墩柱',    'PLAN-ZJ-001', '2023-07-01', '2025-12-31', '2023-07-15', '2025-12-20', 100.00, 'completed',   '桩基及墩柱全部完成',        'wushi'),
  (4, 14, '广场改造及景观提升',    'PLAN-GC-001', '2024-06-01', '2025-11-30', '2024-06-05', '2025-11-25', 100.00, 'completed',   '已竣工移交',                'admin'),
  (5, 5,  '信号系统安装调试',      'PLAN-XH-001', '2025-02-01', '2026-08-31', '2025-02-10', NULL,         55.00, 'delayed',     '受设备到货影响滞后',        'wushi');

-- 18. 质量检查项表
INSERT INTO `cm_quality_check_item`
(`id`, `category`, `check_item_name`, `status`, `create_by`) VALUES
  (1, '结构工程', '混凝土结构外观质量检查',       'enabled', 'admin'),
  (2, '结构工程', '钢筋绑扎间距及保护层检查',     'enabled', 'admin'),
  (3, '装饰工程', '墙面平整度及垂直度检查',       'enabled', 'wushi'),
  (4, '安装工程', '机电管线安装质量检查',         'enabled', 'admin'),
  (5, '防水工程', '防水层施工质量检查',           'sealed',  'wushi');

-- 19. 质量检查表
INSERT INTO `cm_quality_inspection`
(`id`, `inspection_no`, `project_id`, `inspection_type`, `inspection_dept`, `inspection_date`, `inspection_location`, `inspector`, `inspection_result`, `inspection_detail`, `create_by`) VALUES
  (1, 'QI-2026-0001', 1,  'quality', '质量安全部', '2026-05-10', '3号车站主体结构',     '张文琦', 'qualified',   '混凝土强度及外观符合要求',        'wushi'),
  (2, 'QI-2026-0002', 1,  'quality', '质量安全部', '2026-06-15', '4号车站钢筋工程',     '吴十',   'unqualified', '钢筋间距个别部位超标',            'admin'),
  (3, 'QI-2026-0003', 2,  'quality', '质量安全部', '2026-07-01', '高架区间2号墩柱',     '张文琦', 'qualified',   '墩柱垂直度满足规范',              'wushi'),
  (4, 'QI-2026-0004', 5,  'quality', '监理单位',   '2026-07-20', '信号机房设备安装',     '王五',   'pending',     '待设备安装完成后复检',            'admin'),
  (5, 'QI-2026-0005', 1,  'quality', '质量安全部', '2026-08-05', '5号车站防水工程',     '吴十',   'qualified',   '防水层搭接质量合格',              'wushi');

-- 20. 质量检查明细表
INSERT INTO `cm_quality_inspection_item`
(`id`, `inspection_id`, `check_item_id`, `check_result`, `remark`, `create_by`) VALUES
  (1, 1, 1, 'qualified',   '外观密实，无明显蜂窝麻面',   'wushi'),
  (2, 1, 2, 'qualified',   '间距符合设计',               'wushi'),
  (3, 2, 1, 'unqualified', '4号柱钢筋间距超差，需整改',  'admin'),
  (4, 2, 2, 'qualified',   '保护层厚度合格',             'admin'),
  (5, 5, 5, 'qualified',   '搭接长度满足规范',           'wushi');

-- 21. 质量整改单表
INSERT INTO `cm_quality_rectification`
(`id`, `rectification_no`, `project_id`, `related_inspection_id`, `rectification_location`, `required_complete_date`, `rectification_status`, `responsible_person`, `rectification_content`, `create_by`) VALUES
  (1, 'QR-2026-0001', 1,  2,    '4号车站钢筋工程',         '2026-06-30', 'processing', '吴十',   '钢筋间距超差部位进行返工调整',       'admin'),
  (2, 'QR-2026-0002', 2,  NULL, '高架区间2号墩柱',         '2026-08-15', 'pending',    '王五',   '墩柱施工缝处理',                     'wushi'),
  (3, 'QR-2026-0003', 1,  5,    '5号车站防水层',           '2026-08-20', 'pending',    '张文琦', '局部防水层空鼓返修',                 'wushi'),
  (4, 'QR-2026-0004', 14, NULL, '广场铺装返修',            '2025-10-31', 'completed',  '王五',   '花岗岩铺装空鼓部位返修',             'admin'),
  (5, 'QR-2026-0005', 3,  NULL, '综合管廊沉降观测',        '2026-09-10', 'processing', '吴十',   '管廊沉降异常段加固处理',             'admin');

-- 22. 质量整改明细表
INSERT INTO `cm_quality_rectification_item`
(`id`, `rectification_id`, `check_item_id`, `rectification_content`, `item_status`, `create_by`) VALUES
  (1, 1, 1, '对4号柱钢筋间距超差部位返工',       'processing', 'admin'),
  (2, 1, 2, '增加垫块保证保护层厚度',           'pending',    'admin'),
  (3, 2, 1, '施工缝凿毛清理并重新浇筑',         'pending',    'wushi'),
  (4, 4, 1, '空鼓花岗岩全部更换',               'completed',  'admin'),
  (5, 5, 4, '沉降段增设监测点并注浆加固',       'processing', 'admin');

-- 23. 安全检查项表
INSERT INTO `cm_safety_check_item`
(`id`, `category`, `check_item_name`, `status`, `create_by`) VALUES
  (1, '用电安全',   '施工现场临时用电安全检查',         'enabled', 'admin'),
  (2, '高空作业',   '高处作业安全防护措施检查',         'enabled', 'admin'),
  (3, '消防安全',   '施工现场消防器材配置检查',         'enabled', 'wushi'),
  (4, '机械安全',   '大型机械操作人员持证情况检查',     'enabled', 'admin'),
  (5, '用电安全',   '接地保护及漏电保护检查',           'disabled','wushi');

-- 24. 安全检查表
INSERT INTO `cm_safety_inspection`
(`id`, `inspection_no`, `project_id`, `inspection_type`, `inspection_dept`, `inspection_date`, `inspection_location`, `inspector`, `inspection_result`, `inspection_detail`, `create_by`) VALUES
  (1, 'SI-2026-0001', 1,  'safety', '安全环保部', '2026-05-12', '3号车站临电',         '张文琦', 'qualified',   '临时用电规范，配电箱完好',       'wushi'),
  (2, 'SI-2026-0002', 1,  'safety', '安全环保部', '2026-06-18', '4号车站高空作业',     '吴十',   'unqualified', '部分作业人员未系安全带',         'admin'),
  (3, 'SI-2026-0003', 2,  'safety', '安全环保部', '2026-07-05', '高架区间架梁作业',     '王五',   'qualified',   '警戒区设置规范',                 'wushi'),
  (4, 'SI-2026-0004', 5,  'safety', '监理单位',   '2026-07-22', '信号机房消防安全',     '张文琦', 'pending',     '待灭火器配置到位后复检',         'admin'),
  (5, 'SI-2026-0005', 3,  'safety', '安全环保部', '2026-08-08', '管廊基坑支护',         '吴十',   'qualified',   '基坑监测数据正常',               'wushi');

-- 25. 安全检查明细表
INSERT INTO `cm_safety_inspection_item`
(`id`, `inspection_id`, `check_item_id`, `check_result`, `remark`, `create_by`) VALUES
  (1, 1, 1, 'qualified',   '配电箱接地良好',             'wushi'),
  (2, 1, 3, 'qualified',   '灭火器配置充足',             'wushi'),
  (3, 2, 2, 'unqualified', '2名工人未系安全带',          'admin'),
  (4, 2, 4, 'qualified',   '持证情况齐全',               'admin'),
  (5, 5, 3, 'qualified',   '消防通道畅通',               'wushi');

-- 26. 安全整改单表
INSERT INTO `cm_safety_rectification`
(`id`, `rectification_no`, `project_id`, `related_inspection_id`, `rectification_location`, `required_complete_date`, `rectification_status`, `responsible_person`, `rectification_content`, `create_by`) VALUES
  (1, 'SR-2026-0001', 1,  2,    '4号车站高空作业面',       '2026-06-25', 'completed',  '吴十',   '安全带及防坠网全部整改到位',       'admin'),
  (2, 'SR-2026-0002', 5,  NULL, '信号机房',               '2026-08-10', 'pending',    '张文琦', '增配灭火器并建立消防台账',         'admin'),
  (3, 'SR-2026-0003', 3,  NULL, '管廊基坑周边',           '2026-08-20', 'processing', '王五',   '基坑周边防护栏加固',               'wushi'),
  (4, 'SR-2026-0004', 2,  NULL, '高架区间架梁区',         '2026-07-15', 'completed',  '王五',   '警戒区扩大并增设警示标识',         'wushi'),
  (5, 'SR-2026-0005', 1,  NULL, '2号施工便道',            '2026-09-05', 'pending',    '吴十',   '破损电缆更换并埋地处理',           'admin');

-- 27. 安全整改明细表
INSERT INTO `cm_safety_rectification_item`
(`id`, `rectification_id`, `check_item_id`, `rectification_content`, `item_status`, `create_by`) VALUES
  (1, 1, 2, '高处作业全部配备安全带',     'completed',  'admin'),
  (2, 1, 1, '临电线路重新敷设',           'completed',  'admin'),
  (3, 2, 3, '增配灭火器8具',              'pending',    'admin'),
  (4, 3, 4, '防护栏加固及警示灯安装',     'processing', 'wushi'),
  (5, 4, 1, '警戒区警示标识更新',         'completed',  'wushi');

-- 28. 施工日志表
INSERT INTO `cm_construction_log`
(`id`, `project_id`, `plan_name`, `log_date`, `weather`, `construction_location`, `construction_content`, `workload`, `attendance_count`, `construction_detail`, `existing_problems`, `images`, `create_by`) VALUES
  (1, 1,  '车站土建施工',          '2026-05-11', 'cloudy', '3号车站',           '车站主体结构混凝土浇筑', 820.00, 45, '完成车站3层板混凝土浇筑820方', NULL, '["/uploads/construction-log/2026/05/11/img_001.jpg"]', 'wushi'),
  (2, 1,  '正线轨道铺设',          '2026-06-12', 'sunny',  '正线K2+100-K2+600', '铺轨500米及扣件安装',     500.00, 30, '完成铺轨500米', '扣件局部松动已处理', '["/uploads/construction-log/2026/06/12/img_001.jpg"]', 'admin'),
  (3, 2,  '高架区间桩基及墩柱',    '2026-07-08', 'rainy',  '高架区间2号墩柱',     '墩柱钢筋绑扎及模板安装', 12.00,  28, '完成2号墩柱钢筋绑扎', '雨天暂停混凝土浇筑', NULL, 'wushi'),
  (4, 3,  '综合管网迁改',          '2026-08-06', 'sunny',  '市中心段',           '综合管廊顶板混凝土浇筑', 680.00, 40, '完成管廊顶板浇筑680方', NULL, NULL, 'admin'),
  (5, 5,  '信号系统安装调试',      '2026-08-20', 'cloudy', '信号机房',           'CBTC设备安装及线缆敷设', 200.00, 12, '完成设备安装，线缆敷设200米', '待联调', '["/uploads/construction-log/2026/08/20/img_001.jpg"]', 'wushi');

-- 29. 预警规则表
INSERT INTO `cm_warning_rule`
(`id`, `rule_name`, `project_id`, `plan_name`, `progress_percent`, `trigger_time`, `progress_deviation_threshold`, `warning_type`, `level_rule_config`, `status`, `remark`, `create_by`) VALUES
  (1, '车站土建进度滞后预警',   1,  '车站土建施工',        65.50, '2026-08-01 09:00:00', 10.00, '进度停滞预警', '延误超10%触发红色预警',     'enabled',  NULL,   'admin'),
  (2, '钢筋工程质量预警',       1,  '车站土建施工',        NULL,  NULL,                  5.00,  '质量问题预警', '不合格率超5%触发',          'enabled',  NULL,   'wushi'),
  (3, '高空作业安全预警',       2,  '高架区间桩基及墩柱',  NULL,  NULL,                  8.00,  '安全预警',     '安全隐患超8项触发',          'enabled',  NULL,   'admin'),
  (4, '信号安装进度预警',       5,  '信号系统安装调试',    55.00, '2026-08-15 10:30:00', 15.00, '进度停滞预警', '延误超15%触发红色预警',     'enabled',  NULL,   'wushi'),
  (5, '管廊沉降质量预警',       3,  '综合管网迁改',        NULL,  NULL,                  5.00,  '质量问题预警', '沉降超限触发',               'disabled', '暂停启用', 'admin');

-- =====================================================================
-- 全表测试数据结束
-- =====================================================================
