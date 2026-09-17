package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进度管理 VO（列表 / 详情通用）
 * <p>projectName 为展示字段，由后端联查 pm_project 填充（表内不落库）。</p>
 */
@Data
public class ProgressVO {

    /** 主键ID */
    private Long id;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 项目名称（联查展示，非落库字段） */
    private String projectName;

    /** 进度名称 */
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

    /** 进度状态：in-progress进行中 / completed已完成 / delayed延期 */
    private String progressStatus;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
