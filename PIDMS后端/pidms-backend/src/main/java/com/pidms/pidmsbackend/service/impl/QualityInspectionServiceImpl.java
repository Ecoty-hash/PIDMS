package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.QualityInspectionConvert;
import com.pidms.pidmsbackend.dto.QualityInspectionDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.QualityInspection;
import com.pidms.pidmsbackend.entity.QualityInspectionQueryParam;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.QualityInspectionMapper;
import com.pidms.pidmsbackend.service.QualityInspectionService;
import com.pidms.pidmsbackend.vo.QualityInspectionVO;
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
 * 质量检查 服务实现
 */
@Slf4j
@Service
public class QualityInspectionServiceImpl implements QualityInspectionService {

    /** 检查类型：质量检查 */
    private static final String TYPE_QUALITY = "quality";

    /** 检查结果：待检查 */
    private static final String RESULT_PENDING = "pending";
    /** 检查结果：合格 */
    private static final String RESULT_QUALIFIED = "qualified";
    /** 检查结果：不合格 */
    private static final String RESULT_UNQUALIFIED = "unqualified";

    @Resource
    private QualityInspectionMapper qualityInspectionMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private QualityInspectionConvert qualityInspectionConvert;

    @Override
    public Result<PageInfo<QualityInspectionVO>> pageQuery(QualityInspectionQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询质量检查：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<QualityInspection> queryWrapper = new QueryWrapper<>();
        // 高级搜索：检查结果精确匹配
        queryWrapper.eq(StringUtils.hasText(queryParam.getInspectionResult()), "inspection_result", queryParam.getInspectionResult());
        // 高级搜索：检查日期
        queryWrapper.eq(queryParam.getInspectionDate() != null, "inspection_date", queryParam.getInspectionDate());
        // 高级搜索：检查人模糊
        queryWrapper.like(StringUtils.hasText(queryParam.getInspector()), "inspector", queryParam.getInspector());

        // 高级搜索：项目名称过滤 —— 先在 pm_project 匹配名称得到 id 集，再约束 project_id
        if (StringUtils.hasText(queryParam.getProjectName())) {
            List<Long> nameMatchIds = findProjectIdsByName(queryParam.getProjectName());
            if (nameMatchIds.isEmpty()) {
                log.info("项目名称「{}」未匹配到任何项目，返回空列表", queryParam.getProjectName());
                return Result.success(emptyPage(pageNum, pageSize));
            }
            queryWrapper.in("project_id", nameMatchIds);
        }

        queryWrapper.orderByAsc("id");

        Page<QualityInspection> page = new Page<>(pageNum, pageSize);
        Page<QualityInspection> resultPage = qualityInspectionMapper.selectPage(page, queryWrapper);
        List<QualityInspection> records = resultPage.getRecords();
        log.info("分页查询质量检查结果条数：{}", records.size());

        PageInfo<QualityInspectionVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillProjectName(qualityInspectionConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<QualityInspectionVO> getById(Long id) {
        QualityInspection entity = qualityInspectionMapper.selectById(id);
        if (entity == null) {
            return Result.error("质量检查记录不存在，id = " + id);
        }
        return Result.success(fillProjectName(Collections.singletonList(qualityInspectionConvert.entityToVo(entity))).get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(QualityInspectionDTO dto) {
        if (dto.getProjectId() == null) {
            return Result.error("所属项目不能为空");
        }
        if (projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getInspectionNo()) && !isInspectionNoUnique(dto.getInspectionNo(), null)) {
            return Result.error("检查单号已存在：" + dto.getInspectionNo());
        }
        if (StringUtils.hasText(dto.getInspectionResult()) && !isValidResult(dto.getInspectionResult())) {
            return Result.error("检查结果取值不合法，仅支持 pending / qualified / unqualified");
        }

        QualityInspection entity = qualityInspectionConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getInspectionType())) {
            entity.setInspectionType(TYPE_QUALITY);
        }
        if (!StringUtils.hasText(entity.getInspectionResult())) {
            entity.setInspectionResult(RESULT_PENDING);
        }
        entity.setCreateBy(currentOperator());
        qualityInspectionMapper.insert(entity);
        log.info("新增质量检查成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, QualityInspectionDTO dto) {
        QualityInspection exist = qualityInspectionMapper.selectById(id);
        if (exist == null) {
            return Result.error("质量检查记录不存在，id = " + id);
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getInspectionNo()) && !isInspectionNoUnique(dto.getInspectionNo(), id)) {
            return Result.error("检查单号已存在：" + dto.getInspectionNo());
        }
        if (StringUtils.hasText(dto.getInspectionResult()) && !isValidResult(dto.getInspectionResult())) {
            return Result.error("检查结果取值不合法，仅支持 pending / qualified / unqualified");
        }

        QualityInspection entity = qualityInspectionConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        qualityInspectionMapper.updateById(entity);
        log.info("编辑质量检查成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (qualityInspectionMapper.selectById(id) == null) {
            return Result.error("质量检查记录不存在，id = " + id);
        }
        int rows = qualityInspectionMapper.deleteById(id);
        log.info("删除质量检查成功，id = {}", id);
        return Result.success(rows > 0);
    }

    private boolean isValidResult(String result) {
        return RESULT_PENDING.equals(result) || RESULT_QUALIFIED.equals(result) || RESULT_UNQUALIFIED.equals(result);
    }

    private boolean isInspectionNoUnique(String inspectionNo, Long excludeId) {
        Long count = qualityInspectionMapper.selectCount(Wrappers.<QualityInspection>lambdaQuery()
                .eq(QualityInspection::getInspectionNo, inspectionNo)
                .ne(excludeId != null, QualityInspection::getId, excludeId));
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
    private List<QualityInspectionVO> fillProjectName(List<QualityInspectionVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }
        List<Long> projectIds = voList.stream()
                .map(QualityInspectionVO::getProjectId)
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

    private PageInfo<QualityInspectionVO> emptyPage(long pageNum, long pageSize) {
        PageInfo<QualityInspectionVO> pageInfo = new PageInfo<>();
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
