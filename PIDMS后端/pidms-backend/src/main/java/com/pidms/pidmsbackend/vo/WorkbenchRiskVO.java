package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工作台 - 逾期节点预警项
 */
@Data
public class WorkbenchRiskVO {

    private Long projectId;

    private String projectName;

    private Long nodeId;

    /** 节点名称 */
    private String nodeName;

    private String projectLeader;

    private LocalDate planEndDate;

    /** 逾期天数（今天 − 计划结束日） */
    private Long overdueDays;

    /** 节点完成百分比 */
    private BigDecimal completionPercent;

    /** 逾期严重度：serious（≥30 天）/ warning（≥7 天）/ notice（其余） */
    private String severity;
}
