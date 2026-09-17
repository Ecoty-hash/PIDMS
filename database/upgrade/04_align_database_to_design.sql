-- =====================================================================
-- PIDMS · 数据库结构对齐脚本 v2（以《数据库设计文档》/ 02_schema.sql 为准）
-- 数据库：MySQL 8.x   引擎：InnoDB   字符集：utf8mb4_general_ci
--
-- ⚠ 破坏性警告：
--   1) cm_ 施工域 11 张表将 DROP 后按设计重建，现有数据将全部清空；
--   2) 其余 18 张表执行 ALTER（补审计字段/默认值/索引/外键名对齐），数据保留；
--   3) sys_user_role / sys_role_permission 新增 id 主键列，原联合主键改唯一索引。
--
-- 执行前请务必备份：
--   mysqldump -uroot -p --single-transaction pidms > pidms_backup_$(date +%F).sql
-- =====================================================================

USE `pidms`;

-- ---------------------------------------------------------------------
-- 第一步：差异表 ALTER（保留数据）
-- ---------------------------------------------------------------------

-- 1.1 sys_attachment 补审计字段 + 修正默认值
ALTER TABLE `sys_attachment`
  MODIFY COLUMN `file_size` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  MODIFY COLUMN `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  ADD COLUMN `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建人' AFTER `upload_time`,
  ADD COLUMN `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `create_by`,
  ADD COLUMN `update_by`   VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人' AFTER `create_time`,
  ADD COLUMN `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间' AFTER `update_by`;

-- 1.2 sys_dict_data 修正默认值 + 删多余外键 + 补索引
ALTER TABLE `sys_dict_data` DROP FOREIGN KEY `fk_dict_data_type`;
ALTER TABLE `sys_dict_data`
  MODIFY COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  ADD KEY `idx_dict_type` (`dict_type`);

-- 1.3 sys_dict_type 修正 status 默认值
ALTER TABLE `sys_dict_type` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用';

-- 1.4 sys_user 表注释对齐
ALTER TABLE `sys_user` COMMENT='用户表（人员管理）';

-- 1.5 sys_operation_log 修正 create_time 默认值
ALTER TABLE `sys_operation_log` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间';

-- 1.6 sys_user_role 新增 id 主键，(user_id, role_id) 改唯一索引
ALTER TABLE `sys_user_role` DROP FOREIGN KEY `fk_user_role_user`;
ALTER TABLE `sys_user_role` DROP FOREIGN KEY `fk_user_role_role`;
ALTER TABLE `sys_user_role` DROP PRIMARY KEY;
ALTER TABLE `sys_user_role`
  ADD COLUMN `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID' FIRST,
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  ADD CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE;

-- 1.7 sys_role_permission 新增 id 主键，(role_id, permission_code) 改唯一索引
ALTER TABLE `sys_role_permission` DROP FOREIGN KEY `fk_role_perm_role`;
ALTER TABLE `sys_role_permission` DROP PRIMARY KEY;
ALTER TABLE `sys_role_permission`
  ADD COLUMN `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID' FIRST,
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_role_perm` (`role_id`, `permission_code`),
  ADD CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE;

-- 1.8 pm_project_budget_item 补审计字段 + 修正默认值 + 外键名对齐
ALTER TABLE `pm_project_budget_item` DROP FOREIGN KEY `fk_budget_item_budget`;
ALTER TABLE `pm_project_budget_item`
  MODIFY COLUMN `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  ADD COLUMN `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建人' AFTER `remark`,
  ADD COLUMN `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `create_by`,
  ADD COLUMN `update_by`   VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人' AFTER `create_time`,
  ADD COLUMN `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间' AFTER `update_by`,
  ADD CONSTRAINT `fk_budget_item` FOREIGN KEY (`budget_id`) REFERENCES `pm_project_budget` (`id`) ON DELETE CASCADE;

-- 1.9 pm_project_status 修正 status 默认值
ALTER TABLE `pm_project_status` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存';

-- 1.10 pm_seal_type 修正 status 默认值
ALTER TABLE `pm_seal_type` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存';

-- 1.11 cm_quality_check_item 修正 status 默认值
ALTER TABLE `cm_quality_check_item` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存';

-- 1.12 cm_safety_check_item 修正 status 默认值
ALTER TABLE `cm_safety_check_item` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存';

-- 1.13 pm_seal_application 外键名对齐设计
ALTER TABLE `pm_seal_application` DROP FOREIGN KEY `fk_seal_app_project`;
ALTER TABLE `pm_seal_application` DROP FOREIGN KEY `fk_seal_app_type`;
ALTER TABLE `pm_seal_application`
  ADD CONSTRAINT `fk_seal_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`),
  ADD CONSTRAINT `fk_seal_type` FOREIGN KEY (`seal_type_id`) REFERENCES `pm_seal_type` (`id`);

-- ---------------------------------------------------------------------
-- 第二步：cm_ 施工域 11 张表 DROP 后按设计重建（⚠ 数据清空）
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

-- 2.1 DROP 旧表
DROP TABLE IF EXISTS `cm_quality_rectification_item`;
DROP TABLE IF EXISTS `cm_safety_rectification_item`;
DROP TABLE IF EXISTS `cm_quality_inspection_item`;
DROP TABLE IF EXISTS `cm_safety_inspection_item`;
DROP TABLE IF EXISTS `cm_quality_rectification`;
DROP TABLE IF EXISTS `cm_safety_rectification`;
DROP TABLE IF EXISTS `cm_quality_inspection`;
DROP TABLE IF EXISTS `cm_safety_inspection`;
DROP TABLE IF EXISTS `cm_construction_log`;
DROP TABLE IF EXISTS `cm_progress`;
DROP TABLE IF EXISTS `cm_warning_rule`;

-- 2.2 按设计 CREATE 新表
CREATE TABLE `cm_progress` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id`         BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `progress_name`      VARCHAR(200) NOT NULL COMMENT '进度名称',
  `progress_code`      VARCHAR(50)  DEFAULT NULL COMMENT '进度编号',
  `plan_start_date`    DATE         DEFAULT NULL COMMENT '计划开始日期',
  `plan_end_date`      DATE         DEFAULT NULL COMMENT '计划结束日期',
  `actual_start_date`  DATE         DEFAULT NULL COMMENT '实际开始日期',
  `actual_end_date`    DATE         DEFAULT NULL COMMENT '实际结束日期',
  `completion_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '完成百分比(0-100)',
  `progress_status`    VARCHAR(20)  NOT NULL DEFAULT 'in-progress' COMMENT '进度状态：in-progress进行中/completed已完成/delayed延期',
  `remark`             VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`          VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`          VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_progress_project` (`project_id`),
  KEY `idx_progress_status` (`progress_status`),
  CONSTRAINT `fk_progress_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='进度管理表';

CREATE TABLE `cm_quality_inspection` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_no`     VARCHAR(50)  DEFAULT NULL COMMENT '检查单号',
  `project_id`        BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `inspection_type`   VARCHAR(30)  DEFAULT 'quality' COMMENT '检查类型：quality质量检查',
  `inspection_dept`   VARCHAR(50)  DEFAULT NULL COMMENT '检查部门',
  `inspection_date`   DATE         DEFAULT NULL COMMENT '检查日期',
  `inspection_location` VARCHAR(200) DEFAULT NULL COMMENT '检查部位',
  `inspector`         VARCHAR(50)  DEFAULT NULL COMMENT '检查人',
  `inspection_result` VARCHAR(20)  DEFAULT 'pending' COMMENT '检查结果：pending待检查/qualified合格/unqualified不合格',
  `inspection_detail` TEXT         COMMENT '检查详情',
  `create_by`         VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qi_no` (`inspection_no`),
  KEY `idx_qi_project` (`project_id`),
  KEY `idx_qi_date` (`inspection_date`),
  KEY `idx_qi_result` (`inspection_result`),
  CONSTRAINT `fk_qi_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质量检查表';

CREATE TABLE `cm_quality_inspection_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_id`  BIGINT UNSIGNED NOT NULL COMMENT '检查单ID',
  `check_item_id`  BIGINT UNSIGNED NOT NULL COMMENT '检查项ID',
  `check_result`   VARCHAR(20)  DEFAULT 'pending' COMMENT '检查结果：pending待检查/qualified合格/unqualified不合格',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`      VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qi_item` (`inspection_id`, `check_item_id`),
  KEY `idx_qi_item_check` (`check_item_id`),
  CONSTRAINT `fk_qii_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `cm_quality_inspection` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qii_check_item` FOREIGN KEY (`check_item_id`) REFERENCES `cm_quality_check_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质量检查明细表';

CREATE TABLE `cm_quality_rectification` (
  `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rectification_no`       VARCHAR(50)  DEFAULT NULL COMMENT '整改单号',
  `project_id`             BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `related_inspection_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '关联检查单ID',
  `rectification_location` VARCHAR(200) DEFAULT NULL COMMENT '整改部位',
  `required_complete_date` DATE         DEFAULT NULL COMMENT '要求完成日期',
  `rectification_status`   VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '整改状态：pending待整改/processing整改中/completed已整改',
  `responsible_person`     VARCHAR(50)  DEFAULT NULL COMMENT '责任人',
  `rectification_content`  TEXT         COMMENT '整改内容',
  `create_by`              VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`              VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qr_no` (`rectification_no`),
  KEY `idx_qr_project` (`project_id`),
  KEY `idx_qr_inspection` (`related_inspection_id`),
  KEY `idx_qr_status` (`rectification_status`),
  CONSTRAINT `fk_qr_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`),
  CONSTRAINT `fk_qr_inspection` FOREIGN KEY (`related_inspection_id`) REFERENCES `cm_quality_inspection` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质量整改单表';

CREATE TABLE `cm_quality_rectification_item` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rectification_id`    BIGINT UNSIGNED NOT NULL COMMENT '整改单ID',
  `check_item_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '关联检查项ID（可为空）',
  `rectification_content` VARCHAR(1000) DEFAULT NULL COMMENT '整改内容',
  `item_status`         VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '整改状态：pending待整改/processing整改中/completed已整改',
  `create_by`           VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_qri_rectification` (`rectification_id`),
  KEY `idx_qri_check_item` (`check_item_id`),
  CONSTRAINT `fk_qri_rectification` FOREIGN KEY (`rectification_id`) REFERENCES `cm_quality_rectification` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qri_check_item` FOREIGN KEY (`check_item_id`) REFERENCES `cm_quality_check_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质量整改明细表';

CREATE TABLE `cm_safety_inspection` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_no`     VARCHAR(50)  DEFAULT NULL COMMENT '检查单号',
  `project_id`        BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `inspection_type`   VARCHAR(30)  DEFAULT 'safety' COMMENT '检查类型：safety安全检查',
  `inspection_dept`   VARCHAR(50)  DEFAULT NULL COMMENT '检查部门',
  `inspection_date`   DATE         DEFAULT NULL COMMENT '检查日期',
  `inspection_location` VARCHAR(200) DEFAULT NULL COMMENT '检查部位',
  `inspector`         VARCHAR(50)  DEFAULT NULL COMMENT '检查人',
  `inspection_result` VARCHAR(20)  DEFAULT 'pending' COMMENT '检查结果：pending待复检/qualified合格/unqualified不合格',
  `inspection_detail` TEXT         COMMENT '检查详情',
  `create_by`         VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_si_no` (`inspection_no`),
  KEY `idx_si_project` (`project_id`),
  KEY `idx_si_date` (`inspection_date`),
  KEY `idx_si_result` (`inspection_result`),
  CONSTRAINT `fk_si_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全检查表';

CREATE TABLE `cm_safety_inspection_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_id`  BIGINT UNSIGNED NOT NULL COMMENT '检查单ID',
  `check_item_id`  BIGINT UNSIGNED NOT NULL COMMENT '检查项ID',
  `check_result`   VARCHAR(20)  DEFAULT 'pending' COMMENT '检查结果：pending待检查/qualified合格/unqualified不合格',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`      VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_si_item` (`inspection_id`, `check_item_id`),
  KEY `idx_si_item_check` (`check_item_id`),
  CONSTRAINT `fk_sii_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `cm_safety_inspection` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sii_check_item` FOREIGN KEY (`check_item_id`) REFERENCES `cm_safety_check_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全检查明细表';

CREATE TABLE `cm_safety_rectification` (
  `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rectification_no`       VARCHAR(50)  DEFAULT NULL COMMENT '整改单号',
  `project_id`             BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `related_inspection_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '关联检查单ID',
  `rectification_location` VARCHAR(200) DEFAULT NULL COMMENT '整改部位',
  `required_complete_date` DATE         DEFAULT NULL COMMENT '要求完成日期',
  `rectification_status`   VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '整改状态：pending待整改/processing整改中/completed已整改',
  `responsible_person`     VARCHAR(50)  DEFAULT NULL COMMENT '责任人',
  `rectification_content`  TEXT         COMMENT '整改内容',
  `create_by`              VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`              VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sr_no` (`rectification_no`),
  KEY `idx_sr_project` (`project_id`),
  KEY `idx_sr_inspection` (`related_inspection_id`),
  KEY `idx_sr_status` (`rectification_status`),
  CONSTRAINT `fk_sr_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`),
  CONSTRAINT `fk_sr_inspection` FOREIGN KEY (`related_inspection_id`) REFERENCES `cm_safety_inspection` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全整改单表';

CREATE TABLE `cm_safety_rectification_item` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rectification_id`    BIGINT UNSIGNED NOT NULL COMMENT '整改单ID',
  `check_item_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '关联检查项ID（可为空）',
  `rectification_content` VARCHAR(1000) DEFAULT NULL COMMENT '整改内容',
  `item_status`         VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '整改状态：pending待整改/processing整改中/completed已整改',
  `create_by`           VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_sri_rectification` (`rectification_id`),
  KEY `idx_sri_check_item` (`check_item_id`),
  CONSTRAINT `fk_sri_rectification` FOREIGN KEY (`rectification_id`) REFERENCES `cm_safety_rectification` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sri_check_item` FOREIGN KEY (`check_item_id`) REFERENCES `cm_safety_check_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全整改明细表';

CREATE TABLE `cm_construction_log` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id`           BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `plan_name`            VARCHAR(100) DEFAULT NULL COMMENT '对应计划名称',
  `log_date`             DATE         NOT NULL COMMENT '日志日期',
  `weather`              VARCHAR(20)  DEFAULT NULL COMMENT '天气：sunny晴/cloudy多云/rainy雨/snowy雪',
  `construction_location` VARCHAR(200) DEFAULT NULL COMMENT '施工部位',
  `construction_content` VARCHAR(500) DEFAULT NULL COMMENT '施工内容',
  `workload`             DECIMAL(8,2) DEFAULT NULL COMMENT '工作量',
  `attendance_count`     INT          DEFAULT NULL COMMENT '出勤人数',
  `construction_detail`  TEXT         COMMENT '施工详情',
  `existing_problems`    VARCHAR(1000) DEFAULT NULL COMMENT '存在问题',
  `images`               JSON         DEFAULT NULL COMMENT '图片（sys_attachment 路径数组）',
  `create_by`            VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`            VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_cl_project` (`project_id`),
  KEY `idx_cl_log_date` (`log_date`),
  CONSTRAINT `fk_cl_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='施工日志表';

CREATE TABLE `cm_warning_rule` (
  `id`                         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name`                  VARCHAR(200) NOT NULL COMMENT '规则名称',
  `project_id`                 BIGINT UNSIGNED NOT NULL COMMENT '对应项目ID',
  `plan_name`                  VARCHAR(200) DEFAULT NULL COMMENT '对应计划名称',
  `progress_percent`           DECIMAL(5,2) DEFAULT NULL COMMENT '进度百分比(0-100)',
  `trigger_time`               DATETIME     DEFAULT NULL COMMENT '触发时间',
  `progress_deviation_threshold` DECIMAL(5,2) DEFAULT NULL COMMENT '进度偏差阈值(%)',
  `warning_type`               VARCHAR(50)  DEFAULT NULL COMMENT '预警类型：进度停滞预警/质量问题预警/安全预警',
  `level_rule_config`          VARCHAR(1000) DEFAULT NULL COMMENT '分级提醒规则配置',
  `status`                     VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled禁用',
  `remark`                     VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`                  VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`                  VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_wr_project` (`project_id`),
  KEY `idx_wr_status` (`status`),
  CONSTRAINT `fk_wr_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预警规则表';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 第三步：校验（默认注释）
-- ---------------------------------------------------------------------
-- SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='pidms' ORDER BY table_name;
-- SHOW CREATE TABLE `sys_dict_data`;
