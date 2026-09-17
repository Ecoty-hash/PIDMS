package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目预算新增 / 编辑请求体（一期平面预算）
 * <p>
 * 一期采用「单行平面预算」：一条项目预算对应主表一行，六大类分项金额直接提交到主表；
 * 分项未录入时传 null（落库为 NULL）；预算总额 budget_total 由后端在保存时按六类求和，
 * 客户端无需计算，即使传值后端也会忽略并重算。
 */
@Data
public class ProjectBudgetDTO {

    /**
     * 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传）
     */
    private Long id;

    /**
     * 项目名称（必填，后端据此解析 project_id）
     */
    private String projectName;

    /**
     * 预算版本，默认 V1.0
     */
    private String budgetVersion;

    /**
     * 人工预算（未录入传 null）
     */
    private BigDecimal laborBudget;

    /**
     * 材料预算（未录入传 null）
     */
    private BigDecimal materialBudget;

    /**
     * 设备预算（未录入传 null）
     */
    private BigDecimal equipmentBudget;

    /**
     * 费用预算（未录入传 null）
     */
    private BigDecimal expenseBudget;

    /**
     * 分包预算（未录入传 null）
     */
    private BigDecimal subcontractBudget;

    /**
     * 其他预算（未录入传 null）
     */
    private BigDecimal otherBudget;

    /**
     * 二级预算汇单（备注性汇总，一期无二级明细录入）
     */
    private String secondaryBudgetSummary;

    /**
     * 审批状态：pending/approved/rejected（新增默认 pending）
     */
    private String approvalStatus;

    /**
     * 修订状态：unrevised/revising/revised（新增默认 unrevised）
     */
    private String revisionStatus;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 抄送人（多人逗号分隔）
     */
    private String ccPerson;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人（可选；不传则由后端上下文/审计默认填充）
     */
    private String createBy;
}
