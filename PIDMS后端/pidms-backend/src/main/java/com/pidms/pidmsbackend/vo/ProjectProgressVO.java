package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 分项目进度对比（可视化管理 - 横向柱状对比；工作台 - 我负责的项目也复用它）
 */
@Data
public class ProjectProgressVO {

    private Long projectId;

    private String projectName;

    private String projectCode;

    /** 项目状态编码，如 in-progress */
    private String projectStatus;

    /** 项目状态中文名 */
    private String projectStatusName;

    /** 项目负责人 */
    private String projectLeader;

    private LocalDate startDate;

    private LocalDate completionDate;

    /** 实际进度：按节点权重汇总的完成度（未填权重时等价于算术平均） */
    private BigDecimal actualProgress;

    /** 计划进度：按工期时间推进比例推算（最早计划开始日 → 最晚计划结束日） */
    private BigDecimal planProgress;

    /** 期望值（理论进度）：各节点按计划工期线性推进后按权重汇总，与「计划进度」两个口径 */
    private BigDecimal expectedProgress;

    /** 进度偏差 = 实际 − 计划，负数代表滞后 */
    private BigDecimal progressDelta;

    /** 节点总数（只算叶子节点，父节点由子树汇总不重复计数） */
    private Integer nodeCount;

    /** 已完成节点数 */
    private Integer completedNodeCount;

    /** 逾期未完成节点数（计划结束日已过、实际结束日仍为空） */
    private Integer overdueNodeCount;

    /** 叶子节点数（等于 nodeCount，前端展示「其中末级节点 N 个」） */
    private Integer leafCount;

    /** 顶层节点数（一级节点个数） */
    private Integer topNodeCount;
}
