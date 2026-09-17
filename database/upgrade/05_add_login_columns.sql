-- =====================================================================
-- PIDMS · 登录账号升级脚本
-- 数据库：MySQL 8.x   引擎：InnoDB
--
-- 说明：sys_user 表已存在（人员管理，含种子用户），
--       但缺少「登录账号」「登录密码」两列，本脚本补齐并预置初始账号。
--       初始密码统一为 123456（BCrypt 哈希），登录后请尽快修改。
-- =====================================================================

USE `pidms`;

-- 1) sys_user 增加登录账号 + 密码列
ALTER TABLE `sys_user`
  ADD COLUMN `username` VARCHAR(50)  DEFAULT NULL COMMENT '登录账号（唯一）' AFTER `name`,
  ADD COLUMN `password` VARCHAR(100) DEFAULT NULL COMMENT '登录密码（BCrypt 哈希）' AFTER `username`;

INSERT INTO sys_user(id,name,gender,employee_no,phone,org_id) VALUES
(1,'系统管理员','男','E001','13800000001',1),
(2,'吴十','男','E002','13800000002',1),
(3,'张文琦','女','E003','13800000003',1),
(4,'王五','男','E004','13800000004',1);

UPDATE `sys_user` SET `username` = 'admin' WHERE `id` = 1;
UPDATE `sys_user` SET `username` = 'wushi' WHERE `id` = 2;
UPDATE `sys_user` SET `username` = 'zhangwenqi' WHERE `id` = 3;
UPDATE `sys_user` SET `username` = 'wangwu' WHERE `id` = 4;


-- 2) 为现有 4 个种子用户设置登录账号
UPDATE `sys_user` SET `username` = 'admin'      WHERE `id` = 1;  -- 系统管理员
UPDATE `sys_user` SET `username` = 'wushi'      WHERE `id` = 2;  -- 吴十
UPDATE `sys_user` SET `username` = 'zhangwenqi' WHERE `id` = 3;  -- 张文琦
UPDATE `sys_user` SET `username` = 'wangwu'     WHERE `id` = 4;  -- 王五

-- 3) 初始密码统一为 123456（BCrypt，$2a$10$...），须与后端 BCryptPasswordEncoder 匹配
UPDATE `sys_user`
   SET `password` = '$2a$10$uh.1yykU2A7CnJzvuDroXuQSbci1qHOGC4oYu6HvDbfLlfEGNvqLW'
 WHERE `username` IS NOT NULL;

-- 4) 登录账号加唯一索引（先赋值再建索引，避免已有 NULL 影响）
ALTER TABLE `sys_user`
  ADD UNIQUE KEY `uk_username` (`username`);

DESC sys_user;
