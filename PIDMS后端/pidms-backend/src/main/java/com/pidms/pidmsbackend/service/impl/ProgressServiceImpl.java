package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.ProgressConvert;
import com.pidms.pidmsbackend.dto.ProgressDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Progress;
import com.pidms.pidmsbackend.entity.ProgressQueryParam;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.SysUser;
import com.pidms.pidmsbackend.mapper.ProgressMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.SysUserMapper;
import com.pidms.pidmsbackend.service.ProgressService;
import com.pidms.pidmsbackend.service.support.ProjectProgressCalculator;
import com.pidms.pidmsbackend.vo.ProgressBoardVO;
import com.pidms.pidmsbackend.vo.ProgressNodeVO;
import com.pidms.pidmsbackend.vo.ProgressParentOptionVO;
import com.pidms.pidmsbackend.vo.ProgressVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 进度管理 服务实现
 * <p>
 * 完成状态联动：节点状态是「已完成」时，完成百分比锁定为 100、实际结束日期补当天；
 * 状态从「已完成」改回进行中/延期时，实际结束日期清空。这样 ProjectProgressCalculator
 * 的实际进度、逾期判定不会和节点状态互相打架（避免出现「状态已完成、完成度 30%」）。
 * </p>
 */
@Slf4j
@Service
public class ProgressServiceImpl implements ProgressService {

    /** 进度状态：进行中 */
    private static final String STATUS_IN_PROGRESS = "in-progress";
    /** 进度状态：已完成 */
    private static final String STATUS_COMPLETED = "completed";
    /** 进度状态：延期 */
    private static final String STATUS_DELAYED = "delayed";

    /** 人员状态：启用 */
    private static final String USER_STATUS_ENABLED = "enabled";

    /** 项目成员分隔符：中英文逗号都兼容 */
    private static final String MEMBER_SEPARATOR = "[,，]";

    /** 节点负责人列名：库结构比代码旧时用于识别报错 */
    private static final String OWNER_COLUMN = "responsible_person";
    /** 补列脚本：报错提示里直接给出可执行路径 */
    private static final String OWNER_COLUMN_SCRIPT = "database/upgrade/13_add_progress_responsible_person.sql";

    /** 向上追溯层级的最大步数：正常层级远达不到，用来兜住环状脏数据 */
    private static final int MAX_TREE_DEPTH = 64;

    @Resource
    private ProgressMapper progressMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private ProgressConvert progressConvert;
    @Resource
    private ProjectProgressCalculator progressCalculator;

    @Override
    public Result<PageInfo<ProgressVO>> pageQuery(ProgressQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询进度管理：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<Progress> queryWrapper = new QueryWrapper<>();

        // 高级搜索：进度状态精确匹配
        queryWrapper.eq(StringUtils.hasText(queryParam.getProgressStatus()), "progress_status", queryParam.getProgressStatus());
        // 高级搜索：进度名称模糊
        queryWrapper.like(StringUtils.hasText(queryParam.getProgressName()), "progress_name", queryParam.getProgressName());

        // 高级搜索：项目名称过滤 —— 先在 pm_project 匹配名称得到 id 集，再约束 cm_progress.project_id
        List<Long> nameMatchIds = null;
        if (StringUtils.hasText(queryParam.getProjectName())) {
            nameMatchIds = findProjectIdsByName(queryParam.getProjectName());
            if (nameMatchIds.isEmpty()) {
                log.info("项目名称「{}」未匹配到任何项目，返回空列表", queryParam.getProjectName());
                return Result.success(emptyPage(pageNum, pageSize));
            }
            queryWrapper.in("project_id", nameMatchIds);
        }

        // 关键字：进度名称 / 进度编号 / 项目名称（项目名同样先转 id 集）
        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            List<Long> keywordProjectIds = findProjectIdsByName(keyword);
            queryWrapper.and(w -> {
                w.like("progress_name", keyword)
                        .or().like("progress_code", keyword);
                if (!keywordProjectIds.isEmpty()) {
                    w.or().in("project_id", keywordProjectIds);
                }
            });
            log.info("关键字搜索「{}」命中项目 id 集：{}", keyword, keywordProjectIds);
        }

        queryWrapper.orderByAsc("id");

        Page<Progress> page = new Page<>(pageNum, pageSize);
        Page<Progress> resultPage = progressMapper.selectPage(page, queryWrapper);
        List<Progress> records = resultPage.getRecords();
        log.info("分页查询进度管理结果条数：{}", records.size());

        List<ProgressVO> voList = fillProjectName(progressConvert.entityListToVoList(records));
        fillTreeFields(voList, LocalDate.now());

        PageInfo<ProgressVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(voList);
        return Result.success(pageInfo);
    }

    @Override
    public Result<ProgressBoardVO> board(Long projectId) {
        if (projectId == null) {
            return Result.error("projectId 不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return Result.error("项目不存在，projectId = " + projectId);
        }
        LocalDate today = LocalDate.now();
        log.info("查询项目进度详情，projectId = {}，基准日期 = {}", projectId, today);

        // 按计划开始日期排，甘特图与列表保持同一顺序
        List<Progress> nodes = progressMapper.selectList(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getProjectId, projectId)
                .orderByAsc(Progress::getPlanStartDate)
                .orderByAsc(Progress::getId));

        ProgressBoardVO vo = new ProgressBoardVO();
        // 项目基础信息与进度汇总统一由口径计算器产出，和工作台/可视化管理共用一套算法
        vo.setProject(progressCalculator.build(project, nodes, progressCalculator.statusNameMap(), today));
        // 节点按父子关系组成树（顶层为根，子节点多层嵌套），缩进层级由后端算好
        Map<Long, ProjectProgressCalculator.NodeMetrics> metrics = progressCalculator.metricsOf(nodes, today);
        vo.setNodes(progressCalculator.rootsOf(metrics).stream()
                .map(metric -> toNodeVO(metric, 0))
                .collect(Collectors.toList()));
        vo.setResponsibleOptions(responsibleOptions(project));
        vo.setDescription("项目实际进度 = Σ(节点占总进度% × 节点完成度) ÷ Σ权重（父节点完成度由子节点加权汇总，"
                + "未填权重的节点按同组平分剩余份额，整组未填即均分）；计划进度 = 项目工期推进比例"
                + "（工期不完整时退化为计划结束日已到期节点占比）；期望值（理论进度）= 按计划工期线性推进"
                + "（今天 − 计划开始）÷（计划结束 − 计划开始），封顶 100%。"
                + "逾期 = 未完成且计划结束日早于今天（当天不算）。基准日期 " + today + "。");
        return Result.success(vo);
    }

    @Override
    public Result<List<ProgressParentOptionVO>> parentOptions(Long projectId, Long excludeId) {
        if (projectId == null) {
            return Result.error("projectId 不能为空");
        }
        List<Progress> nodes = progressMapper.selectList(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getProjectId, projectId));
        Map<Long, ProjectProgressCalculator.NodeMetrics> metrics =
                progressCalculator.metricsOf(nodes, LocalDate.now());

        List<ProgressParentOptionVO> options = new ArrayList<>();
        for (ProjectProgressCalculator.NodeMetrics root : progressCalculator.rootsOf(metrics)) {
            collectOptions(root, 0, excludeId, options);
        }
        log.info("查询上级节点候选，projectId = {}，excludeId = {}，候选 {} 条",
                projectId, excludeId, options.size());
        return Result.success(options);
    }

    /** 深度优先收集上级节点候选；碰到 excludeId 整棵子树都跳过（自己和子孙都不能当自己的上级） */
    private void collectOptions(ProjectProgressCalculator.NodeMetrics metric, int level,
                                Long excludeId, List<ProgressParentOptionVO> out) {
        if (Objects.equals(metric.getId(), excludeId)) {
            return;
        }
        ProgressParentOptionVO option = new ProgressParentOptionVO();
        option.setId(metric.getId());
        option.setName(metric.getNode().getProgressName());
        option.setLevel(level);
        out.add(option);
        for (ProjectProgressCalculator.NodeMetrics child : metric.getChildren()) {
            collectOptions(child, level + 1, excludeId, out);
        }
    }

    @Override
    public Result<ProgressVO> getById(Long id) {
        Progress entity = progressMapper.selectById(id);
        if (entity == null) {
            return Result.error("进度管理记录不存在，id = " + id);
        }
        return Result.success(fillProjectName(Collections.singletonList(progressConvert.entityToVo(entity))).get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(ProgressDTO dto) {
        if (dto.getProjectId() == null) {
            return Result.error("所属项目不能为空");
        }
        if (projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (!StringUtils.hasText(dto.getProgressName())) {
            return Result.error("进度名称不能为空");
        }
        if (StringUtils.hasText(dto.getProgressStatus()) && !isValidStatus(dto.getProgressStatus())) {
            return Result.error("进度状态取值不合法，仅支持 in-progress / completed / delayed");
        }
        String parentError = checkParent(dto.getParentId(), null, dto.getProjectId());
        if (parentError != null) {
            return Result.error(parentError);
        }
        String weightError = checkWeight(dto.getWeight());
        if (weightError != null) {
            return Result.error(weightError);
        }
        String planDateError = checkPlanDates(dto, null);
        if (planDateError != null) {
            return Result.error(planDateError);
        }

        Progress entity = progressConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getProgressStatus())) {
            entity.setProgressStatus(STATUS_IN_PROGRESS);
        }
        applyStatusLinkage(entity, null, entity.getProgressStatus());
        entity.setCreateBy(currentOperator());
        try {
            progressMapper.insert(entity);
        } catch (DataAccessException e) {
            String hint = missingColumnHint(e);
            if (hint == null) {
                throw e;
            }
            // 库结构比代码旧：本次没有数据落库，标记回滚并返回可执行提示，避免只看到一个 HTTP 500
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(hint, e);
            return Result.error(hint);
        }
        log.info("新增进度管理成功，id = {}，状态 = {}，完成度 = {}",
                entity.getId(), entity.getProgressStatus(), entity.getCompletionPercent());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, ProgressDTO dto) {
        Progress exist = progressMapper.selectById(id);
        if (exist == null) {
            return Result.error("进度管理记录不存在，id = " + id);
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (dto.getProgressName() != null && !StringUtils.hasText(dto.getProgressName())) {
            return Result.error("进度名称不能为空");
        }
        if (StringUtils.hasText(dto.getProgressStatus()) && !isValidStatus(dto.getProgressStatus())) {
            return Result.error("进度状态取值不合法，仅支持 in-progress / completed / delayed");
        }
        // 项目不在本次提交里时，层级校验用库里的 projectId 兜底
        Long projectId = dto.getProjectId() != null ? dto.getProjectId() : exist.getProjectId();
        String parentError = checkParent(dto.getParentId(), id, projectId);
        if (parentError != null) {
            return Result.error(parentError);
        }
        String weightError = checkWeight(dto.getWeight());
        if (weightError != null) {
            return Result.error(weightError);
        }
        String planDateError = checkPlanDates(dto, exist);
        if (planDateError != null) {
            return Result.error(planDateError);
        }

        // 未传字段保持 null，由 updateById 的「仅更新非空字段」语义跳过
        Progress patch = progressConvert.dtoToEntity(dto);
        patch.setId(id);
        patch.setUpdateBy(currentOperator());
        boolean clearActualEnd = applyStatusLinkage(patch, exist, dto.getProgressStatus());
        // updateById 写不了 null，parent_id 置空要靠单独的 SET（见下方）
        boolean clearParent = Boolean.TRUE.equals(dto.getTopLevel());
        if (clearParent) {
            patch.setParentId(null);
        }

        try {
            progressMapper.updateById(patch);
            if (clearActualEnd || clearParent) {
                var wrapper = Wrappers.<Progress>lambdaUpdate().eq(Progress::getId, id);
                if (clearActualEnd) {
                    wrapper.set(Progress::getActualEndDate, null);
                }
                if (clearParent) {
                    wrapper.set(Progress::getParentId, null);
                }
                progressMapper.update(null, wrapper);
            }
        } catch (DataAccessException e) {
            String hint = missingColumnHint(e);
            if (hint == null) {
                throw e;
            }
            // 同上：库结构缺列时给可执行提示，不要只吐一个 HTTP 500
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(hint, e);
            return Result.error(hint);
        }
        log.info("编辑进度管理成功，id = {}，状态 = {}，是否清空实际结束日期 = {}，是否置为顶层节点 = {}",
                id, dto.getProgressStatus(), clearActualEnd, clearParent);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        Progress exist = progressMapper.selectById(id);
        if (exist == null) {
            return Result.error("进度管理记录不存在，id = " + id);
        }
        // 子节点收纳在总节点下：删父节点时整棵子树一起删，避免留下挂不到父节点的孤儿节点
        List<Progress> siblings = progressMapper.selectList(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getProjectId, exist.getProjectId()));
        List<Long> subtree = collectSubtree(siblings, id);
        progressMapper.deleteBatchIds(subtree);
        log.info("删除进度管理成功，id = {}，连同子节点共删除 {} 条", id, subtree.size());
        return Result.success(true);
    }

    // ==================================================================
    //  内部方法
    // ==================================================================

    /**
     * 完成状态联动（与用户确认的口径）：
     * - 状态置为「已完成」→ 完成百分比锁 100；实际结束日期为空时补当天（已有实际结束日期的节点
     *   仅改名时不会被改成今天）
     * - 状态从「已完成」改回进行中/延期 → 清空实际结束日期
     *
     * @param patch     待写入实体（actualEndDate 为 null 表示本次不提供该字段）
     * @param exist     编辑前的记录，新增传 null
     * @param newStatus 本次提交的节点状态，null / 空串表示本次不改状态
     * @return 是否需要把 actual_end_date 显式置 NULL（updateById 写不了 null）
     */
    private boolean applyStatusLinkage(Progress patch, Progress exist, String newStatus) {
        if (STATUS_COMPLETED.equals(newStatus)) {
            patch.setCompletionPercent(ProjectProgressCalculator.HUNDRED);
            boolean hadActualEnd = (exist != null && exist.getActualEndDate() != null)
                    || patch.getActualEndDate() != null;
            if (!hadActualEnd) {
                patch.setActualEndDate(LocalDate.now());
            }
            return false;
        }
        if (StringUtils.hasText(newStatus)) {
            patch.setActualEndDate(null);
            return true;
        }
        return false;
    }

    /**
     * 计划结束日期不能早于计划开始日期。
     * 只在本次提交确实带了日期时校验，避免历史脏数据挡住单纯的状态修改。
     *
     * @return 错误信息，校验通过返回 null
     */
    private String checkPlanDates(ProgressDTO dto, Progress exist) {
        if (dto.getPlanStartDate() == null && dto.getPlanEndDate() == null) {
            return null;
        }
        LocalDate planStart = dto.getPlanStartDate() != null
                ? dto.getPlanStartDate() : (exist == null ? null : exist.getPlanStartDate());
        LocalDate planEnd = dto.getPlanEndDate() != null
                ? dto.getPlanEndDate() : (exist == null ? null : exist.getPlanEndDate());
        if (planStart != null && planEnd != null && planEnd.isBefore(planStart)) {
            return "计划结束日期不能早于计划开始日期";
        }
        return null;
    }

    /**
     * 节点派生结果 → 节点 VO（递归），完成/逾期/权重/期望值一律走口径计算器。
     *
     * @param metric 口径计算结果
     * @param level  层级深度，顶层为 0
     */
    private ProgressNodeVO toNodeVO(ProjectProgressCalculator.NodeMetrics metric, int level) {
        Progress node = metric.getNode();
        ProgressNodeVO vo = new ProgressNodeVO();
        vo.setId(node.getId());
        vo.setProjectId(node.getProjectId());
        vo.setParentId(node.getParentId());
        vo.setLevel(level);
        vo.setProgressName(node.getProgressName());
        vo.setProgressCode(node.getProgressCode());
        vo.setResponsiblePerson(node.getResponsiblePerson());
        vo.setPlanStartDate(node.getPlanStartDate());
        vo.setPlanEndDate(node.getPlanEndDate());
        vo.setEffectivePlanStart(metric.getEffectivePlanStart());
        vo.setEffectivePlanEnd(metric.getEffectivePlanEnd());
        vo.setActualStartDate(node.getActualStartDate());
        vo.setActualEndDate(node.getActualEndDate());
        vo.setCompletionPercent(metric.getCompletion());
        vo.setCompletionAuto(metric.isCompletionAuto());
        vo.setWeight(metric.getStoredWeight());
        vo.setEffectiveWeight(metric.getWeight());
        vo.setWeightAuto(metric.isWeightAuto());
        vo.setExpectedPercent(metric.getExpected());
        vo.setProgressStatus(node.getProgressStatus());
        vo.setProgressStatusName(progressCalculator.nodeStatusName(node.getProgressStatus()));
        vo.setRemark(node.getRemark());
        vo.setCompleted(metric.isCompleted());
        vo.setOverdue(metric.isOverdue());
        vo.setOverdueDays(metric.getOverdueDays());
        vo.setChildCount(metric.getChildren().size());
        vo.setChildren(metric.getChildren().stream()
                .map(child -> toNodeVO(child, level + 1))
                .collect(Collectors.toList()));
        return vo;
    }

    /**
     * 上级节点校验：必须存在、必须同项目、不能是自己、不能挂到自己的子孙下面（防成环）。
     * <p>parent_id 没有数据库外键，环一旦写进去，前端递归渲染会直接栈溢出，所以在入口拦死。</p>
     *
     * @param parentId  本次提交的上级节点，null 表示顶层节点（无需校验）
     * @param selfId    编辑时的自身 id，新增传 null
     * @param projectId 目标项目 id，可为 null（拿不到时跳过同项目校验）
     * @return 错误信息，校验通过返回 null
     */
    private String checkParent(Long parentId, Long selfId, Long projectId) {
        if (parentId == null) {
            return null;
        }
        if (selfId != null && parentId.equals(selfId)) {
            return "上级节点不能是节点自己";
        }
        Progress parent = progressMapper.selectById(parentId);
        if (parent == null) {
            return "上级节点不存在，parentId = " + parentId;
        }
        if (projectId != null && !projectId.equals(parent.getProjectId())) {
            return "上级节点必须属于同一个项目";
        }
        if (selfId != null && isDescendant(parentId, selfId)) {
            return "上级节点不能选自己的子节点（会形成循环层级）";
        }
        return null;
    }

    /** candidateId 是否位于 ancestorId 的子树里（沿 parent_id 向上走，环状脏数据也会停下） */
    private boolean isDescendant(Long candidateId, Long ancestorId) {
        Long cursor = candidateId;
        int guard = 0;
        while (cursor != null && guard++ < MAX_TREE_DEPTH) {
            if (cursor.equals(ancestorId)) {
                return true;
            }
            Progress node = progressMapper.selectById(cursor);
            if (node == null || node.getParentId() == null || node.getParentId().equals(cursor)) {
                return false;
            }
            cursor = node.getParentId();
        }
        return false;
    }

    /** 占总进度百分比的取值范围：不填 = 与同级平分；填了必须 > 0 且 ≤ 100 */
    private String checkWeight(BigDecimal weight) {
        if (weight == null) {
            return null;
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0
                || weight.compareTo(ProjectProgressCalculator.HUNDRED) > 0) {
            return "占总进度百分比需大于 0 且不超过 100（不填表示与同级未填节点平分）";
        }
        return null;
    }

    /** 自身 + 全部子孙节点的 id（先子后父的顺序不重要，deleteBatchIds 一次删完） */
    private List<Long> collectSubtree(List<Progress> all, Long rootId) {
        List<Long> result = new ArrayList<>();
        result.add(rootId);
        Set<Long> seen = new LinkedHashSet<>(result);
        // 节点数不多，逐层扩散即可；seen 同时挡住环状脏数据造成的死循环
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Progress node : all) {
                if (node.getId() == null || node.getParentId() == null || seen.contains(node.getId())) {
                    continue;
                }
                if (seen.contains(node.getParentId())) {
                    seen.add(node.getId());
                    result.add(node.getId());
                    grew = true;
                }
            }
        }
        return result;
    }

    /**
     * 回填列表页的树形展示字段：上级节点名称、子节点数，以及完成度/权重/期望值/逾期这些派生值。
     * <p>
     * 派生值按「这些项目下的全部节点」计算，而不是只按当前这一页：否则父节点的完成度会
     * 随着子节点是否落在本页而变来变去。真正的进度口径只有 ProjectProgressCalculator 一份。
     * </p>
     */
    private void fillTreeFields(List<ProgressVO> voList, LocalDate today) {
        if (CollectionUtils.isEmpty(voList)) {
            return;
        }
        List<Long> projectIds = voList.stream()
                .map(ProgressVO::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        List<Progress> allNodes = progressMapper.selectList(Wrappers.<Progress>lambdaQuery()
                .in(Progress::getProjectId, projectIds));
        Map<Long, ProjectProgressCalculator.NodeMetrics> metrics = progressCalculator.metricsOf(allNodes, today);
        Map<Long, String> idToName = allNodes.stream()
                .filter(node -> node.getId() != null && node.getProgressName() != null)
                .collect(Collectors.toMap(Progress::getId, Progress::getProgressName, (a, b) -> a));

        for (ProgressVO vo : voList) {
            ProjectProgressCalculator.NodeMetrics metric = metrics.get(vo.getId());
            if (metric == null) {
                continue;
            }
            vo.setParentName(vo.getParentId() == null ? null : idToName.get(vo.getParentId()));
            vo.setChildCount(metric.getChildren().size());
            vo.setCompletionPercent(metric.getCompletion());
            vo.setCompletionAuto(metric.isCompletionAuto());
            vo.setWeight(metric.getStoredWeight());
            vo.setEffectiveWeight(metric.getWeight());
            vo.setWeightAuto(metric.isWeightAuto());
            vo.setExpectedPercent(metric.getExpected());
            vo.setCompleted(metric.isCompleted());
            vo.setOverdue(metric.isOverdue());
            vo.setOverdueDays(metric.getOverdueDays());
        }
    }

    /**
     * 节点负责人候选 = 在职内部人员全量名单，并把该项目的负责人 / 成员排在名单最前（LinkedHashSet 保序）。
     * <p>
     * 不限于项目成员：节点负责人允许从全部内部人员里挑，项目自己的人只是更容易先选到。
     * </p>
     */
    private List<String> responsibleOptions(Project project) {
        Set<String> names = new LinkedHashSet<>();
        addName(names, project.getProjectLeader());
        String members = project.getProjectMembers();
        if (members != null && !members.isBlank()) {
            for (String member : members.split(MEMBER_SEPARATOR)) {
                addName(names, member);
            }
        }
        sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getStatus, USER_STATUS_ENABLED)
                        .orderByAsc(SysUser::getId))
                .forEach(user -> addName(names, user.getName()));
        return new ArrayList<>(names);
    }

    private void addName(Set<String> names, String name) {
        if (name != null && !name.isBlank()) {
            names.add(name.trim());
        }
    }

    /**
     * 写库报「Unknown column 'responsible_person'」时，说明运行的库结构比代码旧——
     * 这正是「负责人保存后列表仍是 —」最可能的成因，但默认表现只是一个 HTTP 500，排查成本高。
     * 这里把数据库异常翻译成可直接执行的提示。
     *
     * @return 提示文案；不是缺列导致的异常时返回 null（调用方原样抛出）
     */
    private String missingColumnHint(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            String msg = cause.getMessage();
            if (msg != null && msg.contains("Unknown column") && msg.contains(OWNER_COLUMN)) {
                return "数据库缺少 cm_progress." + OWNER_COLUMN + " 列，节点负责人无法保存："
                        + "请先执行 " + OWNER_COLUMN_SCRIPT + "（执行后重启后端）再重试。";
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return null;
    }

    private boolean isValidStatus(String status) {
        return STATUS_IN_PROGRESS.equals(status) || STATUS_COMPLETED.equals(status) || STATUS_DELAYED.equals(status);
    }

    /**
     * 按项目名称模糊匹配 pm_project，返回项目 id 列表。
     */
    private List<Long> findProjectIdsByName(String name) {
        if (!StringUtils.hasText(name)) {
            return Collections.emptyList();
        }
        return projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .like(Project::getProjectName, name.trim()))
                .stream().map(Project::getId).collect(Collectors.toList());
    }

    /**
     * 批量回填 VO.projectName：取记录内出现的 projectId 去 pm_project 批量查询后映射。
     */
    private List<ProgressVO> fillProjectName(List<ProgressVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }
        List<Long> projectIds = voList.stream()
                .map(ProgressVO::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return voList;
        }
        Map<Long, String> idToName = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));
        voList.forEach(vo -> vo.setProjectName(idToName.get(vo.getProjectId())));
        return voList;
    }

    private PageInfo<ProgressVO> emptyPage(long pageNum, long pageSize) {
        PageInfo<ProgressVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) pageNum);
        pageInfo.setPageSize((int) pageSize);
        pageInfo.setTotal(0);
        pageInfo.setPages(0);
        pageInfo.setList(Collections.emptyList());
        return pageInfo;
    }

    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
