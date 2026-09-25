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

    /** 父节点ID；NULL 为顶层节点 */
    private Long parentId;

    /** 上级节点名称（联查展示，非落库字段） */
    private String parentName;

    /** 直接子节点个数（列表页据此渲染展开箭头） */
    private Integer childCount;

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

    /** 完成百分比(0-100)：叶子取库值，父节点为子节点加权汇总值（Service 填充） */
    private BigDecimal completionPercent;

    /** 完成度是否为子节点自动汇总（父节点 true，页面上该字段只读） */
    private Boolean completionAuto;

    /** 手填的占总进度百分比(0-100)，未填为 null */
    private BigDecimal weight;

    /** 有效权重：手填值，或「同组平分剩余份额」的结果；父节点为子节点汇总 */
    private BigDecimal effectiveWeight;

    /** 有效权重是否为自动得出（未填被平分、或由子节点汇总） */
    private Boolean weightAuto;

    /** 期望值（理论进度）：按计划工期线性推进，无量时线为 null */
    private BigDecimal expectedPercent;

    /** 是否已完成（含父节点由子节点汇总判定） */
    private Boolean completed;

    /** 是否逾期 */
    private Boolean overdue;

    /** 逾期天数（未逾期为 null） */
    private Long overdueDays;

    /** 进度状态：in-progress进行中 / completed已完成 / delayed延期 */
    private String progressStatus;

    /** 节点负责人（真实姓名），可为空 */
    private String responsiblePerson;

    /** 备注（页面上的「节点描述」） */
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
