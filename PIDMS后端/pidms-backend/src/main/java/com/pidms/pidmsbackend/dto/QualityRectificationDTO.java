package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 质量整改单 新增 / 编辑 请求体
 * <p>项目以 projectId 提交；projectName 由后端联查返回展示。</p>
 */
@Data
public class QualityRectificationDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 整改单号（唯一，可空） */
    private String rectificationNo;

    /** 项目ID → pm_project.id（必填） */
    private Long projectId;

    /** 关联检查单ID → cm_quality_inspection.id（可空） */
    private Long relatedInspectionId;

    /** 整改部位 */
    private String rectificationLocation;

    /** 要求完成日期 */
    private LocalDate requiredCompleteDate;

    /** 整改状态：pending待整改 / processing整改中 / completed已整改（新增默认 pending） */
    private String rectificationStatus;

    /** 责任人 */
    private String responsiblePerson;

    /** 整改内容 */
    private String rectificationContent;
}
