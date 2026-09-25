package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 关键节点（可视化管理 - 关键节点时间线）
 * 数据来自 cm_progress 形象进度节点，一个节点即一个关键节点；子节点嵌在 children 里，
 * 大屏上点击可展开/折叠。完成度/期望值/逾期口径与进度详情页完全一致（ProjectProgressCalculator）。
 */
@Data
public class KeyNodeVO {

    private Long id;

    private Long projectId;

    /** 父节点ID；NULL 为顶层节点 */
    private Long parentId;

    /** 层级深度（顶层为 0，前端据此缩进） */
    private Integer level;

    /** 子节点（多层嵌套；无子节点为空数组） */
    private List<KeyNodeVO> children = new ArrayList<>();

    /** 直接子节点个数 */
    private Integer childCount;

    private String projectName;

    /** 节点名称 */
    private String nodeName;

    /** 节点编码 */
    private String nodeCode;

    /** 节点负责人（真实姓名），可为空 */
    private String responsiblePerson;

    /** 项目负责人 */
    private String projectLeader;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    /** 完成百分比：叶子取手填值，父节点为子节点加权汇总值 */
    private BigDecimal completionPercent;

    /** 完成度是否为子节点自动汇总（父节点 true） */
    private Boolean completionAuto;

    /** 手填的占总进度百分比(0-100)，未填为 null */
    private BigDecimal weight;

    /** 有效权重：手填值，或「同组平分剩余份额」的结果；父节点为子节点汇总 */
    private BigDecimal effectiveWeight;

    /** 有效权重是否为自动得出 */
    private Boolean weightAuto;

    /** 期望值（理论进度）：按计划工期线性推进，无计划日期为 null */
    private BigDecimal expectedPercent;

    /** 节点状态：completed 已完成 / current 进行中 / pending 未开始 */
    private String nodeStatus;

    /** 是否逾期（计划结束日已过且未完成） */
    private Boolean overdue;

    /** 逾期天数，未逾期时为 null */
    private Long overdueDays;
}
