package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预算类型实体 bas_budget_type
 */
@Data
@TableName("bas_budget_type")
public class BudgetType {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
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
