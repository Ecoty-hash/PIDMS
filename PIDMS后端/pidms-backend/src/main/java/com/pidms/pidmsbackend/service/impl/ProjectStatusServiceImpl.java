package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.ProjectStatusConvert;
import com.pidms.pidmsbackend.dto.ProjectStatusDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectStatus;
import com.pidms.pidmsbackend.entity.ProjectStatusQueryParam;
import com.pidms.pidmsbackend.mapper.ProjectStatusMapper;
import com.pidms.pidmsbackend.service.ProjectStatusService;
import com.pidms.pidmsbackend.vo.ProjectStatusVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目状态字典服务
 * <p>状态取值：enabled启用 / disabled停用 / sealed封存；封存/解封走专用动作接口。</p>
 */
@Slf4j
@Service
public class ProjectStatusServiceImpl implements ProjectStatusService {

    /** 字典状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 字典状态：停用 */
    private static final String STATUS_DISABLED = "disabled";
    /** 字典状态：封存 */
    private static final String STATUS_SEALED = "sealed";

    @Resource
    private ProjectStatusMapper projectStatusMapper;
    @Resource
    private ProjectStatusConvert projectStatusConvert;

    @Override
    public Result<PageInfo<ProjectStatusVO>> pageQuery(ProjectStatusQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询项目状态：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<ProjectStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());
        // 关键字：状态名称 / 状态编码
        if (StringUtils.hasText(queryParam.getKeyword())) {
            queryWrapper.nested(wrapper -> wrapper
                    .like("status_name", queryParam.getKeyword())
                    .or()
                    .like("status_code", queryParam.getKeyword())
            );
        }
        queryWrapper.orderByAsc("id");

        Page<ProjectStatus> page = new Page<>(pageNum, pageSize);
        Page<ProjectStatus> resultPage = projectStatusMapper.selectPage(page, queryWrapper);
        List<ProjectStatus> records = resultPage.getRecords();
        log.info("分页查询项目状态结果条数：{}", records.size());

        PageInfo<ProjectStatusVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) pageNum);
        pageInfo.setPageSize((int) pageSize);
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(projectStatusConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<ProjectStatusVO> getById(Long id) {
        ProjectStatus entity = projectStatusMapper.selectById(id);
        if (entity == null) {
            return Result.error("项目状态不存在，id = " + id);
        }
        return Result.success(projectStatusConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(ProjectStatusDTO dto) {
        if (!StringUtils.hasText(dto.getStatusName())) {
            return Result.error("项目状态名称不能为空");
        }
        // 状态名称唯一
        Long dupCount = projectStatusMapper.selectCount(Wrappers.<ProjectStatus>lambdaQuery()
                .eq(ProjectStatus::getStatusName, dto.getStatusName()));
        if (dupCount != null && dupCount > 0) {
            return Result.error("项目状态名称已存在：" + dto.getStatusName());
        }
        // 状态编码必填且唯一：项目模块按 statusCode 引用该字典
        if (!StringUtils.hasText(dto.getStatusCode())) {
            return Result.error("状态编码不能为空，如：planning / in-progress / halted");
        }
        dto.setStatusCode(dto.getStatusCode().trim());
        Long codeDup = projectStatusMapper.selectCount(Wrappers.<ProjectStatus>lambdaQuery()
                .eq(ProjectStatus::getStatusCode, dto.getStatusCode()));
        if (codeDup != null && codeDup > 0) {
            return Result.error("状态编码已存在：" + dto.getStatusCode());
        }

        ProjectStatus entity = projectStatusConvert.dtoToEntity(dto);
        // 未传状态时默认启用；新增不允许直接落「封存」
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        // 兼容旧数据 legacy 状态码（active/inactive），保存时归一为标准码
        entity.setStatus(normalizeStatus(entity.getStatus()));
        if (!STATUS_ENABLED.equals(entity.getStatus()) && !STATUS_DISABLED.equals(entity.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled");
        }
        entity.setCreateBy(currentOperator());
        projectStatusMapper.insert(entity);
        log.info("新增项目状态成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    /**
     * 旧数据 legacy 状态码归一为标准码（active→enabled / inactive→disabled）。
     * 保证历史遗留行在「编辑保存」时不会因状态非法而 500，同时落库即完成数据修复。
     */
    private String normalizeStatus(String status) {
        if (STATUS_ENABLED.equals(status) || STATUS_DISABLED.equals(status) || STATUS_SEALED.equals(status)) {
            return status;
        }
        if ("active".equals(status)) {
            return STATUS_ENABLED;
        }
        if ("inactive".equals(status)) {
            return STATUS_DISABLED;
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, ProjectStatusDTO dto) {
        ProjectStatus exist = projectStatusMapper.selectById(id);
        if (exist == null) {
            return Result.error("项目状态不存在，id = " + id);
        }
        if (StringUtils.hasText(dto.getStatusName())) {
            Long dupCount = projectStatusMapper.selectCount(Wrappers.<ProjectStatus>lambdaQuery()
                    .eq(ProjectStatus::getStatusName, dto.getStatusName())
                    .ne(ProjectStatus::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("项目状态名称已存在：" + dto.getStatusName());
            }
        }
        // 兼容旧数据 legacy 状态码（active/inactive），保存时归一为标准码
        if (StringUtils.hasText(dto.getStatus())) {
            dto.setStatus(normalizeStatus(dto.getStatus()));
        }
        // 状态编码若提供则 trim 并校验唯一（编辑允许不传，保留原值；存量无编码行由升级 SQL 补齐）
        if (StringUtils.hasText(dto.getStatusCode())) {
            dto.setStatusCode(dto.getStatusCode().trim());
            Long codeDup = projectStatusMapper.selectCount(Wrappers.<ProjectStatus>lambdaQuery()
                    .eq(ProjectStatus::getStatusCode, dto.getStatusCode())
                    .ne(ProjectStatus::getId, id));
            if (codeDup != null && codeDup > 0) {
                return Result.error("状态编码已存在：" + dto.getStatusCode());
            }
        }
        if (StringUtils.hasText(dto.getStatus())
                && !STATUS_ENABLED.equals(dto.getStatus())
                && !STATUS_DISABLED.equals(dto.getStatus())
                && !STATUS_SEALED.equals(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled/sealed");
        }

        ProjectStatus entity = projectStatusConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传 statusName/status/statusCode 时保留原值
        projectStatusMapper.updateById(entity);
        log.info("编辑项目状态成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (projectStatusMapper.selectById(id) == null) {
            return Result.error("项目状态不存在，id = " + id);
        }
        int rows = projectStatusMapper.deleteById(id);
        log.info("删除项目状态成功，id = {}", id);
        return Result.success(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> seal(Long id) {
        ProjectStatus exist = projectStatusMapper.selectById(id);
        if (exist == null) {
            return Result.error("项目状态不存在，id = " + id);
        }
        if (STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该项目状态已封存，无需重复操作");
        }
        projectStatusMapper.update(null, Wrappers.<ProjectStatus>lambdaUpdate()
                .eq(ProjectStatus::getId, id)
                .set(ProjectStatus::getStatus, STATUS_SEALED)
                .set(ProjectStatus::getUpdateBy, currentOperator()));
        log.info("封存项目状态成功，id = {}", id);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> unseal(Long id) {
        ProjectStatus exist = projectStatusMapper.selectById(id);
        if (exist == null) {
            return Result.error("项目状态不存在，id = " + id);
        }
        if (!STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该项目状态未封存，无需解封");
        }
        projectStatusMapper.update(null, Wrappers.<ProjectStatus>lambdaUpdate()
                .eq(ProjectStatus::getId, id)
                .set(ProjectStatus::getStatus, STATUS_ENABLED)
                .set(ProjectStatus::getUpdateBy, currentOperator()));
        log.info("解封项目状态成功，id = {}", id);
        return Result.success(true);
    }

    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
