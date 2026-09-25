package com.pidms.pidmsbackend.service.support;

import com.pidms.pidmsbackend.entity.Progress;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.ProjectStatus;
import com.pidms.pidmsbackend.mapper.ProjectStatusMapper;
import com.pidms.pidmsbackend.vo.ProjectProgressVO;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目进度口径计算器：工作台、可视化管理、项目进度详情页共用同一套算法。
 *
 * 单独抽出来的原因：进度/计划/逾期这几个口径一旦在两处各写一份，很容易演变成
 * 两套互相矛盾的定义（这个项目在 cm_progress 上已经吃过一次亏）。凡是涉及
 * 「项目实际进度 / 计划进度 / 期望值 / 节点是否完成 / 节点是否逾期 / 权重汇总」的计算，
 * 都必须走这里，前端不要再算一遍。
 *
 * 节点层级（cm_progress.parent_id，多层）与权重（cm_progress.weight，占总进度%）：
 * - 叶子节点完成度 = 手填的 completion_percent
 * - 父节点完成度   = Σ(子节点有效权重 × 子节点完成度) ÷ Σ子节点有效权重
 * - 父节点权重     = Σ子节点有效权重（落库的 weight 只对叶子有意义，父节点上填了也不参与计算）
 * - 项目实际进度   = Σ(顶层节点权重 × 顶层节点完成度) ÷ Σ顶层节点权重，
 *                   等价于对所有叶子节点做加权平均，不会把父节点重复算一次
 * - 期望值（理论进度）= 按计划工期线性推进 (今天 − 计划开始) ÷ (计划结束 − 计划开始)，封顶 100%；
 *                   未填计划日期 → 无期望值；父节点与项目级按同一权重汇总
 * - 节点数 / 已完成数 / 逾期数只统计**叶子节点**，避免父子重复计数
 *
 * 有效权重规则（保证存量数据口径不变）：同一组兄弟节点里，填了权重（> 0）的按填的值算，
 * 没填的平分「100 − 已填之和」；整组都没填即每人 100/n（等价于原来的算术平均）；
 * 已填之和 ≥ 100 时未填者权重为 0（认填了的那部分）。
 * 顶层节点也是一组兄弟（父节点是「项目」），否则它们的权重恒为 0，
 * 页面上「占总进度%」全显示 0%；有子节点的节点不参与平分，份额由子树汇总。
 */
@Component
public class ProjectProgressCalculator {

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** 项目状态编码：在建 */
    public static final String STATUS_IN_PROGRESS = "in-progress";
    /** 项目状态编码：已完工 */
    public static final String STATUS_COMPLETED = "completed";
    /** 节点状态编码：已完成 */
    public static final String NODE_STATUS_COMPLETED = "completed";
    /** 节点状态编码：进行中 */
    public static final String NODE_STATUS_IN_PROGRESS = "in-progress";
    /** 节点状态编码：延期 */
    public static final String NODE_STATUS_DELAYED = "delayed";

    /** 节点状态中文名（固定三值枚举，非字典表） */
    private static final Map<String, String> NODE_STATUS_NAMES = Map.of(
            NODE_STATUS_IN_PROGRESS, "进行中",
            NODE_STATUS_COMPLETED, "已完成",
            NODE_STATUS_DELAYED, "延期");

    private static final String UNKNOWN_STATUS_NAME = "未设置";

    /**
     * 节点展示顺序：计划开始日早的在前，没填计划开始日的排最后，同日期按 id。
     * 三个页面（详情页甘特图 / 进度管理列表 / 大屏关键节点）都用这一个顺序，不再各自排。
     */
    private static final Comparator<NodeMetrics> NODE_ORDER = Comparator
            .comparing((NodeMetrics m) -> m.getNode().getPlanStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(NodeMetrics::getId);

    @Resource
    private ProjectStatusMapper projectStatusMapper;

    /**
     * 节点派生结果：权重、完成度、期望值、完成与逾期判定，父子关系已建立。
     * <p>用 @Getter/@Setter 而不是 @Data：children 是可变集合，不生成 equals/hashCode/toString 更安全。</p>
     */
    @Getter
    @Setter
    public static class NodeMetrics {

        /** 原始实体（编辑回显用，如手填的 weight / completionPercent） */
        private final Progress node;

        private final Long id;

        /** 父节点 id，null = 顶层 */
        private Long parentId;

        /** 该节点在当前集合里是否为顶层（父节点不在集合内时也算顶层，避免分页丢节点） */
        private boolean root;

        /** 有效权重：叶子 = 手填值或平分值；父节点 = 子节点汇总 */
        private BigDecimal weight = ZERO;

        /** 权重是否为自动得出（未填被平分、或由子节点汇总） */
        private boolean weightAuto;

        /** 完成度：叶子 = 手填值；父节点 = 子节点加权平均 */
        private BigDecimal completion = ZERO;

        /** 完成度是否由子节点汇总得出（父节点为 true，编辑时该字段只读） */
        private boolean completionAuto;

        /** 期望值（理论进度），无计划开始/结束日期时为 null */
        private BigDecimal expected;

        /** 有效计划开始日：自身填了用自身的，没填用子树最早 */
        private LocalDate effectivePlanStart;

        /** 有效计划结束日：自身填了用自身的，没填用子树最晚（逾期判定用的就是它） */
        private LocalDate effectivePlanEnd;

        private boolean completed;

        private boolean overdue;

        /** 逾期天数（未逾期为 null） */
        private Long overdueDays;

        private final List<NodeMetrics> children = new ArrayList<>();

        NodeMetrics(Progress node) {
            this.node = node;
            this.id = node.getId();
        }

        /** 是否为叶子节点（没有子节点） */
        public boolean isLeaf() {
            return children.isEmpty();
        }

        /** 原始实体上手填的权重值（可能为 null，表示未填） */
        public BigDecimal getStoredWeight() {
            return node.getWeight();
        }
    }

    /** 按项目 ID 归组节点 */
    public Map<Long, List<Progress>> groupByProject(List<Progress> nodes) {
        Map<Long, List<Progress>> map = new HashMap<>();
        for (Progress node : nodes) {
            if (node.getProjectId() != null) {
                map.computeIfAbsent(node.getProjectId(), k -> new ArrayList<>()).add(node);
            }
        }
        return map;
    }

    // ==================================================================
    //  层级与权重计算
    // ==================================================================

    /**
     * 计算一组节点（同一项目）的派生结果，key = 节点 id。
     * <p>父节点不在集合内的节点会被当作顶层（分页只取一页时不会把子节点算丢），
     * 并且对「互为父子」的脏数据做了提升处理，保证一定能算完。</p>
     */
    public Map<Long, NodeMetrics> metricsOf(List<Progress> nodes, LocalDate today) {
        Map<Long, NodeMetrics> metrics = new LinkedHashMap<>();
        if (nodes == null) {
            return metrics;
        }
        for (Progress node : nodes) {
            if (node != null && node.getId() != null) {
                metrics.put(node.getId(), new NodeMetrics(node));
            }
        }

        // 建父子关系；父节点指向自己或不在集合内 → 当顶层
        for (NodeMetrics metric : metrics.values()) {
            Long parentId = metric.getNode().getParentId();
            metric.setParentId(parentId);
            NodeMetrics parent = (parentId == null || parentId.equals(metric.getId()))
                    ? null : metrics.get(parentId);
            if (parent == null) {
                metric.setRoot(true);
            } else {
                parent.getChildren().add(metric);
            }
        }
        metrics.values().forEach(m -> m.getChildren().sort(NODE_ORDER));

        Set<Long> visited = new HashSet<>();
        for (NodeMetrics metric : metrics.values()) {
            if (metric.isRoot()) {
                compute(metric, today, visited);
            }
        }
        // 兜底：互为父子的脏数据（写入时已拦，这里保证不出现「算不到 = 页面上消失」）
        for (NodeMetrics metric : metrics.values()) {
            if (!visited.contains(metric.getId())) {
                NodeMetrics parent = metrics.get(metric.getParentId());
                if (parent != null) {
                    parent.getChildren().remove(metric);
                }
                metric.setRoot(true);
                compute(metric, today, visited);
            }
        }
        // 顶层节点是「项目」这一层的兄弟组：不平分的话它们权重恒为 0，
        // 页面上「占总进度%」会全显示 0%（汇总本身会退化成算术平均，看不出问题，只有展示看得出来）。
        assignEffectiveWeights(rootsOf(metrics));
        return metrics;
    }

    /** 顶层节点（按计划开始日、再按 id 排序，保证展示顺序稳定） */
    public List<NodeMetrics> rootsOf(Map<Long, NodeMetrics> metrics) {
        return metrics.values().stream()
                .filter(NodeMetrics::isRoot)
                .sorted(NODE_ORDER)
                .collect(Collectors.toList());
    }

    /** 叶子节点（节点数 / 已完成数 / 逾期数按它统计，避免父子重复计数） */
    public List<NodeMetrics> leavesOf(Map<Long, NodeMetrics> metrics) {
        return metrics.values().stream()
                .filter(NodeMetrics::isLeaf)
                .sorted(Comparator.comparing(NodeMetrics::getId))
                .collect(Collectors.toList());
    }

    /** 后序遍历：先给子节点这一组分配有效权重，再递归算子树，最后汇总自身 */
    private void compute(NodeMetrics metric, LocalDate today, Set<Long> visited) {
        if (!visited.add(metric.getId())) {
            return; // 成环保护
        }
        assignEffectiveWeights(metric.getChildren());
        for (NodeMetrics child : metric.getChildren()) {
            compute(child, today, visited);
        }

        LocalDate ownStart = metric.getNode().getPlanStartDate();
        LocalDate ownEnd = metric.getNode().getPlanEndDate();
        if (metric.isLeaf()) {
            metric.setCompletion(orZero(metric.getNode().getCompletionPercent()));
            metric.setCompletionAuto(false);
            metric.setExpected(linearExpected(ownStart, ownEnd, today));
            metric.setEffectivePlanStart(ownStart);
            metric.setEffectivePlanEnd(ownEnd);
            metric.setCompleted(isCompleted(metric.getNode()));
        } else {
            List<NodeMetrics> children = metric.getChildren();
            metric.setWeight(sumWeight(children));
            metric.setWeightAuto(true);
            metric.setCompletion(weightedAvg(children, NodeMetrics::getCompletion));
            metric.setCompletionAuto(true);
            metric.setExpected(weightedAvg(children, m -> m.getExpected() == null ? ZERO : m.getExpected()));
            metric.setEffectivePlanStart(ownStart != null ? ownStart : minDate(children, true));
            metric.setEffectivePlanEnd(ownEnd != null ? ownEnd : minDate(children, false));
            metric.setCompleted(children.stream().allMatch(NodeMetrics::isCompleted));
        }

        boolean overdue = !metric.isCompleted()
                && metric.getEffectivePlanEnd() != null
                && metric.getEffectivePlanEnd().isBefore(today);
        metric.setOverdue(overdue);
        metric.setOverdueDays(overdue ? ChronoUnit.DAYS.between(metric.getEffectivePlanEnd(), today) : null);
    }

    /**
     * 同一组兄弟节点的有效权重：填了（> 0）的按填的算，没填的平分「100 − 已填之和」。
     * 整组都没填时每人 100/n，与原来的「算术平均」完全一致，存量数据口径不变。
     * <p>有子节点的兄弟不参与平分：它的份额由子树汇总（见 compute），
     * 在这里给它分一份会把子树权重凭空放大或缩小。</p>
     */
    private void assignEffectiveWeights(List<NodeMetrics> siblings) {
        if (siblings.isEmpty()) {
            return;
        }
        List<NodeMetrics> leaves = siblings.stream().filter(NodeMetrics::isLeaf).collect(Collectors.toList());
        if (leaves.isEmpty()) {
            return; // 整组都是父节点，各自的权重由子树算
        }
        BigDecimal filledSum = ZERO;
        int unfilled = 0;
        for (NodeMetrics sibling : leaves) {
            BigDecimal stored = sibling.getNode().getWeight();
            if (isFilled(stored)) {
                filledSum = filledSum.add(stored);
            } else {
                unfilled++;
            }
        }
        BigDecimal remaining = HUNDRED.subtract(filledSum);
        if (remaining.compareTo(ZERO) < 0) {
            remaining = ZERO;
        }
        BigDecimal share = unfilled == 0 ? ZERO
                : remaining.divide(BigDecimal.valueOf(unfilled), 4, RoundingMode.HALF_UP);
        for (NodeMetrics sibling : leaves) {
            BigDecimal stored = sibling.getNode().getWeight();
            if (isFilled(stored)) {
                sibling.setWeight(stored);
                sibling.setWeightAuto(false);
            } else {
                sibling.setWeight(share);
                sibling.setWeightAuto(true);
            }
        }
    }

    private boolean isFilled(BigDecimal weight) {
        return weight != null && weight.compareTo(ZERO) > 0;
    }

    private BigDecimal sumWeight(List<NodeMetrics> list) {
        BigDecimal sum = ZERO;
        for (NodeMetrics metric : list) {
            sum = sum.add(orZero(metric.getWeight()));
        }
        return sum;
    }

    /** 按权重加权平均；权重全为 0 时退化为算术平均，避免整组算出 0 */
    public BigDecimal weightedAvg(List<NodeMetrics> list, Function<NodeMetrics, BigDecimal> getter) {
        if (list == null || list.isEmpty()) {
            return ZERO;
        }
        BigDecimal weightSum = ZERO;
        BigDecimal acc = ZERO;
        for (NodeMetrics metric : list) {
            BigDecimal weight = orZero(metric.getWeight());
            weightSum = weightSum.add(weight);
            acc = acc.add(weight.multiply(orZero(getter.apply(metric))));
        }
        if (weightSum.compareTo(ZERO) == 0) {
            return avgValues(list.stream().map(getter).collect(Collectors.toList()));
        }
        return acc.divide(weightSum, 1, RoundingMode.HALF_UP);
    }

    /** 子树的计划日期边界；earliest = true 取最早开始日，false 取最晚结束日 */
    private LocalDate minDate(List<NodeMetrics> children, boolean earliest) {
        LocalDate result = null;
        for (NodeMetrics child : children) {
            LocalDate date = earliest ? child.getEffectivePlanStart() : child.getEffectivePlanEnd();
            if (date == null) {
                continue;
            }
            if (result == null || (earliest ? date.isBefore(result) : date.isAfter(result))) {
                result = date;
            }
        }
        return result;
    }

    /**
     * 期望值（理论进度）：按计划工期线性推进，封顶 100%。
     * 计划日期不全 → null（页面上显示「—」，而不是 0%，避免被误读成「还没到时间」）。
     */
    public BigDecimal linearExpected(LocalDate planStart, LocalDate planEnd, LocalDate today) {
        if (planStart == null || planEnd == null) {
            return null;
        }
        if (!planEnd.isAfter(planStart)) {
            return today.isBefore(planStart) ? ZERO : HUNDRED;
        }
        long totalDays = ChronoUnit.DAYS.between(planStart, planEnd);
        long passedDays = ChronoUnit.DAYS.between(planStart, today);
        if (passedDays <= 0) {
            return ZERO;
        }
        if (passedDays >= totalDays) {
            return HUNDRED;
        }
        return pct(passedDays, totalDays);
    }

    // ==================================================================
    //  项目级汇总
    // ==================================================================

    /** 汇总单个项目的进度信息（状态中文名由 statusNames 提供，可为空 Map） */
    public ProjectProgressVO build(Project project, List<Progress> nodes,
                                   Map<String, String> statusNames, LocalDate today) {
        ProjectProgressVO vo = new ProjectProgressVO();
        vo.setProjectId(project.getId());
        vo.setProjectName(project.getProjectName());
        vo.setProjectCode(project.getProjectCode());
        vo.setProjectStatus(project.getProjectStatus());
        vo.setProjectStatusName(statusName(statusNames, project.getProjectStatus()));
        vo.setProjectLeader(project.getProjectLeader());
        vo.setStartDate(toLocalDate(project.getStartDate()));
        vo.setCompletionDate(toLocalDate(project.getCompletionDate()));

        Map<Long, NodeMetrics> metrics = metricsOf(nodes, today);
        List<NodeMetrics> roots = rootsOf(metrics);
        // 叶子节点才是真正的工作量：父节点由子树汇总，不能重复计一次
        List<NodeMetrics> leaves = leavesOf(metrics);

        BigDecimal actual = weightedAvg(roots, NodeMetrics::getCompletion);
        BigDecimal plan = planProgressOf(nodes, today);
        BigDecimal expected = weightedAvg(roots, m -> m.getExpected() == null ? ZERO : m.getExpected());
        vo.setActualProgress(actual);
        vo.setPlanProgress(plan);
        vo.setExpectedProgress(expected);
        vo.setProgressDelta(actual.subtract(plan));
        vo.setNodeCount(leaves.size());
        vo.setCompletedNodeCount((int) leaves.stream().filter(NodeMetrics::isCompleted).count());
        vo.setOverdueNodeCount((int) leaves.stream().filter(NodeMetrics::isOverdue).count());
        vo.setLeafCount(leaves.size());
        vo.setTopNodeCount(roots.size());
        return vo;
    }

    /**
     * 实际进度：按权重汇总的完成度（无权重时等价于原来的算术平均）。
     * 保持「一组节点 → 一个百分比」的语义，工作台/可视化直接复用。
     */
    public BigDecimal avgCompletion(List<Progress> nodes, LocalDate today) {
        Map<Long, NodeMetrics> metrics = metricsOf(nodes, today);
        return weightedAvg(rootsOf(metrics), NodeMetrics::getCompletion);
    }

    /** 期望值：按权重汇总的理论进度（与 avgCompletion 同一套层级口径） */
    public BigDecimal expectedOf(List<Progress> nodes, LocalDate today) {
        Map<Long, NodeMetrics> metrics = metricsOf(nodes, today);
        return weightedAvg(rootsOf(metrics), m -> m.getExpected() == null ? ZERO : m.getExpected());
    }

    /** 一组百分比的均值（忽略 null），保留 1 位小数；空集合返回 0 */
    public BigDecimal avgValues(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }
        BigDecimal sum = ZERO;
        int count = 0;
        for (BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
                count++;
            }
        }
        return count == 0 ? ZERO : sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    /** 计划进度：按工期时间推进比例推算（项目级时间基准，与节点期望值是两个口径，页面上分开展示） */
    public BigDecimal planProgressOf(List<Progress> nodes, LocalDate today) {
        if (nodes == null || nodes.isEmpty()) {
            return ZERO;
        }
        LocalDate start = null;
        LocalDate end = null;
        for (Progress node : nodes) {
            if (node.getPlanStartDate() != null && (start == null || node.getPlanStartDate().isBefore(start))) {
                start = node.getPlanStartDate();
            }
            if (node.getPlanEndDate() != null && (end == null || node.getPlanEndDate().isAfter(end))) {
                end = node.getPlanEndDate();
            }
        }
        if (start == null || end == null || !end.isAfter(start)) {
            long due = nodes.stream()
                    .filter(n -> n.getPlanEndDate() != null && !n.getPlanEndDate().isAfter(today))
                    .count();
            return pct(due, nodes.size());
        }
        long totalDays = ChronoUnit.DAYS.between(start, end);
        long passedDays = ChronoUnit.DAYS.between(start, today);
        if (passedDays <= 0) {
            return ZERO;
        }
        if (passedDays >= totalDays) {
            return HUNDRED;
        }
        return pct(passedDays, totalDays);
    }

    // ==================================================================
    //  单节点判定（叶子节点的基础规则，树形汇总见 metricsOf）
    // ==================================================================

    /** 节点是否已完成（实体自身口径；父节点的汇总判定见 NodeMetrics） */
    public boolean isCompleted(Progress node) {
        return node.getActualEndDate() != null
                || NODE_STATUS_COMPLETED.equals(node.getProgressStatus())
                || (node.getCompletionPercent() != null && node.getCompletionPercent().compareTo(HUNDRED) >= 0);
    }

    /** 节点是否逾期（当天到期不算逾期） */
    public boolean isOverdue(Progress node, LocalDate today) {
        return !isCompleted(node)
                && node.getPlanEndDate() != null
                && node.getPlanEndDate().isBefore(today);
    }

    /** 逾期天数（已完成的节点返回 null） */
    public Long overdueDays(Progress node, LocalDate today) {
        return isOverdue(node, today) ? ChronoUnit.DAYS.between(node.getPlanEndDate(), today) : null;
    }

    /** 逾期严重度：serious ≥30 天 / warning ≥7 天 / notice 其余 */
    public String severity(Long overdueDays) {
        if (overdueDays == null) {
            return null;
        }
        if (overdueDays >= 30) {
            return "serious";
        }
        return overdueDays >= 7 ? "warning" : "notice";
    }

    /** statusCode → statusName 字典 */
    public Map<String, String> statusNameMap() {
        Map<String, String> map = new HashMap<>();
        for (ProjectStatus status : projectStatusMapper.selectList(null)) {
            if (status.getStatusCode() != null && !status.getStatusCode().isBlank()) {
                map.put(status.getStatusCode(), status.getStatusName());
            }
        }
        return map;
    }

    /** 节点状态中文名；空值或未知编码回落为编码本身 / 未设置 */
    public String nodeStatusName(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN_STATUS_NAME;
        }
        return NODE_STATUS_NAMES.getOrDefault(code, code);
    }

    /** 状态中文名；字典缺失时回落为编码本身，空值显示「未设置」 */
    public String statusName(Map<String, String> statusNames, String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN_STATUS_NAME;
        }
        String name = statusNames == null ? null : statusNames.get(code);
        return (name == null || name.isBlank()) ? code : name;
    }

    public LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    /** 百分比，保留 1 位小数；分母 <= 0 返回 0 */
    public BigDecimal pct(long part, long total) {
        if (total <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
