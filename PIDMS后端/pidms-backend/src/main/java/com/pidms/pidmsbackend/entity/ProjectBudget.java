package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pm_project_budget")
public class ProjectBudget {
    @TableId(type = IdType.AUTO)

    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 预算版本
     */
    private String budgetVersion;

    /**
     * 预算总金额（六类分项之和，保存时后端汇总）
     */
    private BigDecimal budgetTotal;

    /**
     * 人工预算（未录入存 NULL）
     */
    private BigDecimal laborBudget;

    /**
     * 材料预算（未录入存 NULL）
     */
    private BigDecimal materialBudget;

    /**
     * 设备预算（未录入存 NULL）
     */
    private BigDecimal equipmentBudget;

    /**
     * 费用预算（未录入存 NULL）
     */
    private BigDecimal expenseBudget;

    /**
     * 分包预算（未录入存 NULL）
     */
    private BigDecimal subcontractBudget;

    /**
     * 其他预算（未录入存 NULL）
     */
    private BigDecimal otherBudget;

    /**
     * 二级预算汇单（一期无二级明细录入，作备注性汇总）
     */
    private String secondaryBudgetSummary;

    /**
     * 审批状态：pending / approved / rejected
     */
    private String approvalStatus;

    /**
     * 修订状态：unrevised / revising / revised
     */
    private String revisionStatus;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 抄送人
     */
    private String ccPerson;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
