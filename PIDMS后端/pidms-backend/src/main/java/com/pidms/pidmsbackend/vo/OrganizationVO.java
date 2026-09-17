package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 机构管理 VO（列表 / 详情 / 机构树 通用）
 */
@Data
public class OrganizationVO {

    /** 主键ID */
    private Long id;

    /** 上级机构ID → sys_org.id；顶级机构为 null */
    private Long parentOrgId;

    /** 上级机构名称（联查 sys_org 回填，非落库字段） */
    private String parentOrgName;

    /** 机构名称 */
    private String orgName;

    /** 机构编码 */
    private String orgCode;

    /** 机构顺序 */
    private Integer sortOrder;

    /** 描述 */
    private String description;

    /** 状态：enabled启用 / disabled停用 */
    private String status;

    /** 下级机构（仅 GET /api/organizations/tree 返回时填充） */
    private List<OrganizationVO> children;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
