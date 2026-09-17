package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则 新增 / 编辑 请求体
 * <p>项目以 projectId 提交；projectName 由后端联查返回展示。</p>
 */
@Data
public class WarningRuleDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 规则名称（必填） */
    private String ruleName;

    /** 对应项目ID → pm_project.id（必填） */
    private Long projectId;

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

    /** 状态：enabled启用 / disabled禁用（新增默认 enabled） */
    private String status;

    /** 备注 */
    private String remark;
}
