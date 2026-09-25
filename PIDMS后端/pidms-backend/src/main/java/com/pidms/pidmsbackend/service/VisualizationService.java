package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.vo.InspectionStatsVO;
import com.pidms.pidmsbackend.vo.KeyNodeVO;
import com.pidms.pidmsbackend.vo.ProjectStatusStatVO;
import com.pidms.pidmsbackend.vo.ProgressTrendVO;
import com.pidms.pidmsbackend.vo.VisualizationDashboardVO;

import java.util.List;

/**
 * 可视化管理 服务接口（全局视角的统计看板）
 */
public interface VisualizationService {

    /**
     * 大屏总览：项目规模、总体进度、状态分布、分项目进度对比
     */
    Result<VisualizationDashboardVO> dashboard();

    /**
     * 计划 vs 实际 进度趋势
     *
     * @param granularity 统计粒度：day / week / month，缺省 day
     */
    Result<ProgressTrendVO> progressTrend(String granularity);

    /**
     * 质量/安全检查合格率与整改闭环率
     */
    Result<InspectionStatsVO> inspectionStats();

    /**
     * 项目状态分布（环形图数据）
     */
    Result<List<ProjectStatusStatVO>> projectStatus();

    /**
     * 关键节点时间线
     *
     * @param projectId 项目 ID，为空则返回全部项目（最多 200 条）
     */
    Result<List<KeyNodeVO>> keyNodes(Long projectId);
}
