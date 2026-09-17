package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.WarningRuleConvert;
import com.pidms.pidmsbackend.dto.WarningRuleDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.WarningRule;
import com.pidms.pidmsbackend.entity.WarningRuleQueryParam;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.WarningRuleMapper;
import com.pidms.pidmsbackend.service.WarningRuleService;
import com.pidms.pidmsbackend.vo.WarningRuleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 预警规则 服务实现
 */
@Slf4j
@Service
public class WarningRuleServiceImpl implements WarningRuleService {

    /** 状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 状态：禁用 */
    private static final String STATUS_DISABLED = "disabled";

    @Resource
    private WarningRuleMapper warningRuleMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private WarningRuleConvert warningRuleConvert;

    @Override
    public Result<PageInfo<WarningRuleVO>> pageQuery(WarningRuleQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询预警规则：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<WarningRule> queryWrapper = new QueryWrapper<>();
        // 高级搜索：规则名称模糊
        queryWrapper.like(StringUtils.hasText(queryParam.getRuleName()), "rule_name", queryParam.getRuleName());
        // 高级搜索：预警类型精确匹配
        queryWrapper.eq(StringUtils.hasText(queryParam.getWarningType()), "warning_type", queryParam.getWarningType());
        // 高级搜索：状态精确匹配
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());

        // 高级搜索：项目名称过滤
        if (StringUtils.hasText(queryParam.getProjectName())) {
            List<Long> nameMatchIds = findProjectIdsByName(queryParam.getProjectName());
            if (nameMatchIds.isEmpty()) {
                log.info("项目名称「{}」未匹配到任何项目，返回空列表", queryParam.getProjectName());
                return Result.success(emptyPage(pageNum, pageSize));
            }
            queryWrapper.in("project_id", nameMatchIds);
        }

        queryWrapper.orderByAsc("id");

        Page<WarningRule> page = new Page<>(pageNum, pageSize);
        Page<WarningRule> resultPage = warningRuleMapper.selectPage(page, queryWrapper);
        List<WarningRule> records = resultPage.getRecords();
        log.info("分页查询预警规则结果条数：{}", records.size());

        PageInfo<WarningRuleVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillProjectName(warningRuleConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<WarningRuleVO> getById(Long id) {
        WarningRule entity = warningRuleMapper.selectById(id);
        if (entity == null) {
            return Result.error("预警规则不存在，id = " + id);
        }
        return Result.success(fillProjectName(Collections.singletonList(warningRuleConvert.entityToVo(entity))).get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(WarningRuleDTO dto) {
        if (!StringUtils.hasText(dto.getRuleName())) {
            return Result.error("规则名称不能为空");
        }
        if (dto.getProjectId() == null) {
            return Result.error("对应项目不能为空");
        }
        if (projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("对应项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }

        WarningRule entity = warningRuleConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        entity.setCreateBy(currentOperator());
        warningRuleMapper.insert(entity);
        log.info("新增预警规则成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, WarningRuleDTO dto) {
        WarningRule exist = warningRuleMapper.selectById(id);
        if (exist == null) {
            return Result.error("预警规则不存在，id = " + id);
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("对应项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }

        WarningRule entity = warningRuleConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        warningRuleMapper.updateById(entity);
        log.info("编辑预警规则成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (warningRuleMapper.selectById(id) == null) {
            return Result.error("预警规则不存在，id = " + id);
        }
        int rows = warningRuleMapper.deleteById(id);
        log.info("删除预警规则成功，id = {}", id);
        return Result.success(rows > 0);
    }

    private boolean isValidStatus(String status) {
        return STATUS_ENABLED.equals(status) || STATUS_DISABLED.equals(status);
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
    private List<WarningRuleVO> fillProjectName(List<WarningRuleVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }
        List<Long> projectIds = voList.stream()
                .map(WarningRuleVO::getProjectId)
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

    private PageInfo<WarningRuleVO> emptyPage(long pageNum, long pageSize) {
        PageInfo<WarningRuleVO> pageInfo = new PageInfo<>();
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
