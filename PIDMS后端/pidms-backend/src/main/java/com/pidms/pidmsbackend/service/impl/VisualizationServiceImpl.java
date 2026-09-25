package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.entity.Progress;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.ProjectStatus;
import com.pidms.pidmsbackend.entity.QualityInspection;
import com.pidms.pidmsbackend.entity.QualityRectification;
import com.pidms.pidmsbackend.entity.SafetyInspection;
import com.pidms.pidmsbackend.entity.SafetyRectification;
import com.pidms.pidmsbackend.mapper.ProgressMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.ProjectStatusMapper;
import com.pidms.pidmsbackend.mapper.QualityInspectionMapper;
import com.pidms.pidmsbackend.mapper.QualityRectificationMapper;
import com.pidms.pidmsbackend.mapper.SafetyInspectionMapper;
import com.pidms.pidmsbackend.mapper.SafetyRectificationMapper;
import com.pidms.pidmsbackend.service.VisualizationService;
import com.pidms.pidmsbackend.service.support.ProjectProgressCalculator;
import com.pidms.pidmsbackend.vo.InspectionStatsVO;
import com.pidms.pidmsbackend.vo.KeyNodeVO;
import com.pidms.pidmsbackend.vo.ProjectProgressVO;
import com.pidms.pidmsbackend.vo.ProjectStatusStatVO;
import com.pidms.pidmsbackend.vo.ProgressTrendVO;
import com.pidms.pidmsbackend.vo.TrendPointVO;
import com.pidms.pidmsbackend.vo.VisualizationDashboardVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 可视化管理 服务实现（全局视角统计）
 *
 * 进度/逾期口径统一走 {@link ProjectProgressCalculator}，与工作台共用一套算法。
 * 其余口径：
 * - 合格率 = 合格 / (合格 + 不合格)，未出结果的不计入分母
 * - 整改闭环率 = 整改状态为 completed 的记录数 / 整改记录总数
 *
 * 聚合方式为 MyBatis-Plus 查询后在内存里计算，与项目既有风格一致（不写自定义 SQL）；
 * 数据量级是项目管理系统的项目/节点数，内存聚合完全够用。
 */
@Slf4j
@Service
public class VisualizationServiceImpl implements VisualizationService {

    private static final String RESULT_QUALIFIED = "qualified";
    private static final String RESULT_UNQUALIFIED = "unqualified";
    private static final String RECTIFICATION_COMPLETED = "completed";

    private static final String UNKNOWN_STATUS_CODE = "unknown";

    private static final int KEY_NODE_LIMIT = 200;
    private static final int TREND_DAY_POINTS = 7;
    private static final int TREND_WEEK_POINTS = 8;
    private static final int TREND_MONTH_POINTS = 6;

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectStatusMapper projectStatusMapper;
    @Resource
    private ProgressMapper progressMapper;
    @Resource
    private QualityInspectionMapper qualityInspectionMapper;
    @Resource
    private QualityRectificationMapper qualityRectificationMapper;
    @Resource
    private SafetyInspectionMapper safetyInspectionMapper;
    @Resource
    private SafetyRectificationMapper safetyRectificationMapper;
    @Resource
    private ProjectProgressCalculator progressCalculator;

    // ==================================================================
    //  大屏总览
    // ==================================================================

    @Override
    public Result<VisualizationDashboardVO> dashboard() {
        LocalDate today = LocalDate.now();
        List<Project> projects = projectMapper.selectList(null);
        List<Progress> allNodes = progressMapper.selectList(null);
        Map<Long, List<Progress>> byProject = progressCalculator.groupByProject(allNodes);
        Map<String, String> statusNames = progressCalculator.statusNameMap();

        List<ProjectProgressVO> rows = new ArrayList<>();
        List<Progress> ongoingNodes = new ArrayList<>();
        for (Project project : projects) {
            List<Progress> nodes = byProject.getOrDefault(project.getId(), Collections.emptyList());
            rows.add(progressCalculator.build(project, nodes, statusNames, today));
            if (ProjectProgressCalculator.STATUS_IN_PROGRESS.equals(project.getProjectStatus())) {
                ongoingNodes.addAll(nodes);
            }
        }
        // 没有在建项目时（例如演示库里全是已完工项目），用全部节点兜底，避免大屏一片 0
        boolean ongoingEmpty = ongoingNodes.isEmpty();
        List<Progress> scope = ongoingEmpty ? allNodes : ongoingNodes;

        VisualizationDashboardVO vo = new VisualizationDashboardVO();
        vo.setTotalProjects((long) projects.size());
        vo.setOngoingProjects(countByStatus(projects, ProjectProgressCalculator.STATUS_IN_PROGRESS));
        vo.setCompletedProjects(countByStatus(projects, ProjectProgressCalculator.STATUS_COMPLETED));
        vo.setNodeCount((long) allNodes.size());
        // 逾期数只统计叶子节点：父节点由子树汇总，算了会重复计数
        vo.setOverdueNodeCount((long) progressCalculator.leavesOf(progressCalculator.metricsOf(allNodes, today))
                .stream().filter(ProjectProgressCalculator.NodeMetrics::isOverdue).count());
        vo.setDelayedProjects(rows.stream()
                .filter(r -> r.getOverdueNodeCount() != null && r.getOverdueNodeCount() > 0).count());

        BigDecimal actual = progressCalculator.avgCompletion(scope, today);
        BigDecimal plan = progressCalculator.planProgressOf(scope, today);
        BigDecimal expected = progressCalculator.expectedOf(scope, today);
        vo.setOverallProgress(actual);
        vo.setPlanProgress(plan);
        vo.setExpectedProgress(expected);
        vo.setProgressDelta(actual.subtract(plan));
        vo.setDescription("实际进度 = Σ(顶层节点有效权重 × 节点完成度) ÷ Σ权重（父节点完成度由子节点加权汇总，"
                + "未填权重的节点按同组平分剩余份额，存量数据等价于原来的算术平均）；"
                + "计划进度按节点计划工期的时间推进比例推算；期望值（理论进度）= 按计划工期线性推进 "
                + "(今天 − 计划开始) ÷ (计划结束 − 计划开始)，封顶 100%；"
                + "进度偏差 = 实际 − 计划，负值表示整体滞后。统计范围："
                + (ongoingEmpty ? "全部项目" : "在建项目") + "，基准日期 " + today);

        // 分项目进度对比：进度高的排前面，一眼看出谁在拖后腿
        rows.sort(Comparator.comparing(ProjectProgressVO::getActualProgress, Comparator.reverseOrder())
                .thenComparing(ProjectProgressVO::getProjectId, Comparator.nullsLast(Comparator.naturalOrder())));
        vo.setProjectProgress(rows);
        vo.setStatusDistribution(statusDistribution(projects, statusNames));
        return Result.success(vo);
    }

    // ==================================================================
    //  计划 vs 实际 趋势
    // ==================================================================

    @Override
    public Result<ProgressTrendVO> progressTrend(String granularity) {
        LocalDate today = LocalDate.now();
        String g = resolveGranularity(granularity);
        List<Progress> nodes = progressMapper.selectList(null);
        int total = nodes.size();

        List<TrendPointVO> points = new ArrayList<>();
        for (Bucket bucket : buckets(g, today)) {
            TrendPointVO point = new TrendPointVO();
            point.setLabel(bucket.label);
            // 计划完成率：计划结束日已到期的节点占比
            long planned = nodes.stream()
                    .filter(n -> n.getPlanEndDate() != null && !n.getPlanEndDate().isAfter(bucket.end))
                    .count();
            // 实际完成率：实际结束日不晚于该时点的节点占比
            long finished = nodes.stream()
                    .filter(n -> n.getActualEndDate() != null && !n.getActualEndDate().isAfter(bucket.end))
                    .count();
            point.setPlan(progressCalculator.pct(planned, total));
            point.setActual(progressCalculator.pct(finished, total));
            points.add(point);
        }

        ProgressTrendVO vo = new ProgressTrendVO();
        vo.setGranularity(g);
        vo.setPoints(points);
        vo.setDescription(trendDescription(g, total, today));
        return Result.success(vo);
    }

    // ==================================================================
    //  质量 / 安全
    // ==================================================================

    @Override
    public Result<InspectionStatsVO> inspectionStats() {
        YearMonth currentMonth = YearMonth.from(LocalDate.now());

        List<QualityInspection> qualityInspections = qualityInspectionMapper.selectList(null);
        long qQualified = countBy(qualityInspections, QualityInspection::getInspectionResult, RESULT_QUALIFIED);
        long qUnqualified = countBy(qualityInspections, QualityInspection::getInspectionResult, RESULT_UNQUALIFIED);
        List<QualityRectification> qualityRectifications = qualityRectificationMapper.selectList(null);
        long qRectDone = countRectificationDone(qualityRectifications, QualityRectification::getRectificationStatus);

        List<SafetyInspection> safetyInspections = safetyInspectionMapper.selectList(null);
        long sQualified = countBy(safetyInspections, SafetyInspection::getInspectionResult, RESULT_QUALIFIED);
        long sUnqualified = countBy(safetyInspections, SafetyInspection::getInspectionResult, RESULT_UNQUALIFIED);
        List<SafetyRectification> safetyRectifications = safetyRectificationMapper.selectList(null);
        long sRectDone = countRectificationDone(safetyRectifications, SafetyRectification::getRectificationStatus);

        InspectionStatsVO vo = new InspectionStatsVO();
        vo.setQualityInspectionCount((long) qualityInspections.size());
        vo.setQualityMonthCount(qualityInspections.stream()
                .filter(i -> inMonth(i.getInspectionDate(), currentMonth)).count());
        vo.setQualityQualifiedCount(qQualified);
        vo.setQualityUnqualifiedCount(qUnqualified);
        vo.setQualityRate(progressCalculator.pct(qQualified, qQualified + qUnqualified));
        vo.setQualityRectificationTotal((long) qualityRectifications.size());
        vo.setQualityRectificationDone(qRectDone);
        vo.setQualityClosedRate(progressCalculator.pct(qRectDone, qualityRectifications.size()));

        vo.setSafetyInspectionCount((long) safetyInspections.size());
        vo.setSafetyMonthCount(safetyInspections.stream()
                .filter(i -> inMonth(i.getInspectionDate(), currentMonth)).count());
        vo.setSafetyQualifiedCount(sQualified);
        vo.setSafetyUnqualifiedCount(sUnqualified);
        vo.setSafetyRate(progressCalculator.pct(sQualified, sQualified + sUnqualified));
        vo.setSafetyRectificationTotal((long) safetyRectifications.size());
        vo.setSafetyRectificationDone(sRectDone);
        vo.setSafetyClosedRate(progressCalculator.pct(sRectDone, safetyRectifications.size()));
        return Result.success(vo);
    }

    // ==================================================================
    //  状态分布
    // ==================================================================

    @Override
    public Result<List<ProjectStatusStatVO>> projectStatus() {
        return Result.success(statusDistribution(projectMapper.selectList(null), progressCalculator.statusNameMap()));
    }

    // ==================================================================
    //  关键节点
    // ==================================================================

    @Override
    public Result<List<KeyNodeVO>> keyNodes(Long projectId) {
        LambdaQueryWrapper<Progress> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(Progress::getProjectId, projectId);
        }
        List<Progress> nodes = progressMapper.selectList(wrapper);
        LocalDate today = LocalDate.now();

        Map<Long, Project> projects = projectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));

        // 与进度详情页同一套口径：先算派生结果，再按父子关系组成树，子节点收纳在总节点下
        Map<Long, ProjectProgressCalculator.NodeMetrics> metrics = progressCalculator.metricsOf(nodes, today);
        List<KeyNodeVO> list = metrics.values().stream()
                .filter(ProjectProgressCalculator.NodeMetrics::isRoot)
                // 临近计划结束日的排前面；没有计划结束日的排最后（父节点用子树最晚结束日）
                .sorted(Comparator.comparing((ProjectProgressCalculator.NodeMetrics m) -> m.getEffectivePlanEnd(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProjectProgressCalculator.NodeMetrics::getEffectivePlanStart,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(KEY_NODE_LIMIT)
                .map(m -> toKeyNode(m, projects.get(m.getNode().getProjectId()), 0))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    // ==================================================================
    //  内部计算
    // ==================================================================

    /**
     * 关键节点派生结果 → VO（递归），完成/逾期/权重/期望值一律走口径计算器，
     * 大屏与进度详情页展示的是同一套数字。
     *
     * @param level 层级深度，顶层为 0（前端据此缩进，可展开/折叠）
     */
    private KeyNodeVO toKeyNode(ProjectProgressCalculator.NodeMetrics metric, Project project, int level) {
        Progress node = metric.getNode();
        KeyNodeVO vo = new KeyNodeVO();
        vo.setId(node.getId());
        vo.setProjectId(node.getProjectId());
        vo.setParentId(node.getParentId());
        vo.setLevel(level);
        vo.setProjectName(project == null ? null : project.getProjectName());
        vo.setProjectLeader(project == null ? null : project.getProjectLeader());
        vo.setNodeName(node.getProgressName());
        vo.setNodeCode(node.getProgressCode());
        vo.setResponsiblePerson(node.getResponsiblePerson());
        vo.setPlanStartDate(node.getPlanStartDate());
        vo.setPlanEndDate(node.getPlanEndDate());
        vo.setActualStartDate(node.getActualStartDate());
        vo.setActualEndDate(node.getActualEndDate());
        vo.setCompletionPercent(metric.getCompletion());
        vo.setCompletionAuto(metric.isCompletionAuto());
        vo.setWeight(metric.getStoredWeight());
        vo.setEffectiveWeight(metric.getWeight());
        vo.setWeightAuto(metric.isWeightAuto());
        vo.setExpectedPercent(metric.getExpected());

        if (metric.isCompleted()) {
            vo.setNodeStatus("completed");
        } else if (metric.getCompletion().compareTo(BigDecimal.ZERO) > 0
                || node.getActualStartDate() != null) {
            vo.setNodeStatus("current");
        } else {
            vo.setNodeStatus("pending");
        }

        vo.setOverdue(metric.isOverdue());
        vo.setOverdueDays(metric.getOverdueDays());
        vo.setChildCount(metric.getChildren().size());
        vo.setChildren(metric.getChildren().stream()
                .map(child -> toKeyNode(child, project, level + 1))
                .collect(Collectors.toList()));
        return vo;
    }

    private List<ProjectStatusStatVO> statusDistribution(List<Project> projects, Map<String, String> statusNames) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Project project : projects) {
            String code = (project.getProjectStatus() == null || project.getProjectStatus().isBlank())
                    ? UNKNOWN_STATUS_CODE : project.getProjectStatus();
            counts.merge(code, 1L, Long::sum);
        }

        long total = projects.size();
        List<ProjectStatusStatVO> list = new ArrayList<>();
        Set<String> handled = new HashSet<>();
        // 先按字典顺序输出，保证前端图例顺序稳定
        for (ProjectStatus status : projectStatusMapper.selectList(null)) {
            String code = status.getStatusCode();
            Long count = code == null ? null : counts.get(code);
            if (code == null || count == null || count == 0) {
                continue;
            }
            list.add(statusStat(code, progressCalculator.statusName(statusNames, code), count, total));
            handled.add(code);
        }
        // 字典里没有的状态（历史脏数据）兜底展示，避免图例数字对不上
        counts.forEach((code, count) -> {
            if (!handled.contains(code)) {
                list.add(statusStat(code, progressCalculator.statusName(statusNames, code), count, total));
            }
        });
        return list;
    }

    private ProjectStatusStatVO statusStat(String code, String name, Long count, long total) {
        ProjectStatusStatVO stat = new ProjectStatusStatVO();
        stat.setStatusCode(code);
        stat.setStatusName(name);
        stat.setCount(count);
        stat.setPercent(progressCalculator.pct(count, total));
        return stat;
    }

    /** 按某个字段值统计条数（质量/安全检查结果字段同名同义，用取数函数复用） */
    private <T> long countBy(List<T> list, Function<T, String> getter, String value) {
        return list.stream().filter(item -> value.equals(getter.apply(item))).count();
    }

    /** 整改记录中「已整改」的条数 */
    private <T> long countRectificationDone(List<T> list, Function<T, String> statusGetter) {
        return list.stream().filter(item -> RECTIFICATION_COMPLETED.equals(statusGetter.apply(item))).count();
    }

    private long countByStatus(List<Project> projects, String status) {
        return projects.stream().filter(p -> status.equals(p.getProjectStatus())).count();
    }

    private boolean inMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private String resolveGranularity(String granularity) {
        if (granularity == null) {
            return "day";
        }
        String g = granularity.trim().toLowerCase();
        return ("week".equals(g) || "month".equals(g)) ? g : "day";
    }

    /** 趋势横轴：日=最近 7 天，周=最近 8 个自然周（周一起算，标签取周一），月=最近 6 个月 */
    private List<Bucket> buckets(String granularity, LocalDate today) {
        List<Bucket> list = new ArrayList<>();
        if ("month".equals(granularity)) {
            YearMonth current = YearMonth.from(today);
            for (int i = TREND_MONTH_POINTS - 1; i >= 0; i--) {
                YearMonth month = current.minusMonths(i);
                LocalDate end = month.atEndOfMonth();
                list.add(new Bucket(end.isAfter(today) ? today : end, month.format(YM)));
            }
        } else if ("week".equals(granularity)) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            for (int i = TREND_WEEK_POINTS - 1; i >= 0; i--) {
                LocalDate start = monday.minusWeeks(i);
                LocalDate end = start.plusDays(6);
                list.add(new Bucket(end.isAfter(today) ? today : end, start.format(MD)));
            }
        } else {
            for (int i = TREND_DAY_POINTS - 1; i >= 0; i--) {
                LocalDate day = today.minusDays(i);
                list.add(new Bucket(day, day.format(MD)));
            }
        }
        return list;
    }

    private String trendDescription(String granularity, int total, LocalDate today) {
        String range;
        if ("month".equals(granularity)) {
            range = "最近 6 个月，标签为月份";
        } else if ("week".equals(granularity)) {
            range = "最近 8 个自然周（周一至周日），标签取周一日期";
        } else {
            range = "最近 7 天";
        }
        return range + "。计划完成率 = 计划结束日已到期的节点占比；实际完成率 = 实际结束日不晚于该时点的节点占比。"
                + "当前共 " + total + " 个形象进度节点，基准日期 " + today + "。";
    }

    /** 趋势横轴的一个时点 */
    private static class Bucket {
        /** 该时点（统计截止日，含当天） */
        private final LocalDate end;
        /** 横轴标签 */
        private final String label;

        private Bucket(LocalDate end, String label) {
            this.end = end;
            this.label = label;
        }
    }
}
