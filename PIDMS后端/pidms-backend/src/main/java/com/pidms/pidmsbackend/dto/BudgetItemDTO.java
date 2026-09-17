package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data

public class BudgetItemDTO {

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
}
