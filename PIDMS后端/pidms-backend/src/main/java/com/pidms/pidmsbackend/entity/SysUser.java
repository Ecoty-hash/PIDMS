package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（人员管理）—— 对应 sys_user 表。
 * 登录用：username / password / name / status；角色从 sys_user_role 关联查询。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 登录密码（BCrypt 哈希） */
    private String password;

    /** 姓名 */
    private String name;

    /** 性别：male男 / female女 */
    private String gender;

    private String employeeNo;

    private String phone;

    /** 所属机构ID → sys_org.id */
    private Long orgId;

    /** enabled 在职 / disabled 离职 */
    private String status;

    private String image;

    private String remark;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
