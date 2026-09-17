package com.pidms.pidmsbackend.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预算类型 VO（列表 / 详情通用）
 */
@Data
public class BudgetTypeVO {

    /** 主键ID */
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

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
