package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 安全检查项 分页查询参数
 */
@Data
public class SafetyCheckItemQueryParam {

    /** 页码，默认1 */
    private Long page;

    /** 每页条数，默认20 */
    private Long pageSize;

    /** 关键字模糊搜索（检查项名称） */
    private String keyword;

    /** 所属分类（高级搜索） */
    private String category;

    /** 状态：enabled启用 / disabled停用 / sealed封存（高级搜索） */
    private String status;
}
