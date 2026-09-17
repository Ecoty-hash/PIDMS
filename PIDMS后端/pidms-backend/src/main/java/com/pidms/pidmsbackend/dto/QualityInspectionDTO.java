package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 质量检查 新增 / 编辑 请求体
 * <p>项目以 projectId 提交；projectName 由后端联查返回展示。</p>
 */
@Data
public class QualityInspectionDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 检查单号（唯一，可空） */
    private String inspectionNo;

    /** 项目ID → pm_project.id（必填） */
    private Long projectId;

    /** 检查类型：quality质量检查（新增默认 quality） */
    private String inspectionType;

    /** 检查部门 */
    private String inspectionDept;

    /** 检查日期 */
    private LocalDate inspectionDate;

    /** 检查部位 */
    private String inspectionLocation;

    /** 检查人 */
    private String inspector;

    /** 检查结果：pending待检查 / qualified合格 / unqualified不合格（新增默认 pending） */
    private String inspectionResult;

    /** 检查详情 */
    private String inspectionDetail;
}
