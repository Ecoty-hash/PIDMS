package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 供应商 分页查询参数
 */
@Data
public class SupplierQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索 */
    private String keyword;

    /** supplierType（高级搜索，精确匹配） */
    private String supplierType;

    /** contactPerson（高级搜索，精确匹配） */
    private String contactPerson;

    /** supplierName（高级搜索，模糊匹配） */
    private String supplierName;
}
