package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 预算类型 新增 / 编辑 请求体
 */
@Data
public class BudgetTypeDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 预算类型：labor人工费 / material材料费 / equipment设备费 / expense费用 / subcontract分包费 / other其他费用 */
    private String budgetType;

    /** 二级预算类型名称 */
    private String secondaryBudgetType;

    /** 二级预算类型编码，格式 XX-XX-XXX */
    private String secondaryBudgetTypeCode;

    /** 状态：enabled启用 / sealed封存 */
    private String status;

    /** 备注 */
    private String remark;
}
