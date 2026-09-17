package com.pidms.pidmsbackend.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 质量检查 分页查询参数
 * <p>projectName 按项目名称过滤（后端需先匹配 pm_project 再回查本表）。</p>
 */
@Data
public class QualityInspectionQueryParam {

    /** 页码，默认1 */
    private Long page;

    /** 每页条数，默认20 */
    private Long pageSize;

    /** 项目名称（高级搜索） */
    private String projectName;

    /** 检查结果：pending待检查 / qualified合格 / unqualified不合格 */
    private String inspectionResult;

    /** 检查日期 */
    private LocalDate inspectionDate;

    /** 检查人 */
    private String inspector;
}
