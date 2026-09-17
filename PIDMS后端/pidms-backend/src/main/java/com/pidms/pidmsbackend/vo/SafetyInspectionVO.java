package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 安全检查 VO（列表 / 详情通用）
 * <p>projectName 为展示字段，由后端联查 pm_project 填充（表内不落库）。</p>
 */
@Data
public class SafetyInspectionVO {

    /** 主键ID */
    private Long id;

    /** 检查单号 */
    private String inspectionNo;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 项目名称（联查展示，非落库字段） */
    private String projectName;

    /** 检查类型 */
    private String inspectionType;

    /** 检查部门 */
    private String inspectionDept;

    /** 检查日期 */
    private LocalDate inspectionDate;

    /** 检查部位 */
    private String inspectionLocation;

    /** 检查人 */
    private String inspector;

    /** 检查结果：pending待复检 / qualified合格 / unqualified不合格 */
    private String inspectionResult;

    /** 检查详情 */
    private String inspectionDetail;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
