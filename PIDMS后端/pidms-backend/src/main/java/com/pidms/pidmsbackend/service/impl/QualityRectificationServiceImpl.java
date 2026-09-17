package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.QualityRectificationConvert;
import com.pidms.pidmsbackend.dto.QualityRectificationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.QualityRectification;
import com.pidms.pidmsbackend.entity.QualityRectificationQueryParam;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.QualityRectificationMapper;
import com.pidms.pidmsbackend.service.QualityRectificationService;
import com.pidms.pidmsbackend.vo.QualityRectificationVO;
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
 * 质量整改单 服务实现
 */
@Slf4j
@Service
public class QualityRectificationServiceImpl implements QualityRectificationService {

    /** 整改状态：待整改 */
    private static final String STATUS_PENDING = "pending";
    /** 整改状态：整改中 */
    private static final String STATUS_PROCESSING = "processing";
    /** 整改状态：已整改 */
    private static final String STATUS_COMPLETED = "completed";

    @Resource
    private QualityRectificationMapper qualityRectificationMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private QualityRectificationConvert qualityRectificationConvert;

    @Override
    public Result<PageInfo<QualityRectificationVO>> pageQuery(QualityRectificationQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询质量整改单：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<QualityRectification> queryWrapper = new QueryWrapper<>();
        // 高级搜索：整改状态精确匹配
        queryWrapper.eq(StringUtils.hasText(queryParam.getRectificationStatus()), "rectification_status", queryParam.getRectificationStatus());
        // 高级搜索：要求完成日期
        queryWrapper.eq(queryParam.getRequiredCompleteDate() != null, "required_complete_date", queryParam.getRequiredCompleteDate());
        // 高级搜索：责任人模糊
        queryWrapper.like(StringUtils.hasText(queryParam.getResponsiblePerson()), "responsible_person", queryParam.getResponsiblePerson());

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

        Page<QualityRectification> page = new Page<>(pageNum, pageSize);
        Page<QualityRectification> resultPage = qualityRectificationMapper.selectPage(page, queryWrapper);
        List<QualityRectification> records = resultPage.getRecords();
        log.info("分页查询质量整改单结果条数：{}", records.size());

        PageInfo<QualityRectificationVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillProjectName(qualityRectificationConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<QualityRectificationVO> getById(Long id) {
        QualityRectification entity = qualityRectificationMapper.selectById(id);
        if (entity == null) {
            return Result.error("质量整改单不存在，id = " + id);
        }
        return Result.success(fillProjectName(Collections.singletonList(qualityRectificationConvert.entityToVo(entity))).get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(QualityRectificationDTO dto) {
        if (dto.getProjectId() == null) {
            return Result.error("所属项目不能为空");
        }
        if (projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getRectificationNo()) && !isRectificationNoUnique(dto.getRectificationNo(), null)) {
            return Result.error("整改单号已存在：" + dto.getRectificationNo());
        }
        if (StringUtils.hasText(dto.getRectificationStatus()) && !isValidStatus(dto.getRectificationStatus())) {
            return Result.error("整改状态取值不合法，仅支持 pending / processing / completed");
        }

        QualityRectification entity = qualityRectificationConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getRectificationStatus())) {
            entity.setRectificationStatus(STATUS_PENDING);
        }
        entity.setCreateBy(currentOperator());
        qualityRectificationMapper.insert(entity);
        log.info("新增质量整改单成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, QualityRectificationDTO dto) {
        QualityRectification exist = qualityRectificationMapper.selectById(id);
        if (exist == null) {
            return Result.error("质量整改单不存在，id = " + id);
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getRectificationNo()) && !isRectificationNoUnique(dto.getRectificationNo(), id)) {
            return Result.error("整改单号已存在：" + dto.getRectificationNo());
        }
        if (StringUtils.hasText(dto.getRectificationStatus()) && !isValidStatus(dto.getRectificationStatus())) {
            return Result.error("整改状态取值不合法，仅支持 pending / processing / completed");
        }

        QualityRectification entity = qualityRectificationConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        qualityRectificationMapper.updateById(entity);
        log.info("编辑质量整改单成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (qualityRectificationMapper.selectById(id) == null) {
            return Result.error("质量整改单不存在，id = " + id);
        }
        int rows = qualityRectificationMapper.deleteById(id);
        log.info("删除质量整改单成功，id = {}", id);
        return Result.success(rows > 0);
    }

    private boolean isValidStatus(String status) {
        return STATUS_PENDING.equals(status) || STATUS_PROCESSING.equals(status) || STATUS_COMPLETED.equals(status);
    }

    private boolean isRectificationNoUnique(String rectificationNo, Long excludeId) {
        Long count = qualityRectificationMapper.selectCount(Wrappers.<QualityRectification>lambdaQuery()
                .eq(QualityRectification::getRectificationNo, rectificationNo)
                .ne(excludeId != null, QualityRectification::getId, excludeId));
        return count == null || count == 0;
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
    private List<QualityRectificationVO> fillProjectName(List<QualityRectificationVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }
        List<Long> projectIds = voList.stream()
                .map(QualityRectificationVO::getProjectId)
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

    private PageInfo<QualityRectificationVO> emptyPage(long pageNum, long pageSize) {
        PageInfo<QualityRectificationVO> pageInfo = new PageInfo<>();
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
