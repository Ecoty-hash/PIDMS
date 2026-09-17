package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 公司文档 分页查询参数
 */
@Data
public class CompanyDocQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索 */
    private String keyword;

    /** docCategory（高级搜索，精确匹配） */
    private String docCategory;

    /** publishDept（高级搜索，精确匹配） */
    private String publishDept;

    /** docTitle（高级搜索，模糊匹配） */
    private String docTitle;
}
