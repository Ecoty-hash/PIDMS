package com.pidms.pidmsbackend.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 质量整改单 分页查询参数
 * <p>projectName 按项目名称过滤（后端需先匹配 pm_project 再回查本表）。</p>
 */
@Data
public class QualityRectificationQueryParam {

    /** 页码，默认1 */
    private Long page;

    /** 每页条数，默认20 */
    private Long pageSize;

    /** 项目名称（高级搜索） */
    private String projectName;

    /** 整改状态：pending待整改 / processing整改中 / completed已整改 */
    private String rectificationStatus;

    /** 要求完成日期 */
    private LocalDate requiredCompleteDate;

    /** 责任人 */
    private String responsiblePerson;
}
