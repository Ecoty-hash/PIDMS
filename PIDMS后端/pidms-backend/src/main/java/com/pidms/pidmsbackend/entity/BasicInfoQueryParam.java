package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 基础资料 分页查询参数
 */
@Data
public class BasicInfoQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索 */
    private String keyword;

    /** infoCategory（高级搜索，精确匹配） */
    private String infoCategory;

    /** status（高级搜索，精确匹配） */
    private String status;
}
