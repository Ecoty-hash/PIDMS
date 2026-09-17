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
import com.pidms.pidmsbackend.mapper.ProgressMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.service.ProgressService;
import com.pidms.pidmsbackend.vo.ProgressVO;
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
 * 进度管理 服务实现
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

    @Resource
    private ProgressMapper progressMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProgressConvert progressConvert;

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

        PageInfo<ProgressVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillProjectName(progressConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
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

        Progress entity = progressConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getProgressStatus())) {
            entity.setProgressStatus(STATUS_IN_PROGRESS);
        }
        entity.setCreateBy(currentOperator());
        progressMapper.insert(entity);
        log.info("新增进度管理成功，id = {}", entity.getId());
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

        Progress entity = progressConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        progressMapper.updateById(entity);
        log.info("编辑进度管理成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (progressMapper.selectById(id) == null) {
            return Result.error("进度管理记录不存在，id = " + id);
        }
        int rows = progressMapper.deleteById(id);
        log.info("删除进度管理成功，id = {}", id);
        return Result.success(rows > 0);
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
