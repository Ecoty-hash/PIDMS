-- =====================================================================
-- PIDMS 企业管理系统 - 核心域建表脚本
-- 数据库：MySQL 8.x    引擎：InnoDB    字符集：utf8mb4_general_ci
-- 覆盖：系统管理(9) + 项目管理(7) + 施工管理(13) = 29 张表
--
-- 设计约定：
--   1) 每张业务表均含公共审计字段：
--        id BIGINT UNSIGNED 自增主键
--        create_by / create_time / update_by / update_time
--   2) 枚举/字典字段统一存「code 值」(VARCHAR)，中文标签见 sys_dict_data；
--      状态取值：enabled 启用 / disabled 停用 / sealed 封存
--   3) 金额统一 DECIMAL(18,2)，百分比 DECIMAL(5,2)，日期 DATE，时间戳 DATETIME
--   4) 外键统一指向对应主表 id，外键列均已建索引
--   5) 物理外键用于保证引用完整性（本期为单体应用，无分库需求）
-- =====================================================================

USE `pidms`;

-- ---------------------------------------------------------------------
-- 一、系统管理域（sys_）
-- ---------------------------------------------------------------------

-- 1. 机构表（机构管理）
CREATE TABLE `sys_org` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '上级机构ID，顶级为NULL',
  `org_code`      VARCHAR(50)  NOT NULL COMMENT '机构编码',
  `org_name`      VARCHAR(100) NOT NULL COMMENT '机构名称',
  `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `description`   VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  `create_by`     VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_code` (`org_code`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_org_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_org` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机构表';

-- 2. 角色表（角色管理）
CREATE TABLE `sys_role` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code`       VARCHAR(50)  NOT NULL COMMENT '角色编码',
  `role_name`       VARCHAR(50)  NOT NULL COMMENT '角色名称',
  `role_description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled禁用',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 3. 用户表（人员管理）
CREATE TABLE `sys_user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`          VARCHAR(50)  NOT NULL COMMENT '姓名',
  `gender`        VARCHAR(10)  DEFAULT NULL COMMENT '性别：男 / 女',
  `employee_no`   VARCHAR(50)  NOT NULL COMMENT '工号',
  `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  `org_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '所属机构ID',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled在职 / disabled离职',
  `image`         VARCHAR(255) DEFAULT NULL COMMENT '头像/照片（sys_attachment 或 URL）',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`     VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_org_id` (`org_id`),
  CONSTRAINT `fk_user_org` FOREIGN KEY (`org_id`) REFERENCES `sys_org` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表（人员管理）';

-- 4. 用户-角色关联表
CREATE TABLE `sys_user_role` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id`     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `create_by`   VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 5. 角色-权限关联表（角色管理的权限设置）
CREATE TABLE `sys_role_permission` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id`         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限标识，如 project:list / project:all',
  `permission_name` VARCHAR(100) DEFAULT NULL COMMENT '权限名称（展示用），如 项目-查看',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_code`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限关联表';

-- 6. 字典类型表（支撑接口文档「通用枚举」）
CREATE TABLE `sys_dict_type` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type`   VARCHAR(50)  NOT NULL COMMENT '字典类型编码，如 project_status',
  `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典类型名称，如 项目状态',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- 7. 字典数据表
CREATE TABLE `sys_dict_data` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type`   VARCHAR(50)  NOT NULL COMMENT '所属字典类型编码',
  `dict_code`   VARCHAR(50)  NOT NULL COMMENT '字典项值（code，存储于业务表）',
  `dict_label`  VARCHAR(100) NOT NULL COMMENT '字典项标签（中文，展示用）',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_data` (`dict_type`, `dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- 8. 附件表（项目文档/施工日志图片/各类表单附件统一落此表）
CREATE TABLE `sys_attachment` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_type`    VARCHAR(50)  NOT NULL COMMENT '业务模块标识，如 project-doc / construction-log',
  `biz_id`      BIGINT UNSIGNED NOT NULL COMMENT '业务记录主键ID',
  `file_name`   VARCHAR(255) NOT NULL COMMENT '原文件名',
  `file_path`   VARCHAR(500) NOT NULL COMMENT '存储路径',
  `file_size`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `file_type`   VARCHAR(50)  DEFAULT NULL COMMENT '文件类型/扩展名，如 pdf / jpg',
  `upload_by`   VARCHAR(50)  DEFAULT NULL COMMENT '上传人',
  `upload_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='附件表';

-- 9. 操作日志表（审计：增删改/审批/归档/封存等关键操作留痕）
CREATE TABLE `sys_operation_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name`   VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
  `module`      VARCHAR(50)  DEFAULT NULL COMMENT '所属模块，如 seal-application',
  `action`      VARCHAR(50)  DEFAULT NULL COMMENT '操作类型：新增/编辑/删除/审批通过/驳回/归档/封存...',
  `method`      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法：GET/POST/PUT/DELETE',
  `path`        VARCHAR(200) DEFAULT NULL COMMENT '请求路径',
  `request_id`  VARCHAR(50)  DEFAULT NULL COMMENT '请求追踪ID',
  `ip`          VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
  `detail`      VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_op_module` (`module`),
  KEY `idx_op_time` (`create_time`),
  KEY `idx_op_user` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ---------------------------------------------------------------------
-- 二、项目管理域（pm_）
-- ---------------------------------------------------------------------

-- 10. 项目表（项目维护）
CREATE TABLE `pm_project` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_code`         VARCHAR(50)  DEFAULT NULL COMMENT '项目编号（批量申请编号生成，可为空）',
  `project_name`         VARCHAR(200) NOT NULL COMMENT '项目名称',
  `owner_unit`           VARCHAR(200) DEFAULT NULL COMMENT '甲方单位',
  `construction_unit`    VARCHAR(200) DEFAULT NULL COMMENT '施工单位',
  `construction_address` VARCHAR(200) DEFAULT NULL COMMENT '施工地址',
  `start_date`           DATE         DEFAULT NULL COMMENT '开工日期',
  `completion_date`      DATE         DEFAULT NULL COMMENT '竣工日期',
  `project_status`       VARCHAR(30)  NOT NULL DEFAULT 'in-progress' COMMENT '项目状态：planning规划中/in-progress在建/completed已完工/suspended已暂停',
  `project_leader`       VARCHAR(50)  DEFAULT NULL COMMENT '项目负责人',
  `project_members`      VARCHAR(500) DEFAULT NULL COMMENT '项目成员（多人逗号分隔）',
  `participation_status` VARCHAR(20)  DEFAULT 'not-participated' COMMENT '参与状态：participated参与 / not-participated未参与',
  `archive_status`       VARCHAR(20)  NOT NULL DEFAULT 'unarchived' COMMENT '归档状态：archived已归档 / unarchived未归档',
  `project_overview`     TEXT         COMMENT '项目概况',
  `remark`               VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`            VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`            VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_project_status` (`project_status`),
  KEY `idx_archive_status` (`archive_status`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_completion_date` (`completion_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目表';

-- 11. 项目预算表（一期：一条项目预算 = 一行，六类分项金额直接存主表；分项未录入存 NULL，不启用一主多子）
CREATE TABLE `pm_project_budget` (
  `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id`              BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `budget_version`          VARCHAR(20)  DEFAULT 'V1.0' COMMENT '预算版本',
  `budget_total`            DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '预算总额（六类分项之和，保存时后端汇总）',
  `labor_budget`            DECIMAL(18,2) DEFAULT NULL COMMENT '人工预算（未录入存 NULL）',
  `material_budget`         DECIMAL(18,2) DEFAULT NULL COMMENT '材料预算（未录入存 NULL）',
  `equipment_budget`        DECIMAL(18,2) DEFAULT NULL COMMENT '设备预算（未录入存 NULL）',
  `expense_budget`          DECIMAL(18,2) DEFAULT NULL COMMENT '费用预算（未录入存 NULL）',
  `subcontract_budget`      DECIMAL(18,2) DEFAULT NULL COMMENT '分包预算（未录入存 NULL）',
  `other_budget`            DECIMAL(18,2) DEFAULT NULL COMMENT '其他预算（未录入存 NULL）',
  `secondary_budget_summary` VARCHAR(500) DEFAULT NULL COMMENT '二级预算汇单（一期无二级明细录入，作备注性汇总）',
  `approval_status`         VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending待审批/approved已审批/rejected已拒绝',
  `revision_status`         VARCHAR(20)  NOT NULL DEFAULT 'unrevised' COMMENT '修订状态：unrevised未修订/revising修订中/revised已修订',
  `approver`                VARCHAR(50)  DEFAULT NULL COMMENT '审批人',
  `cc_person`               VARCHAR(200) DEFAULT NULL COMMENT '抄送人（多人逗号分隔）',
  `remark`                  VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`               VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`               VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_budget_project` (`project_id`),
  KEY `idx_budget_approval` (`approval_status`),
  CONSTRAINT `fk_budget_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目预算表';

-- 12. 项目预算明细表（一期预留：业务不读写，结构保留待二期「多行明细录入」启用一主多子模型）
CREATE TABLE `pm_project_budget_item` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `budget_id`       BIGINT UNSIGNED NOT NULL COMMENT '预算ID',
  `budget_category` VARCHAR(30)  NOT NULL COMMENT '预算类别：labor人工/material材料/equipment设备/expense费用/subcontract分包/other其他',
  `budget_type_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '预留：关联基础管理-预算类型表，二期启用',
  `amount`          DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_category` (`budget_id`, `budget_category`),
  CONSTRAINT `fk_budget_item` FOREIGN KEY (`budget_id`) REFERENCES `pm_project_budget` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目预算明细表（一期预留不读写，二期启用）';

-- 13. 项目文档表
CREATE TABLE `pm_project_doc` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '所属项目ID',
  `doc_name`        VARCHAR(200) DEFAULT NULL COMMENT '文档名称',
  `doc_category`    VARCHAR(50)  DEFAULT NULL COMMENT '文档分类：施工图纸/技术资料/验收资料',
  `doc_type`        VARCHAR(50)  DEFAULT NULL COMMENT '文档类型：施工图纸/施工方案/验收报告',
  `version`         VARCHAR(20)  DEFAULT NULL COMMENT '版本号',
  `doc_description` VARCHAR(1000) DEFAULT NULL COMMENT '文档说明',
  `upload_by`       VARCHAR(50)  DEFAULT NULL COMMENT '上传人',
  `upload_time`     DATETIME     DEFAULT NULL COMMENT '上传时间',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_project` (`project_id`),
  KEY `idx_doc_category` (`doc_category`),
  CONSTRAINT `fk_doc_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目文档表';

-- 14. 项目状态定义表（项目状态模块，可自定义状态字典）
CREATE TABLE `pm_project_status` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `status_name` VARCHAR(50) NOT NULL COMMENT '项目状态名称',
  `status_code` VARCHAR(50) DEFAULT NULL COMMENT '状态编码',
  `status`      VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存',
  `create_by`   VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(50) DEFAULT NULL COMMENT '最后修改人',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_ps_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目状态定义表';

-- 15. 印章类型表
CREATE TABLE `pm_seal_type` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_name`   VARCHAR(50) NOT NULL COMMENT '印章类型名称',
  `status`      VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存',
  `create_by`   VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(50) DEFAULT NULL COMMENT '最后修改人',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_seal_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='印章类型表';

-- 16. 用印申请表
CREATE TABLE `pm_seal_application` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no`          VARCHAR(50)  DEFAULT NULL COMMENT '单号',
  `title`            VARCHAR(200) NOT NULL COMMENT '标题',
  `project_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '项目ID',
  `applicant`        VARCHAR(50)  DEFAULT NULL COMMENT '申请人',
  `apply_date`       DATE         DEFAULT NULL COMMENT '申请日期',
  `seal_department`  VARCHAR(50)  DEFAULT NULL COMMENT '用印部门',
  `seal_type_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '印章类型ID',
  `seal_file_name`   VARCHAR(200) DEFAULT NULL COMMENT '用印文件名称',
  `file_copies`      INT          NOT NULL DEFAULT 1 COMMENT '文件份数',
  `seal_method`      VARCHAR(20)  DEFAULT NULL COMMENT '用印方式：原件盖章 / 复印件盖章',
  `seal_description` VARCHAR(1000) DEFAULT NULL COMMENT '用印说明',
  `approval_status`  VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending待审批/approved已审批/rejected已拒绝',
  `approver`         VARCHAR(50)  DEFAULT NULL COMMENT '审批人',
  `remark`           VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`        VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seal_bill_no` (`bill_no`),
  KEY `idx_seal_project` (`project_id`),
  KEY `idx_seal_type` (`seal_type_id`),
  KEY `idx_seal_approval` (`approval_status`),
  CONSTRAINT `fk_seal_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`),
  CONSTRAINT `fk_seal_type` FOREIGN KEY (`seal_type_id`) REFERENCES `pm_seal_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用印申请表';

-- ---------------------------------------------------------------------
-- 三、施工管理域（cm_）
-- ---------------------------------------------------------------------

-- 17. 进度管理表
CREATE TABLE `cm_progress` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id`         BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `parent_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '父节点ID → cm_progress.id，NULL为顶层节点（支持多层子节点）',
  `progress_name`      VARCHAR(200) NOT NULL COMMENT '进度名称',
  `progress_code`      VARCHAR(50)  DEFAULT NULL COMMENT '进度编号',
  `plan_start_date`    DATE         DEFAULT NULL COMMENT '计划开始日期',
  `plan_end_date`      DATE         DEFAULT NULL COMMENT '计划结束日期',
  `actual_start_date`  DATE         DEFAULT NULL COMMENT '实际开始日期',
  `actual_end_date`    DATE         DEFAULT NULL COMMENT '实际结束日期',
  `completion_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '完成百分比(0-100)',
  `weight`             DECIMAL(5,2) DEFAULT NULL COMMENT '占总进度百分比(0-100)，仅叶子节点填写，父节点由子节点汇总',
  `progress_status`    VARCHAR(20)  NOT NULL DEFAULT 'in-progress' COMMENT '进度状态：in-progress进行中/completed已完成/delayed延期',
  `responsible_person` VARCHAR(50)  DEFAULT NULL COMMENT '节点负责人（真实姓名）',
  `remark`             VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`          VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`          VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_progress_project` (`project_id`),
  KEY `idx_progress_parent` (`parent_id`),
  KEY `idx_progress_status` (`progress_status`),
  CONSTRAINT `fk_progress_project` FOREIGN KEY (`project_id`) REFERENCES `pm_project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='进度管理表';

-- 18. 质量检查项表
CREATE TABLE `cm_quality_check_item` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category`        VARCHAR(50)  DEFAULT NULL COMMENT '所属分类：结构工程/装饰工程/安装工程/防水工程',
  `check_item_name` VARCHAR(200) NOT NULL COMMENT '检查项名称',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_qci_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质量检查项表';

-- 19. 质量检查表
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

-- 20. 质量检查明细表（一次检查对应多个检查项）
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

-- 21. 质量整改单表
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

-- 22. 质量整改明细表
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

-- 23. 安全检查项表
CREATE TABLE `cm_safety_check_item` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category`        VARCHAR(50)  DEFAULT NULL COMMENT '所属分类：用电安全/高空作业/消防安全/机械安全',
  `check_item_name` VARCHAR(200) NOT NULL COMMENT '检查项名称',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled启用 / disabled停用 / sealed封存',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '最后修改人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_sci_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全检查项表';

-- 24. 安全检查表
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

-- 25. 安全检查明细表
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

-- 26. 安全整改单表
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

-- 27. 安全整改明细表
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

-- 28. 施工日志表
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

-- 29. 预警规则表
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
