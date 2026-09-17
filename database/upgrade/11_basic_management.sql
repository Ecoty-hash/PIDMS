-- =====================================================================
-- PIDMS · 11 基础管理域建表脚本
-- 数据库：MySQL 8.x    引擎：InnoDB    字符集：utf8mb4_general_ci
--
-- 背景：init/02_schema.sql 覆盖「系统管理(9) + 项目管理(7) + 施工管理(13) = 29 张表」，
--       基础管理域 10 个模块（客户/供应商/内部单位/工种类型/预算类型/基础资料/
--       公司文档/出差申请/请假申请/补卡申请）此前没有对应的表。
--       本脚本按《数据库设计文档》§7「后续演进」规划的表名增量补齐，
--       不修改已发布的 init/02_schema.sql。
--
-- 设计约定（与 02_schema.sql 完全一致）：
--   1) 公共审计字段：id BIGINT UNSIGNED 自增主键 +
--      create_by / create_time / update_by / update_time
--   2) 枚举/字典字段统一存「英文 code」(VARCHAR)，中文标签见 sys_dict_data；
--      状态取值沿用 enabled 启用 / disabled 停用 / sealed 封存
--   3) 金额 DECIMAL(18,2)，天数 DECIMAL(5,1)，日期 DATE，时间戳 DATETIME
--   4) 关联字段统一落外键 ID（方向A）：出差申请挂 pm_project.project_id，
--      展示用的名称由后端联查回填，表中不落冗余名称列
--   5) 物理外键保证引用完整性（单体应用，无分库需求）
--
-- 幂等：全部使用 CREATE TABLE IF NOT EXISTS，可重复执行。
-- 执行：在 Navicat / IDEA Database / mysql 命令行对 pidms 库执行一次。新库同样适用。
-- =====================================================================

USE `pidms`;

-- ---------------------------------------------------------------------
-- 一、字典/档案类（无外部依赖）
-- ---------------------------------------------------------------------

-- 1. 基础资料表（基础资料：材料/设备/工种分类字典）
CREATE TABLE IF NOT EXISTS `bas_basic_info` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `info_code`       VARCHAR(50)  NOT NULL COMMENT '资料编号',
  `info_name`       VARCHAR(100) NOT NULL COMMENT '资料名称',
  `info_category`   VARCHAR(30)  NOT NULL COMMENT '分类：material材料分类 / equipment设备分类 / worktype工种分类',
  `specification`   VARCHAR(200) DEFAULT NULL COMMENT '规格型号',
  `unit`            VARCHAR(20)  DEFAULT NULL COMMENT '单位',
  `reference_price` DECIMAL(18,2) DEFAULT NULL COMMENT '参考单价',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active在用 / archived已归档',
  `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basic_info_code` (`info_code`),
  KEY `idx_basic_info_category` (`info_category`),
  KEY `idx_basic_info_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='基础资料表';

-- 2. 客户表
CREATE TABLE IF NOT EXISTS `bas_customer` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_code`  VARCHAR(50)  DEFAULT NULL COMMENT '客户编号',
  `customer_name`  VARCHAR(100) NOT NULL COMMENT '客户名称',
  `customer_type`  VARCHAR(30)  NOT NULL COMMENT '客户类型：government政府单位 / enterprise企业客户 / state-owned国企客户',
  `contact_person` VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  `contact_phone`  VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `address`        VARCHAR(200) DEFAULT NULL COMMENT '地址',
  `office_address` VARCHAR(200) DEFAULT NULL COMMENT '办公地址',
  `invoice_title`  VARCHAR(200) DEFAULT NULL COMMENT '发票抬头',
  `tax_no`         VARCHAR(50)  DEFAULT NULL COMMENT '税号',
  `phone`          VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  `bank_name`      VARCHAR(100) DEFAULT NULL COMMENT '开户行',
  `bank_account`   VARCHAR(50)  DEFAULT NULL COMMENT '银行卡号',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `image`          VARCHAR(255) DEFAULT NULL COMMENT '图片（文件名/URL）',
  `attachments`    VARCHAR(500) DEFAULT NULL COMMENT '附件（文件名，多个以顿号分隔）',
  `create_by`      VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_code` (`customer_code`),
  KEY `idx_customer_name` (`customer_name`),
  KEY `idx_customer_type` (`customer_type`),
  KEY `idx_customer_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户表';

-- 3. 供应商表
CREATE TABLE IF NOT EXISTS `bas_supplier` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_code`   VARCHAR(50)  DEFAULT NULL COMMENT '供应商编号',
  `supplier_name`   VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `supplier_type`   VARCHAR(30)  NOT NULL COMMENT '供应商类型：material材料供应商 / equipment设备供应商 / labor劳务分包',
  `contact_person`  VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  `contact_phone`   VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `contact_address` VARCHAR(200) DEFAULT NULL COMMENT '联系地址',
  `office_address`  VARCHAR(200) DEFAULT NULL COMMENT '办公地址',
  `quoter`          VARCHAR(50)  DEFAULT NULL COMMENT '报价人',
  `quoter_phone`    VARCHAR(20)  DEFAULT NULL COMMENT '报价人手机',
  `account_name`    VARCHAR(100) DEFAULT NULL COMMENT '户名',
  `bank_name`       VARCHAR(100) DEFAULT NULL COMMENT '开户行',
  `bank_account`    VARCHAR(50)  DEFAULT NULL COMMENT '银行卡号',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `image`           VARCHAR(255) DEFAULT NULL COMMENT '图片（文件名/URL）',
  `attachments`     VARCHAR(500) DEFAULT NULL COMMENT '附件（文件名，多个以顿号分隔）',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`),
  KEY `idx_supplier_name` (`supplier_name`),
  KEY `idx_supplier_type` (`supplier_type`),
  KEY `idx_supplier_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商表';

-- 4. 内部单位表
CREATE TABLE IF NOT EXISTS `bas_internal_unit` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `unit_code`      VARCHAR(50)  DEFAULT NULL COMMENT '单位编号',
  `unit_name`      VARCHAR(100) NOT NULL COMMENT '单位名称',
  `unit_type`      VARCHAR(30)  DEFAULT NULL COMMENT '单位类型：branch分公司 / project项目部 / department部门',
  `manager`        VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
  `contact_person` VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  `contact_phone`  VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `office_address` VARCHAR(200) DEFAULT NULL COMMENT '办公地址',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `image`          VARCHAR(255) DEFAULT NULL COMMENT '图片（文件名/URL）',
  `attachments`    VARCHAR(500) DEFAULT NULL COMMENT '附件（文件名，多个以顿号分隔）',
  `create_by`      VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_internal_unit_code` (`unit_code`),
  KEY `idx_internal_unit_name` (`unit_name`),
  KEY `idx_internal_unit_type` (`unit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='内部单位表';

-- 5. 工种类型表
CREATE TABLE IF NOT EXISTS `bas_work_type` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_name`   VARCHAR(50) NOT NULL COMMENT '工种类型名称',
  `status`      VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存',
  `create_by`   VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(50) DEFAULT NULL COMMENT '最后修改人',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_type_name` (`type_name`),
  KEY `idx_work_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工种类型表';

-- 6. 预算类型表（维护预算类型及二级预算类型字典）
--    说明：budget_type 取值与 pm_project_budget 的六类分项金额字段一一对应，
--          用于「项目预算」录入时选择费用类别。
CREATE TABLE IF NOT EXISTS `bas_budget_type` (
  `id`                         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `budget_type`                VARCHAR(30)  NOT NULL COMMENT '预算类型：labor人工费 / material材料费 / equipment设备费 / expense费用 / subcontract分包费 / other其他费用',
  `secondary_budget_type`      VARCHAR(100) NOT NULL COMMENT '二级预算类型名称',
  `secondary_budget_type_code` VARCHAR(50)  NOT NULL COMMENT '二级预算类型编码，格式 XX-XX-XXX，如 RGF-GZ-001',
  `status`                     VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / sealed封存',
  `remark`                     VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`                  VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`                  VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_type_code` (`secondary_budget_type_code`),
  KEY `idx_budget_type` (`budget_type`),
  KEY `idx_budget_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预算类型表';

-- 7. 公司文档表
CREATE TABLE IF NOT EXISTS `bas_company_doc` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_code`        VARCHAR(50)  NOT NULL COMMENT '文档编号',
  `doc_name`        VARCHAR(200) NOT NULL COMMENT '文档名称（列表展示，未填时由后端取文档标题回填）',
  `doc_title`       VARCHAR(200) DEFAULT NULL COMMENT '文档标题',
  `doc_category`    VARCHAR(30)  NOT NULL COMMENT '文档分类：regulation公司制度 / specification技术规范 / management管理文件',
  `version`         VARCHAR(20)  DEFAULT NULL COMMENT '版本号',
  `publish_dept`    VARCHAR(100) DEFAULT NULL COMMENT '发布部门',
  `publish_date`    DATE         DEFAULT NULL COMMENT '发布日期',
  `doc_description` VARCHAR(1000) DEFAULT NULL COMMENT '文档说明',
  `upload_by`       VARCHAR(50)  DEFAULT NULL COMMENT '上传人',
  `upload_time`     DATETIME     DEFAULT NULL COMMENT '上传时间',
  `attachments`     VARCHAR(500) DEFAULT NULL COMMENT '附件（文件名，多个以顿号分隔）',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_doc_code` (`doc_code`),
  KEY `idx_company_doc_category` (`doc_category`),
  KEY `idx_company_doc_publish_date` (`publish_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司文档表';

-- ---------------------------------------------------------------------
-- 二、流程申请类（出差 / 请假 / 补卡，共用 approval_status 审批流转）
--    审批状态：pending未审批 / approved已通过 / rejected已驳回
-- ---------------------------------------------------------------------

-- 8. 出差申请表
CREATE TABLE IF NOT EXISTS `bas_business_trip` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `apply_no`         VARCHAR(50)  DEFAULT NULL COMMENT '申请单号',
  `project_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '项目ID → pm_project.id',
  `applicant`        VARCHAR(50)  DEFAULT NULL COMMENT '申请人',
  `trip_destination` VARCHAR(200) DEFAULT NULL COMMENT '出差地点',
  `start_date`       DATE         DEFAULT NULL COMMENT '开始日期',
  `end_date`         DATE         DEFAULT NULL COMMENT '结束日期',
  `trip_reason`      VARCHAR(1000) DEFAULT NULL COMMENT '出差事由',
  `attachments`      VARCHAR(500) DEFAULT NULL COMMENT '附件（文件名，多个以顿号分隔）',
  `apply_time`       DATETIME     DEFAULT NULL COMMENT '申请时间',
  `approval_status`  VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending未审批 / approved已通过 / rejected已驳回',
  `approver`         VARCHAR(50)  DEFAULT NULL COMMENT '审批人',
  `approval_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time`    DATETIME     DEFAULT NULL COMMENT '审批时间',
  `create_by`        VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_trip_apply_no` (`apply_no`),
  KEY `idx_business_trip_project` (`project_id`),
  KEY `idx_business_trip_applicant` (`applicant`),
  KEY `idx_business_trip_approval` (`approval_status`),
  CONSTRAINT `fk_business_trip_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出差申请表';

-- 9. 请假申请表
CREATE TABLE IF NOT EXISTS `bas_leave_application` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `apply_no`         VARCHAR(50)  DEFAULT NULL COMMENT '申请单号',
  `applicant`        VARCHAR(50)  DEFAULT NULL COMMENT '申请人',
  `leave_type`       VARCHAR(30)  DEFAULT NULL COMMENT '请假类型：annual年假 / personal事假 / sick病假 / marriage婚假 / maternity产假 / compensatory调休',
  `start_date`       DATE         DEFAULT NULL COMMENT '开始日期',
  `end_date`         DATE         DEFAULT NULL COMMENT '结束日期',
  `leave_days`       DECIMAL(5,1) DEFAULT NULL COMMENT '请假天数（由后端按起止日期重算，忽略客户端传值）',
  `leave_reason`     VARCHAR(1000) DEFAULT NULL COMMENT '请假原因',
  `attachments`      VARCHAR(500) DEFAULT NULL COMMENT '附件（文件名，多个以顿号分隔）',
  `apply_time`       DATETIME     DEFAULT NULL COMMENT '申请时间',
  `approval_status`  VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending未审批 / approved已通过 / rejected已驳回',
  `approver`         VARCHAR(50)  DEFAULT NULL COMMENT '审批人',
  `approval_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time`    DATETIME     DEFAULT NULL COMMENT '审批时间',
  `create_by`        VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leave_application_apply_no` (`apply_no`),
  KEY `idx_leave_application_applicant` (`applicant`),
  KEY `idx_leave_application_type` (`leave_type`),
  KEY `idx_leave_application_approval` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='请假申请表';

-- 10. 补卡申请表
CREATE TABLE IF NOT EXISTS `bas_makeup_application` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `apply_no`         VARCHAR(50)  DEFAULT NULL COMMENT '申请单号',
  `applicant`        VARCHAR(50)  DEFAULT NULL COMMENT '申请人',
  `card_miss_date`   DATE         DEFAULT NULL COMMENT '缺卡日期',
  `card_miss_type`   VARCHAR(30)  DEFAULT NULL COMMENT '缺卡类型：on-duty上班 / off-duty下班 / whole-day全天',
  `card_miss_time`   VARCHAR(50)  DEFAULT NULL COMMENT '缺卡时间',
  `makeup_reason`    VARCHAR(1000) DEFAULT NULL COMMENT '补卡原因',
  `witness`          VARCHAR(50)  DEFAULT NULL COMMENT '证明人',
  `apply_time`       DATETIME     DEFAULT NULL COMMENT '申请时间',
  `approval_status`  VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending未审批 / approved已通过 / rejected已驳回',
  `approver`         VARCHAR(50)  DEFAULT NULL COMMENT '审批人',
  `approval_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time`    DATETIME     DEFAULT NULL COMMENT '审批时间',
  `create_by`        VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_makeup_application_apply_no` (`apply_no`),
  KEY `idx_makeup_application_applicant` (`applicant`),
  KEY `idx_makeup_application_miss_date` (`card_miss_date`),
  KEY `idx_makeup_application_approval` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='补卡申请表';

-- ---------------------------------------------------------------------
-- 三、存量数据归一：sys_user.gender 中文码 → 英文 code
--    「人员管理」模块按方向A 统一存英文 code（male男 / female女），
--    而 03_seed.sql 早期种子的 gender 存的是中文「男」「女」。
--    本段把存量行归位，并同步 sys_dict_data 的 gender 字典项 code。
--    幂等：可重复执行。
-- ---------------------------------------------------------------------
UPDATE `sys_user` SET `gender` = 'male'   WHERE `gender` = '男';
UPDATE `sys_user` SET `gender` = 'female' WHERE `gender` = '女';

UPDATE `sys_dict_data` SET `dict_code` = 'male'   WHERE `dict_type` = 'gender' AND `dict_code` = '男';
UPDATE `sys_dict_data` SET `dict_code` = 'female' WHERE `dict_type` = 'gender' AND `dict_code` = '女';

-- ---------------------------------------------------------------------
-- 四、回执：确认建表结果
-- ---------------------------------------------------------------------
SELECT table_name AS 表名, table_comment AS 说明
FROM information_schema.tables
WHERE table_schema = 'pidms' AND table_name LIKE 'bas\_%'
ORDER BY table_name;
