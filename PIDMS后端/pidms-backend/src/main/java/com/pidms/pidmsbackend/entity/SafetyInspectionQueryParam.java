package com.pidms.pidmsbackend.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 安全检查 分页查询参数
 * <p>projectName 按项目名称过滤（后端需先匹配 pm_project 再回查本表）。</p>
 */
@Data
public class SafetyInspectionQueryParam {

    /** 页码，默认1 */
    private Long page;

    /** 每页条数，默认20 */
    private Long pageSize;

    /** 项目名称（高级搜索） */
    private String projectName;

    /** 检查类型 */
    private String inspectionType;

    /** 检查结果：pending待复检 / qualified合格 / unqualified不合格 */
    private String inspectionResult;

    /** 检查日期 */
    private LocalDate inspectionDate;

    /** 检查人 */
    private String inspector;
}
