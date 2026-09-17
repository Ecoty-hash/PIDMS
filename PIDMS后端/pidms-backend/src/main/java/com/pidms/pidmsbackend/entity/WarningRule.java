package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则表 cm_warning_rule
 * <p>针对项目进度 / 质量 / 安全配置预警规则；项目通过 project_id 关联 pm_project。</p>
 * 状态取值：enabled启用 / disabled禁用；预警类型：进度停滞预警 / 质量问题预警 / 安全预警
 */
@Data
@TableName("cm_warning_rule")
public class WarningRule {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
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

    /** 状态：enabled启用 / disabled禁用（默认 enabled） */
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
