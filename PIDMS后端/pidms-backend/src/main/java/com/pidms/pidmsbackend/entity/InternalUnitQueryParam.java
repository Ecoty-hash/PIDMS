package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 内部单位 分页查询参数
 */
@Data
public class InternalUnitQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索 */
    private String keyword;

    /** unitName（高级搜索，精确匹配） */
    private String unitName;

    /** unitType（高级搜索，精确匹配） */
    private String unitType;
}
