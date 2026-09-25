package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 可视化管理大屏总览（/api/visualization/dashboard）
 */
@Data
public class VisualizationDashboardVO {

    /** 项目总数 */
    private Long totalProjects;

    /** 在建项目数 */
    private Long ongoingProjects;

    /** 已完工项目数 */
    private Long completedProjects;

    /** 存在逾期节点的项目数 */
    private Long delayedProjects;

    /** 逾期未完成节点总数 */
    private Long overdueNodeCount;

    /** 形象进度节点总数 */
    private Long nodeCount;

    /**
     * 总体实际进度（%）：在建项目（无在建项目时取全部项目）下所有顶层节点的
     * 加权完成度，等价于叶子节点加权平均；未填权重的节点按同组平分剩余份额。
     */
    private BigDecimal overallProgress;

    /** 总体计划进度（%）：按工期时间推进比例推算 */
    private BigDecimal planProgress;

    /** 总体期望值（理论进度，%）：按各节点计划工期线性推进后加权汇总 */
    private BigDecimal expectedProgress;

    /** 进度偏差 = 实际 − 计划，负数代表整体滞后 */
    private BigDecimal progressDelta;

    /** 以当前日期为基准，统计口径说明 */
    private String description;

    /** 项目状态分布 */
    private List<ProjectStatusStatVO> statusDistribution;

    /** 分项目进度对比 */
    private List<ProjectProgressVO> projectProgress;
}
