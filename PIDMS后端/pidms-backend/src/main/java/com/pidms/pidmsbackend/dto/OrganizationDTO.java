package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 机构管理 新增 / 编辑 请求体
 */
@Data
public class OrganizationDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 上级机构ID → sys_org.id；顶级机构传 null */
    private Long parentOrgId;

    /** 机构名称 */
    private String orgName;

    /** 机构编码 */
    private String orgCode;

    /** 机构顺序 */
    private Integer sortOrder;

    /** 描述 */
    private String description;

    /** 状态：enabled启用 / disabled停用（不传默认 enabled） */
    private String status;
}
