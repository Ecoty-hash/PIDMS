package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.convert.ProjectBudgetAddConvert;
import com.pidms.pidmsbackend.convert.ProjectBudgetConvert;
import com.pidms.pidmsbackend.dto.ProjectBudgetDTO;
// import com.pidms.pidmsbackend.entity.BudgetItem;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.ProjectBudget;
import com.pidms.pidmsbackend.entity.ProjectBudgetQueryParam;
// import com.pidms.pidmsbackend.mapper.ProjectBudgetItemMapper;
import com.pidms.pidmsbackend.mapper.ProjectBudgetMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.service.ProjectBudgetService;
import com.pidms.pidmsbackend.vo.ProjectBudgetVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 项目预算服务（一期平面预算）
 * <p>
 * 一条项目预算 = pm_project_budget 主表一行，六大类分项金额直接存主表；
 * pm_project_budget_item 一期预留不读写，本类不再聚合/写入明细表。
 */
@Slf4j
@Service
public class ProjectBudgetServiceImpl implements ProjectBudgetService {

    @Resource
    private ProjectBudgetMapper projectBudgetMapper;
    @Resource
    private ProjectMapper projectMapper;
    // 一期平面预算：不读写明细子表，故不注入子表 Mapper（二期启用明细时再放开）
    // @Resource
    // private ProjectBudgetItemMapper projectBudgetItemMapper;

    @Override
    public Result<PageInfo<ProjectBudgetVO>> pageQuery(ProjectBudgetQueryParam projectBudgetQueryParam) {
        long pageNum = projectBudgetQueryParam.getPage() == null ? 1 : projectBudgetQueryParam.getPage();
        long pageSize = projectBudgetQueryParam.getPageSize() == null ? 20 : projectBudgetQueryParam.getPageSize();
        log.info("分页查询项目预算：pageNum = {}, pageSize = {}", pageNum, pageSize);

        QueryWrapper<ProjectBudget> queryWrapper = new QueryWrapper<>();

        // 项目名称筛选：先在项目表模糊匹配项目名，再按 project_id 过滤（预算主表不冗余项目名）
        if (StringUtils.hasText(projectBudgetQueryParam.getProjectName())) {
            List<Project> matchedProjects = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                    .like(Project::getProjectName, projectBudgetQueryParam.getProjectName())
                    .select(Project::getId));
            List<Long> matchedIds = matchedProjects.stream().map(Project::getId).toList();
            queryWrapper.in("project_id", matchedIds.isEmpty() ? List.of(-1L) : matchedIds);
        }

        // 精确条件筛选
        queryWrapper.eq(StringUtils.hasText(projectBudgetQueryParam.getBudgetVersion()), "budget_version", projectBudgetQueryParam.getBudgetVersion());
        queryWrapper.eq(StringUtils.hasText(projectBudgetQueryParam.getApprovalStatus()), "approval_status", projectBudgetQueryParam.getApprovalStatus());
        queryWrapper.eq(StringUtils.hasText(projectBudgetQueryParam.getRevisionStatus()), "revision_status", projectBudgetQueryParam.getRevisionStatus());
        queryWrapper.eq(StringUtils.hasText(projectBudgetQueryParam.getCreateBy()), "create_by", projectBudgetQueryParam.getCreateBy());

        // 创建时间筛选
        if (projectBudgetQueryParam.getCreateTime() != null) {
            queryWrapper.apply("date(create_time) = {0}", projectBudgetQueryParam.getCreateTime());
        }

        // 全局关键字模糊搜索（二级预算汇单/备注）
        if (StringUtils.hasText(projectBudgetQueryParam.getKeyword())) {
            queryWrapper.nested(wrapper -> wrapper
                    .like("secondary_budget_summary", projectBudgetQueryParam.getKeyword())
                    .or()
                    .like("remark", projectBudgetQueryParam.getKeyword())
            );
        }

        log.info("查询语句： {}", queryWrapper.getSqlSegment());

        // MyBatis-Plus 分页查询主表
        Page<ProjectBudget> page = new Page<>(pageNum, pageSize);
        Page<ProjectBudget> projectBudgetPage = projectBudgetMapper.selectPage(page, queryWrapper);
        List<ProjectBudget> records = projectBudgetPage.getRecords();
        log.info("分页查询结果条数： {}", records.size());

        PageInfo<ProjectBudgetVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) pageNum);
        pageInfo.setPageSize((int) pageSize);
        pageInfo.setTotal(projectBudgetPage.getTotal());
        pageInfo.setPages((int) ((projectBudgetPage.getTotal() + pageSize - 1) / pageSize));

        if (records.isEmpty()) {
            pageInfo.setList(List.of());
            return Result.success(pageInfo);
        }

        // 批量查项目表，组装 id → 项目名称 map（填充列表 projectName 列）
        Set<Long> projectIdSet = records.stream()
                .map(ProjectBudget::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> projectNameMap = new HashMap<>();
        if (!projectIdSet.isEmpty()) {
            projectMapper.selectBatchIds(projectIdSet)
                    .forEach(project -> projectNameMap.put(project.getId(), project.getProjectName()));
        }

        // 主表字段已含六类分项金额，直接转换回填项目名称，无需再聚合明细表
        List<ProjectBudgetVO> voList = records.stream()
                .map(ProjectBudgetConvert.INSTANCE::entityToVo)
                .peek(vo -> vo.setProjectName(projectNameMap.get(vo.getProjectId())))
                .toList();

        pageInfo.setList(voList);
        return Result.success(pageInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> addBudget(ProjectBudgetDTO projectBudgetDTO) {
        // 校验项目名称，回填 projectId
        Project project = resolveProject(projectBudgetDTO.getProjectName());
        if (project == null) {
            return Result.error("项目名称不存在：" + projectBudgetDTO.getProjectName());
        }

        // DTO → Entity（六类分项金额直接映射到主表字段）
        ProjectBudget projectBudget = ProjectBudgetAddConvert.INSTANCE.addDtoToEntity(projectBudgetDTO);
        projectBudget.setProjectId(project.getId());
        // 预算总额由后端按六类分项求和，忽略客户端传值
        projectBudget.setBudgetTotal(sumCategoryBudgets(projectBudget));

        projectBudgetMapper.insert(projectBudget);
        log.info("新增项目预算成功，id = {}", projectBudget.getId());
        return Result.success(projectBudget.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> updateBudget(Long id, ProjectBudgetDTO projectBudgetDTO) {
        ProjectBudget exist = projectBudgetMapper.selectById(id);
        if (exist == null) {
            return Result.error("项目预算不存在，id = " + id);
        }

        ProjectBudget projectBudget = ProjectBudgetAddConvert.INSTANCE.addDtoToEntity(projectBudgetDTO);
        // 若传了项目名称则重新解析项目，否则保留原项目
        if (StringUtils.hasText(projectBudgetDTO.getProjectName())) {
            Project project = resolveProject(projectBudgetDTO.getProjectName());
            if (project == null) {
                return Result.error("项目名称不存在：" + projectBudgetDTO.getProjectName());
            }
            projectBudget.setProjectId(project.getId());
        } else {
            projectBudget.setProjectId(exist.getProjectId());
        }
        projectBudget.setBudgetTotal(sumCategoryBudgets(projectBudget));

        // 显式 SET：允许把六类分项/二级汇单/备注等清空为 NULL；
        // 审批状态、修订状态、版本仅当请求体传值时更新，避免编辑误改审批流。
        var updateWrapper = Wrappers.<ProjectBudget>lambdaUpdate()
                .eq(ProjectBudget::getId, id)
                .set(ProjectBudget::getProjectId, projectBudget.getProjectId())
                .set(ProjectBudget::getBudgetTotal, projectBudget.getBudgetTotal())
                .set(ProjectBudget::getLaborBudget, projectBudget.getLaborBudget())
                .set(ProjectBudget::getMaterialBudget, projectBudget.getMaterialBudget())
                .set(ProjectBudget::getEquipmentBudget, projectBudget.getEquipmentBudget())
                .set(ProjectBudget::getExpenseBudget, projectBudget.getExpenseBudget())
                .set(ProjectBudget::getSubcontractBudget, projectBudget.getSubcontractBudget())
                .set(ProjectBudget::getOtherBudget, projectBudget.getOtherBudget())
                .set(ProjectBudget::getSecondaryBudgetSummary, projectBudget.getSecondaryBudgetSummary())
                .set(ProjectBudget::getApprover, projectBudget.getApprover())
                .set(ProjectBudget::getCcPerson, projectBudget.getCcPerson())
                .set(ProjectBudget::getRemark, projectBudget.getRemark());
        if (StringUtils.hasText(projectBudgetDTO.getBudgetVersion())) {
            updateWrapper.set(ProjectBudget::getBudgetVersion, projectBudgetDTO.getBudgetVersion());
        }
        if (StringUtils.hasText(projectBudgetDTO.getApprovalStatus())) {
            updateWrapper.set(ProjectBudget::getApprovalStatus, projectBudgetDTO.getApprovalStatus());
        }
        if (StringUtils.hasText(projectBudgetDTO.getRevisionStatus())) {
            updateWrapper.set(ProjectBudget::getRevisionStatus, projectBudgetDTO.getRevisionStatus());
        }

        projectBudgetMapper.update(null, updateWrapper);
        log.info("编辑项目预算成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteBudget(Long id) {
        if (projectBudgetMapper.selectById(id) == null) {
            return Result.error("项目预算不存在，id = " + id);
        }
        // 一期平面预算：明细子表业务不读写。
        // 数据库外键 fk_budget_item 已配置 ON DELETE CASCADE，删除主表会自动级联清理明细，
        // 无需在此手动删除（二期启用明细读写后再放开下面注释行）。
        // projectBudgetItemMapper.delete(Wrappers.<BudgetItem>lambdaQuery().eq(BudgetItem::getBudgetId, id));
        int rows = projectBudgetMapper.deleteById(id);
        return Result.success(rows > 0);
    }

    @Override
    public Result<ProjectBudgetVO> getById(Long id) {
        ProjectBudget projectBudget = projectBudgetMapper.selectById(id);
        if (projectBudget == null) {
            return Result.error("项目预算不存在，id = " + id);
        }
        ProjectBudgetVO vo = ProjectBudgetConvert.INSTANCE.entityToVo(projectBudget);
        if (projectBudget.getProjectId() != null) {
            Project project = projectMapper.selectById(projectBudget.getProjectId());
            if (project != null) {
                vo.setProjectName(project.getProjectName());
            }
        }
        return Result.success(vo);
    }

    // ---------- 私有工具 ----------

    /**
     * 按项目名称精确查找项目（取第一条，避免同名项目导致 TooManyResults）
     */
    private Project resolveProject(String projectName) {
        if (!StringUtils.hasText(projectName)) {
            return null;
        }
        return projectMapper.selectOne(Wrappers.<Project>lambdaQuery()
                .eq(Project::getProjectName, projectName)
                .last("LIMIT 1"));
    }

    /**
     * 六类分项金额求和（null 视为 0）
     */
    private BigDecimal sumCategoryBudgets(ProjectBudget budget) {
        return Stream.of(
                        budget.getLaborBudget(),
                        budget.getMaterialBudget(),
                        budget.getEquipmentBudget(),
                        budget.getExpenseBudget(),
                        budget.getSubcontractBudget(),
                        budget.getOtherBudget())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
