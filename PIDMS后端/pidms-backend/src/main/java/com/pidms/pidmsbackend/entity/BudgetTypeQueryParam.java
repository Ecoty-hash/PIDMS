package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 预算类型 分页查询参数
 */
@Data
public class BudgetTypeQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索 */
    private String keyword;

    /** budgetType（高级搜索，精确匹配） */
    private String budgetType;

    /** secondaryBudgetTypeCode（高级搜索，精确匹配） */
    private String secondaryBudgetTypeCode;

    /** secondaryBudgetType（高级搜索，模糊匹配） */
    private String secondaryBudgetType;
}
