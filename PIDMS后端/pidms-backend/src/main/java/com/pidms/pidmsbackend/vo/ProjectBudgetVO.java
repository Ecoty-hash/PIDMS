package com.pidms.pidmsbackend.vo;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class ProjectBudgetVO {
        private Long id;

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
         * 二级预算汇单（一期无二级明细录入，作备注性汇总）
         */
        private String secondaryBudgetSummary;

        /**
         * 审批状态
         */
        private String approvalStatus;

        /**
         * 修订状态
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
         * 最后修改人
         */
        private String updateBy;

        /**
         * 最后修改时间
         */
        private LocalDateTime updateTime;

        /**
         * 项目名称(关联带出)
         */
        private String projectName;
        private BigDecimal laborBudget;      // 人工预算
        private BigDecimal materialBudget;   // 材料预算
        private BigDecimal equipmentBudget;  // 设备预算
        private BigDecimal expenseBudget;    // 费用预算
        private BigDecimal subcontractBudget;// 分包预算
        private BigDecimal otherBudget;      // 其他预算
    }
