package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理 VO（列表 / 详情通用）
 */
@Data
public class RoleVO {

    /** 主键ID */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;

    /** 角色描述 */
    private String roleDescription;

    /** 状态：enabled启用 / disabled禁用 */
    private String status;

    /** 权限列表（预留：二期接入 sys_role_permission，当前恒为 null） */
    private String permissions;

    /** 用户分配（预留：二期接入 sys_user_role，当前恒为 null） */
    private String userAssignments;

    /** 权限设置（预留：二期接入 sys_role_permission，当前恒为空数组） */
    private List<String> permissionSettings;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
