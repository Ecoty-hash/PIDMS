-- =====================================================================
-- PIDMS · 项目模块测试数据（pm_project）
-- 数据库：MySQL 8.x
--
-- 说明：
--   · 提供 18 条项目测试数据，覆盖 在建 / 规划中 / 已完工 / 已暂停 及 归档/未归档；
--   · project_status / archive_status 按数据库设计存英文 code，
--     前端 StatusBadge 会自动转中文显示（in-progress→在建、archived→已归档 等）；
--   · 执行前会清空 pm_project 现有数据（TRUNCATE），如需保留请删掉下方清空语句。
--   · 注意：pm_project 被 pm_project_budget / pm_project_doc / pm_seal_application /
--     cm_progress / cm_quality_inspection / cm_quality_rectification / cm_safety_inspection /
--     cm_safety_rectification / cm_construction_log / cm_warning_rule 等 10 张表外键引用，
--     直接 TRUNCATE 会报 1701（Cannot truncate a table referenced in a foreign key constraint）。
--     解决：先 SET FOREIGN_KEY_CHECKS=0 再 TRUNCATE（InnoDB 下可行，且会重置自增id=1，
--     保证本项目 id 1~18 与 05_test_data_all.sql 的引用一致），完事后再恢复。
-- =====================================================================

USE `pidms`;

-- 清空 pm_project（外键检查临时关闭，避免 1701 报错）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `pm_project`;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `pm_project`
(`project_code`, `project_name`, `owner_unit`, `construction_unit`, `construction_address`,
 `start_date`, `completion_date`, `project_status`, `project_leader`, `project_members`,
 `participation_status`, `archive_status`, `project_overview`, `remark`, `create_by`) VALUES

-- ============ 在建（in-progress） ============
('PM-2023-001', '墨西哥蒙特雷地铁6号线延长线项目', '蒙特雷市交通建设管理局', '中盛建工集团有限公司', '墨西哥新莱昂州蒙特雷市',
 '2023-03-01', '2026-12-31', 'in-progress', '吴十', '吴十,张文琦,王五',
 'participated', 'unarchived', '地铁6号线延长线土建、轨道及车站工程，全长12.6公里，共9座车站。', '重点项目，业主月度联检。', 'admin'),

('PM-2023-002', '墨西哥蒙特雷地铁4号线延长线项目', '蒙特雷市交通建设管理局', '汇通基础设施建设集团', '墨西哥蒙特雷市',
 '2023-06-15', '2026-06-30', 'in-progress', '吴十', '吴十,张文琦',
 'participated', 'unarchived', '4号线延长线高架区间及2座车站施工。', NULL, 'admin'),

('PM-2024-001', '蒙特雷地铁6号线延长线综合管网改造项目', '蒙特雷城市轨道运营有限公司', '恒通电气化工程局', '蒙特雷市中心',
 '2024-02-20', '2026-09-30', 'in-progress', '张文琦', '张文琦,王五',
 'participated', 'unarchived', '既有管网迁改及综合管廊建设。', '与6号线主体交叉施工。', 'wushi'),

('PM-2024-003', '蒙特雷地铁4号线延长线车辆段项目', '蒙特雷城市轨道运营有限公司', '松辽机车车辆制造有限公司', '蒙特雷市北部',
 '2024-08-01', '2027-03-31', 'in-progress', '吴十', '吴十,张文琦,王五',
 'participated', 'unarchived', '车辆段及综合维修基地建设。', NULL, 'admin'),

('PM-2025-001', '蒙特雷地铁6号线延长线信号系统升级项目', '蒙特雷城市轨道运营有限公司', '联信信号技术有限公司', '蒙特雷市',
 '2025-01-15', '2026-08-31', 'in-progress', '张文琦', '张文琦',
 'participated', 'unarchived', 'CBTC信号系统安装调试。', NULL, 'wushi'),

('PM-2024-004', '蒙特雷地铁6号线延长线供电系统项目', '蒙特雷城市轨道运营有限公司', '恒通电气化工程局', '蒙特雷市',
 '2024-03-15', '2026-10-31', 'in-progress', '张文琦', '张文琦,王五',
 'participated', 'unarchived', '牵引供电系统及变电所工程。', NULL, 'wushi'),

('PM-2024-005', '蒙特雷地铁6号线延长线轨道工程', '蒙特雷城市轨道运营有限公司', '中盛建工集团有限公司', '蒙特雷市',
 '2024-07-01', '2026-07-31', 'in-progress', '吴十', '吴十',
 'participated', 'unarchived', '正线及辅助线铺轨工程。', '焊轨施工中。', 'admin'),

('PM-2026-004', '蒙特雷地铁6号线延长线试运行调试项目', '蒙特雷城市轨道运营有限公司', '松辽机车车辆制造有限公司', '蒙特雷市',
 '2026-05-01', '2026-12-31', 'in-progress', '张文琦', '张文琦,吴十',
 'participated', 'unarchived', '列车试运行及联调联试。', NULL, 'admin'),

('PM-2026-005', '蒙特雷地铁6号线延长线沿线土地开发前期研究', '蒙特雷市交通建设管理局', '华通路桥建设集团', '蒙特雷市',
 '2026-03-01', '2026-11-30', 'in-progress', '王五', '王五',
 'not-participated', 'unarchived', 'TOD沿线土地综合开发研究。', NULL, 'wushi'),

('PM-2025-004', '蒙特雷地铁6号线延长线机电安装工程', '蒙特雷城市轨道运营有限公司', '恒通电气化工程局', '蒙特雷市',
 '2025-02-15', '2026-09-30', 'in-progress', '张文琦', '张文琦,王五',
 'participated', 'unarchived', '通风空调、给排水、消防等机电安装。', NULL, 'admin'),

-- ============ 规划中（planning） ============
('PM-2025-002', '蒙特雷都市区轨道交通车辆采购项目', '蒙特雷城市轨道运营有限公司', '松辽机车车辆制造有限公司', '长春/蒙特雷',
 '2025-03-01', '2027-06-30', 'planning', '王五', '王五,吴十',
 'not-participated', 'unarchived', '采购30列新型城轨车辆。', '待业主审批采购方案。', 'admin'),

('PM-2026-002', '蒙特雷地铁3号线延长线土建工程', '蒙特雷市交通建设管理局', '华通路桥建设集团', '蒙特雷市',
 '2026-02-25', '2028-12-31', 'planning', '吴十', '吴十',
 'not-participated', 'unarchived', '3号线延长线隧道及车站土建。', NULL, 'admin'),

('PM-2026-003', '蒙特雷地铁5号线新建项目', '墨西哥联邦交通基建署', '中盛建工集团有限公司', '蒙特雷市',
 '2026-04-15', '2029-06-30', 'planning', '王五', '王五,吴十',
 'not-participated', 'unarchived', '5号线全线可行性研究及初步设计。', '政府立项阶段。', 'admin'),

-- ============ 已完工（completed） ============
('PM-2024-002', '蒙特雷地铁6号线延长线广场改造项目', '蒙特雷市交通建设管理局', '华通路桥建设集团', '蒙特雷市',
 '2024-05-10', '2025-12-20', 'completed', '王五', '王五',
 'participated', 'archived', '沿线5个车站广场改造及景观提升。', '已竣工移交。', 'wushi'),

('PM-2025-003', '蒙特雷地铁1号线既有线改造项目', '蒙特雷城市轨道运营有限公司', '中盛建工集团有限公司', '蒙特雷市',
 '2025-05-20', '2026-05-31', 'completed', '吴十', '吴十',
 'participated', 'archived', '1号线12座车站翻新及设备更新。', '已完工。', 'admin'),

('PM-2023-003', '蒙特雷地铁4号线延长线车站装修工程', '蒙特雷城市轨道运营有限公司', '华通路桥建设集团', '蒙特雷市',
 '2023-09-10', '2025-08-31', 'completed', '王五', '王五',
 'participated', 'archived', '延长线5座车站装饰装修。', '已移交运营。', 'wushi'),

('PM-2024-006', '蒙特雷地铁6号线延长线竣工结算审计项目', '蒙特雷城市轨道运营有限公司', '汇通基础设施建设集团', '蒙特雷市',
 '2024-01-15', '2025-05-31', 'completed', '吴十', '吴十',
 'participated', 'archived', '6号线延长线一期竣工结算审计。', '已归档。', 'admin'),

-- ============ 已暂停（suspended） ============
('PM-2026-001', '蒙特雷地铁2号线车辆检修基地项目', '蒙特雷城市轨道运营有限公司', '汇通基础设施建设集团', '蒙特雷市东郊',
 '2026-01-10', '2028-06-30', 'suspended', '王五', '王五,张文琦',
 'not-participated', 'unarchived', '2号线车辆检修基地前期设计。', '初步设计评审中，暂缓推进。', 'wushi');
