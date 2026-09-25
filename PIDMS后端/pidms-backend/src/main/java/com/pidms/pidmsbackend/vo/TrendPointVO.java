package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 进度趋势上的一个数据点（可视化管理 - 计划/实际对比折线）
 */
@Data
public class TrendPointVO {

    /** 横轴标签：日/周为 MM-dd，月为 yyyy-MM */
    private String label;

    /** 截止该时点的计划节点完成率（%） */
    private BigDecimal plan;

    /** 截止该时点的实际节点完成率（%） */
    private BigDecimal actual;
}
