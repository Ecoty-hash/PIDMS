package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则 VO（列表 / 详情通用）
 * <p>projectName 为展示字段，由后端联查 pm_project 填充（表内不落库）。</p>
 */
@Data
public class WarningRuleVO {

    /** 主键ID */
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 对应项目ID → pm_project.id */
    private Long projectId;

    /** 项目名称（联查展示，非落库字段） */
    private String projectName;

    /** 对应计划名称 */
    private String planName;

    /** 进度百分比(0-100) */
    private BigDecimal progressPercent;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 进度偏差阈值(%) */
    private BigDecimal progressDeviationThreshold;

    /** 预警类型：进度停滞预警 / 质量问题预警 / 安全预警 */
    private String warningType;

    /** 分级提醒规则配置 */
    private String levelRuleConfig;

    /** 状态：enabled启用 / disabled禁用 */
    private String status;

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
