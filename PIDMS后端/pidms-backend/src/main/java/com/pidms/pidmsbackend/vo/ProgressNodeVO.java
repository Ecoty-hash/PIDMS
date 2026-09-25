package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 进度节点 VO（项目进度详情页的节点列表 / 甘特图数据源，父子嵌套）
 * <p>completed / overdue / overdueDays / completionPercent / expectedPercent / 权重
 * 一律由 ProjectProgressCalculator 统一判定，不在前端另算一份。</p>
 */
@Data
public class ProgressNodeVO {

    /** 主键ID */
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 父节点ID；NULL 为顶层节点 */
    private Long parentId;

    /** 层级深度（顶层为 0，用于前端缩进；后端算好，前端不再递归推导） */
    private Integer level;

    /** 子节点（多层嵌套；无子节点为空数组） */
    private List<ProgressNodeVO> children = new ArrayList<>();

    /** 直接子节点个数 */
    private Integer childCount;

    /** 节点名称 */
    private String progressName;

    /** 节点编号 */
    private String progressCode;

    /** 节点负责人（真实姓名），可为空 */
    private String responsiblePerson;

    /** 计划开始日期 */
    private LocalDate planStartDate;

    /** 计划结束日期 */
    private LocalDate planEndDate;

    /** 实际开始日期 */
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    private LocalDate actualEndDate;

    /** 完成百分比(0-100)：叶子取手填值，父节点为子节点加权汇总值 */
    private BigDecimal completionPercent;

    /** 完成度是否为子节点自动汇总（父节点 true，弹窗里该字段只读） */
    private Boolean completionAuto;

    /** 手填的占总进度百分比(0-100)，未填为 null（弹窗回显用） */
    private BigDecimal weight;

    /** 有效权重：手填值，或「同组平分剩余份额」的结果；父节点为子节点汇总 */
    private BigDecimal effectiveWeight;

    /** 有效权重是否为自动得出（未填被平分、或由子节点汇总） */
    private Boolean weightAuto;

    /** 期望值（理论进度）：按计划工期线性推进，无计划日期为 null */
    private BigDecimal expectedPercent;

    /** 有效计划开始日：自身填了用自身，没填用子树最早（甘特图父节点摘要条用） */
    private LocalDate effectivePlanStart;

    /** 有效计划结束日：自身填了用自身，没填用子树最晚（逾期判定用的就是它） */
    private LocalDate effectivePlanEnd;

    /** 节点状态：in-progress进行中 / completed已完成 / delayed延期 */
    private String progressStatus;

    /** 节点状态中文名 */
    private String progressStatusName;

    /** 节点描述 */
    private String remark;

    /** 是否已完成（口径见 ProjectProgressCalculator） */
    private Boolean completed;

    /** 是否逾期 */
    private Boolean overdue;

    /** 逾期天数（未逾期为 null） */
    private Long overdueDays;
}
