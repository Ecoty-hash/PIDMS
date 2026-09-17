package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 人员管理 分页查询参数
 */
@Data
public class PersonnelQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索（姓名 / 工号 / 电话） */
    private String keyword;

    /** 姓名（高级搜索，模糊匹配） */
    private String name;

    /** 工号（高级搜索，精确匹配） */
    private String employeeNo;

    /** 角色ID → sys_role.id（高级搜索） */
    private Long roleId;

    /** 所属机构ID → sys_org.id（高级搜索） */
    private Long orgId;

    /** 状态：enabled在职 / disabled离职（高级搜索） */
    private String status;
}
