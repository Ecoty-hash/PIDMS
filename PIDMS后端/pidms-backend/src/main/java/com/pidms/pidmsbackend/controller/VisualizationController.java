package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.service.VisualizationService;
import com.pidms.pidmsbackend.vo.InspectionStatsVO;
import com.pidms.pidmsbackend.vo.KeyNodeVO;
import com.pidms.pidmsbackend.vo.ProjectStatusStatVO;
import com.pidms.pidmsbackend.vo.ProgressTrendVO;
import com.pidms.pidmsbackend.vo.VisualizationDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可视化管理 接口（全局统计看板，前端 /visualization 页面使用）
 */
@Slf4j
@RestController
@RequestMapping("/api/visualization")
@Tag(name = "可视化管理接口")
public class VisualizationController {

    @Resource
    private VisualizationService visualizationService;

    @GetMapping("/dashboard")
    @Operation(summary = "大屏总览：项目规模、总体进度、状态分布、分项目进度对比")
    public Result<VisualizationDashboardVO> dashboard() {
        log.info("查询可视化管理总览");
        return visualizationService.dashboard();
    }

    @GetMapping("/progress-trend")
    @Operation(summary = "计划 vs 实际 进度趋势（granularity: day/week/month，缺省 day）")
    public Result<ProgressTrendVO> progressTrend(@RequestParam(required = false) String granularity) {
        log.info("查询进度趋势，granularity：{}", granularity);
        return visualizationService.progressTrend(granularity);
    }

    @GetMapping("/inspection-stats")
    @Operation(summary = "质量/安全检查合格率与整改闭环率")
    public Result<InspectionStatsVO> inspectionStats() {
        log.info("查询质量/安全检查统计");
        return visualizationService.inspectionStats();
    }

    @GetMapping("/project-status")
    @Operation(summary = "项目状态分布")
    public Result<List<ProjectStatusStatVO>> projectStatus() {
        log.info("查询项目状态分布");
        return visualizationService.projectStatus();
    }

    @GetMapping("/key-nodes")
    @Operation(summary = "关键节点时间线（projectId 可选）")
    public Result<List<KeyNodeVO>> keyNodes(@RequestParam(required = false) Long projectId) {
        log.info("查询关键节点，projectId：{}", projectId);
        return visualizationService.keyNodes(projectId);
    }
}
