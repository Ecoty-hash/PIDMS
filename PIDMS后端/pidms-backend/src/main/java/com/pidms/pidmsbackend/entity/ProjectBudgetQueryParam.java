package com.pidms.pidmsbackend.entity;

import lombok.Data;
import java.time.LocalDate;

/**
 * 项目预算分页查询参数
 */
@Data
public class ProjectBudgetQueryParam {

    /**
     * 页码，默认 1
     */
    private Integer page;

    /**
     * 每页条数，默认 20
     */
    private Integer pageSize;

    /**
     * 关键字模糊搜索
     */
    private String keyword;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 审批状态：pending待审批 / approved已审批 / rejected已拒绝
     */
    private String approvalStatus;

    /**
     * 修订状态：unrevised未修订 / revising修订中 / revised已修订
     */
    private String revisionStatus;

    /**
     * 预算版本
     */
    private String budgetVersion;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDate createTime;

}
