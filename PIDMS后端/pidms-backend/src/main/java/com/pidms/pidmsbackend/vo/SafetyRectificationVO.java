package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 安全整改单 VO（列表 / 详情通用）
 * <p>projectName 为展示字段，由后端联查 pm_project 填充（表内不落库）。</p>
 */
@Data
public class SafetyRectificationVO {

    /** 主键ID */
    private Long id;

    /** 整改单号 */
    private String rectificationNo;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 项目名称（联查展示，非落库字段） */
    private String projectName;

    /** 关联检查单ID → cm_safety_inspection.id */
    private Long relatedInspectionId;

    /** 整改部位 */
    private String rectificationLocation;

    /** 要求完成日期 */
    private LocalDate requiredCompleteDate;

    /** 整改状态：pending待整改 / processing整改中 / completed已整改 */
    private String rectificationStatus;

    /** 责任人 */
    private String responsiblePerson;

    /** 整改内容 */
    private String rectificationContent;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
