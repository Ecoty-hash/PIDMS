package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.WorkTypeConvert;
import com.pidms.pidmsbackend.dto.WorkTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.WorkType;
import com.pidms.pidmsbackend.entity.WorkTypeQueryParam;
import com.pidms.pidmsbackend.mapper.WorkTypeMapper;
import com.pidms.pidmsbackend.service.WorkTypeService;
import com.pidms.pidmsbackend.vo.WorkTypeVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工种类型 服务实现
 * <p>状态：enabled启用 / disabled停用 / sealed封存，封存与解封走 /seal、/unseal 动作接口。</p>
 */
@Slf4j
@Service
public class WorkTypeServiceImpl implements WorkTypeService {

    /** status：enabled */
    private static final String STATUS_ENABLED = "enabled";
    /** status：disabled */
    private static final String STATUS_DISABLED = "disabled";
    /** status：sealed */
    private static final String STATUS_SEALED = "sealed";

    @Resource
    private WorkTypeMapper workTypeMapper;

    @Resource
    private WorkTypeConvert workTypeConvert;

    @Override
    public Result<PageInfo<WorkTypeVO>> pageQuery(WorkTypeQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询工种类型：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<WorkType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            queryWrapper.like("type_name", queryParam.getKeyword().trim());
        }
        queryWrapper.orderByAsc("id");

        Page<WorkType> page = new Page<>(pageNum, pageSize);
        Page<WorkType> resultPage = workTypeMapper.selectPage(page, queryWrapper);
        List<WorkType> records = resultPage.getRecords();
        log.info("分页查询工种类型结果条数：{}", records.size());

        PageInfo<WorkTypeVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(workTypeConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<WorkTypeVO> getById(Long id) {
        log.info("查询工种类型详情，id：{}", id);
        WorkType entity = workTypeMapper.selectById(id);
        if (entity == null) {
            return Result.error("工种类型不存在，id = " + id);
        }
        return Result.success(workTypeConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(WorkTypeDTO dto) {
        log.info("新增工种类型，参数：{}", dto);
        if (!StringUtils.hasText(dto.getTypeName())) {
            return Result.error("工种类型名称不能为空");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled / sealed");
        }
        if (StringUtils.hasText(dto.getTypeName())) {
            Long dupCount = workTypeMapper.selectCount(Wrappers.<WorkType>lambdaQuery()
                    .eq(WorkType::getTypeName, dto.getTypeName()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("工种类型名称已存在：" + dto.getTypeName());
            }
        }
        WorkType entity = workTypeConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setCreateBy(currentOperator());
        workTypeMapper.insert(entity);
        log.info("新增工种类型成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, WorkTypeDTO dto) {
        log.info("编辑工种类型，id：{}，参数：{}", id, dto);
        WorkType exist = workTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("工种类型不存在，id = " + id);
        }
        if (dto.getTypeName() != null && !StringUtils.hasText(dto.getTypeName())) {
            return Result.error("工种类型名称不能为空");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled / sealed");
        }
        if (StringUtils.hasText(dto.getTypeName())) {
            Long dupCount = workTypeMapper.selectCount(Wrappers.<WorkType>lambdaQuery()
                    .eq(WorkType::getTypeName, dto.getTypeName())
                    .ne(WorkType::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("工种类型名称已存在：" + dto.getTypeName());
            }
        }

        WorkType entity = workTypeConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        workTypeMapper.updateById(entity);
        log.info("编辑工种类型成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除工种类型，id：{}", id);
        if (workTypeMapper.selectById(id) == null) {
            return Result.error("工种类型不存在，id = " + id);
        }
        int rows = workTypeMapper.deleteById(id);
        log.info("删除工种类型成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 status 取值合法性。
     */
    private boolean isValidStatus(String value) {
        return value == null
                || STATUS_ENABLED.equals(value)
                || STATUS_DISABLED.equals(value)
                || STATUS_SEALED.equals(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> seal(Long id) {
        WorkType exist = workTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("工种类型不存在，id = " + id);
        }
        if (STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该工种类型已封存，无需重复操作");
        }
        workTypeMapper.update(null, Wrappers.<WorkType>lambdaUpdate()
                .eq(WorkType::getId, id)
                .set(WorkType::getStatus, STATUS_SEALED)
                .set(WorkType::getUpdateBy, currentOperator()));
        log.info("封存工种类型成功，id = {}", id);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> unseal(Long id) {
        WorkType exist = workTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("工种类型不存在，id = " + id);
        }
        if (!STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该工种类型未封存，无需解封");
        }
        workTypeMapper.update(null, Wrappers.<WorkType>lambdaUpdate()
                .eq(WorkType::getId, id)
                .set(WorkType::getStatus, STATUS_ENABLED)
                .set(WorkType::getUpdateBy, currentOperator()));
        log.info("解封工种类型成功，id = {}", id);
        return Result.success(true);
    }

    /**
     * 当前操作人：取登录上下文中的登录名；未登录（如本地直连调试）时返回 null。
     */
    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
