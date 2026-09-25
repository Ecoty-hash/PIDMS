package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 质量/安全检查统计（可视化管理 - 检查合格率与整改闭环率）
 *
 * 口径：
 * - 合格率 = 合格次数 / (合格 + 不合格)，未出结果的（pending）不计入分母
 * - 整改闭环率 = 整改状态为 completed 的记录数 / 整改记录总数
 */
@Data
public class InspectionStatsVO {

    // ---------------- 质量 ----------------

    /** 质量检查累计次数（含未出结果） */
    private Long qualityInspectionCount;

    /** 本月质量检查次数 */
    private Long qualityMonthCount;

    /** 质量检查合格次数 */
    private Long qualityQualifiedCount;

    /** 质量检查不合格次数 */
    private Long qualityUnqualifiedCount;

    /** 质量检查合格率（%） */
    private BigDecimal qualityRate;

    /** 质量整改记录总数 */
    private Long qualityRectificationTotal;

    /** 质量整改已完成数 */
    private Long qualityRectificationDone;

    /** 质量整改闭环率（%） */
    private BigDecimal qualityClosedRate;

    // ---------------- 安全 ----------------

    /** 安全检查累计次数（含未出结果） */
    private Long safetyInspectionCount;

    /** 本月安全检查次数 */
    private Long safetyMonthCount;

    /** 安全检查合格次数 */
    private Long safetyQualifiedCount;

    /** 安全检查不合格次数 */
    private Long safetyUnqualifiedCount;

    /** 安全检查合格率（%） */
    private BigDecimal safetyRate;

    /** 安全整改记录总数 */
    private Long safetyRectificationTotal;

    /** 安全整改已完成数 */
    private Long safetyRectificationDone;

    /** 安全整改闭环率（%） */
    private BigDecimal safetyClosedRate;
}
