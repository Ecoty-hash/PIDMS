package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色管理 新增 / 编辑 请求体
 * <p>一期只落 sys_role 本体。permissionSettings（权限设置）与 userAssignments（用户分配）
 * 按接口文档保留在请求体中，服务端接收后暂不处理，权限/分配关系留待二期实现。</p>
 */
@Data
public class RoleDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;

    /** 角色描述 */
    private String roleDescription;

    /** 状态：enabled启用 / disabled禁用（不传默认 enabled） */
    private String status;

    /** 权限设置（预留，一期不处理） */
    private List<String> permissionSettings;

    /** 用户分配（预留，一期不处理） */
    private String userAssignments;
}
