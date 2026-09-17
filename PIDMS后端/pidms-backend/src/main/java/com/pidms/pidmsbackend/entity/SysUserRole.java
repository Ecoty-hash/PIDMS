package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联（人员管理的角色分配）—— 对应 sys_user_role 表。
 * <p>唯一键 (user_id, role_id)；外键对 sys_user / sys_role 均 ON DELETE CASCADE。</p>
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID → sys_user.id */
    private Long userId;

    /** 角色ID → sys_role.id */
    private Long roleId;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
