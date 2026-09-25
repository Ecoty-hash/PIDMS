package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 进度趋势（可视化管理 - 计划 vs 实际）
 */
@Data
public class ProgressTrendVO {

    /** 统计粒度：day / week / month */
    private String granularity;

    /** 统计口径说明，前端直接展示，避免看的人误读 */
    private String description;

    /** 数据点，按时间正序 */
    private List<TrendPointVO> points;
}
