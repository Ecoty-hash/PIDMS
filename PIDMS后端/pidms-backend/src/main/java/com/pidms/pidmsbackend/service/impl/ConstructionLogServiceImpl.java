package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.ConstructionLogConvert;
import com.pidms.pidmsbackend.dto.ConstructionLogDTO;
import com.pidms.pidmsbackend.entity.ConstructionLog;
import com.pidms.pidmsbackend.entity.ConstructionLogQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.mapper.ConstructionLogMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.service.ConstructionLogService;
import com.pidms.pidmsbackend.vo.ConstructionLogVO;
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
 * 施工日志 服务实现
 */
@Slf4j
@Service
public class ConstructionLogServiceImpl implements ConstructionLogService {

    /** 天气：晴 */
    private static final String WEATHER_SUNNY = "sunny";
    /** 天气：多云 */
    private static final String WEATHER_CLOUDY = "cloudy";
    /** 天气：雨 */
    private static final String WEATHER_RAINY = "rainy";
    /** 天气：雪 */
    private static final String WEATHER_SNOWY = "snowy";

    @Resource
    private ConstructionLogMapper constructionLogMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ConstructionLogConvert constructionLogConvert;

    @Override
    public Result<PageInfo<ConstructionLogVO>> pageQuery(ConstructionLogQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询施工日志：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<ConstructionLog> queryWrapper = new QueryWrapper<>();
        // 高级搜索：天气精确匹配
        queryWrapper.eq(StringUtils.hasText(queryParam.getWeather()), "weather", queryParam.getWeather());
        // 高级搜索：日志日期精确匹配
        queryWrapper.eq(queryParam.getLogDate() != null, "log_date", queryParam.getLogDate());
        // 高级搜索：施工部位模糊
        queryWrapper.like(StringUtils.hasText(queryParam.getConstructionLocation()), "construction_location", queryParam.getConstructionLocation());

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

        Page<ConstructionLog> page = new Page<>(pageNum, pageSize);
        Page<ConstructionLog> resultPage = constructionLogMapper.selectPage(page, queryWrapper);
        List<ConstructionLog> records = resultPage.getRecords();
        log.info("分页查询施工日志结果条数：{}", records.size());

        PageInfo<ConstructionLogVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillProjectName(constructionLogConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<ConstructionLogVO> getById(Long id) {
        ConstructionLog entity = constructionLogMapper.selectById(id);
        if (entity == null) {
            return Result.error("施工日志不存在，id = " + id);
        }
        return Result.success(fillProjectName(Collections.singletonList(constructionLogConvert.entityToVo(entity))).get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(ConstructionLogDTO dto) {
        if (dto.getProjectId() == null) {
            return Result.error("所属项目不能为空");
        }
        if (projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (dto.getLogDate() == null) {
            return Result.error("日志日期不能为空");
        }
        if (StringUtils.hasText(dto.getWeather()) && !isValidWeather(dto.getWeather())) {
            return Result.error("天气取值不合法，仅支持 sunny / cloudy / rainy / snowy");
        }

        ConstructionLog entity = constructionLogConvert.dtoToEntity(dto);
        entity.setCreateBy(currentOperator());
        constructionLogMapper.insert(entity);
        log.info("新增施工日志成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, ConstructionLogDTO dto) {
        ConstructionLog exist = constructionLogMapper.selectById(id);
        if (exist == null) {
            return Result.error("施工日志不存在，id = " + id);
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }
        if (StringUtils.hasText(dto.getWeather()) && !isValidWeather(dto.getWeather())) {
            return Result.error("天气取值不合法，仅支持 sunny / cloudy / rainy / snowy");
        }

        ConstructionLog entity = constructionLogConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        constructionLogMapper.updateById(entity);
        log.info("编辑施工日志成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (constructionLogMapper.selectById(id) == null) {
            return Result.error("施工日志不存在，id = " + id);
        }
        int rows = constructionLogMapper.deleteById(id);
        log.info("删除施工日志成功，id = {}", id);
        return Result.success(rows > 0);
    }

    private boolean isValidWeather(String weather) {
        return WEATHER_SUNNY.equals(weather) || WEATHER_CLOUDY.equals(weather)
                || WEATHER_RAINY.equals(weather) || WEATHER_SNOWY.equals(weather);
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
    private List<ConstructionLogVO> fillProjectName(List<ConstructionLogVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }
        List<Long> projectIds = voList.stream()
                .map(ConstructionLogVO::getProjectId)
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

    private PageInfo<ConstructionLogVO> emptyPage(long pageNum, long pageSize) {
        PageInfo<ConstructionLogVO> pageInfo = new PageInfo<>();
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
