package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 机构管理 分页查询参数
 */
@Data
public class OrganizationQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索（机构名称 / 机构编码） */
    private String keyword;

    /** 机构名称（高级搜索，模糊匹配） */
    private String orgName;

    /** 机构编码（高级搜索，精确匹配） */
    private String orgCode;

    /** 状态：enabled启用 / disabled停用（高级搜索） */
    private String status;
}
