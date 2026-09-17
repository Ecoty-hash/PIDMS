package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色（角色管理）—— 对应 sys_role 表。
 * <p>一期只维护角色本身；角色-权限（sys_role_permission）与用户分配（sys_user_role）
 * 的读写留待二期，详见升级脚本说明。</p>
 */
@Data
@TableName("sys_role")
public class SysRole {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色描述 */
    private String roleDescription;

    /** 状态：enabled启用 / disabled禁用 */
    private String status;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
