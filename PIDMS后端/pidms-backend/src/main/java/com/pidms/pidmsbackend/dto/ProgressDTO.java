package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 进度管理 新增 / 编辑 请求体
 * <p>项目以 projectId 提交（数据库仅存外键 project_id）；projectName 由后端联查返回展示。</p>
 */
@Data
public class ProgressDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 项目ID → pm_project.id（必填） */
    private Long projectId;

    /** 进度名称（必填） */
    private String progressName;

    /** 进度编号 */
    private String progressCode;

    /** 计划开始日期 */
    private LocalDate planStartDate;

    /** 计划结束日期 */
    private LocalDate planEndDate;

    /** 实际开始日期 */
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    private LocalDate actualEndDate;

    /** 完成百分比(0-100) */
    private BigDecimal completionPercent;

    /** 进度状态：in-progress进行中 / completed已完成 / delayed延期（新增默认 in-progress） */
    private String progressStatus;

    /** 备注 */
    private String remark;
}
