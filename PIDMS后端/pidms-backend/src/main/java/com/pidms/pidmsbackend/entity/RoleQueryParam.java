package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 角色管理 分页查询参数
 */
@Data
public class RoleQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索（角色名称 / 角色编码） */
    private String keyword;

    /** 角色名称（高级搜索，模糊匹配） */
    private String roleName;

    /** 角色编码（高级搜索，精确匹配） */
    private String roleCode;

    /** 状态：enabled启用 / disabled禁用（高级搜索） */
    private String status;
}
