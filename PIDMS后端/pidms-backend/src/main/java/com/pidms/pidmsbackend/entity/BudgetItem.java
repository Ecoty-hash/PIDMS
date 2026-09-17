package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目预算明细实体（对应表 pm_project_budget_item）
 */
@Data
@TableName("pm_project_budget_item")
public class BudgetItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属预算ID
     */
    private Long budgetId;

    /**
     * 预算类别：labor人工 / material材料 / equipment设备 / expense费用 / subcontract分包 / other其他
     */
    private String budgetCategory;

    /**
     * 预留：二级预算类型ID（二期启用）
     */
    private Long budgetTypeId;

    /**
     * 金额
     */
    private BigDecimal amount;

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
