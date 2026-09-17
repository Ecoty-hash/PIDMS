package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.InternalUnitConvert;
import com.pidms.pidmsbackend.dto.InternalUnitDTO;
import com.pidms.pidmsbackend.entity.InternalUnit;
import com.pidms.pidmsbackend.entity.InternalUnitQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.InternalUnitMapper;
import com.pidms.pidmsbackend.service.InternalUnitService;
import com.pidms.pidmsbackend.vo.InternalUnitVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 内部单位 服务实现
 */
@Slf4j
@Service
public class InternalUnitServiceImpl implements InternalUnitService {

    /** unitType：branch */
    private static final String UNIT_TYPE_BRANCH = "branch";
    /** unitType：project */
    private static final String UNIT_TYPE_PROJECT = "project";
    /** unitType：department */
    private static final String UNIT_TYPE_DEPARTMENT = "department";

    /** status：enabled */
    private static final String STATUS_ENABLED = "enabled";
    /** status：disabled */
    private static final String STATUS_DISABLED = "disabled";

    @Resource
    private InternalUnitMapper internalUnitMapper;

    @Resource
    private InternalUnitConvert internalUnitConvert;

    @Override
    public Result<PageInfo<InternalUnitVO>> pageQuery(InternalUnitQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询内部单位：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<InternalUnit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getUnitName()), "unit_name", queryParam.getUnitName());
        queryWrapper.eq(StringUtils.hasText(queryParam.getUnitType()), "unit_type", queryParam.getUnitType());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("unit_code", keyword)
                        .or().like("unit_name", keyword)
                        .or().like("manager", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<InternalUnit> page = new Page<>(pageNum, pageSize);
        Page<InternalUnit> resultPage = internalUnitMapper.selectPage(page, queryWrapper);
        List<InternalUnit> records = resultPage.getRecords();
        log.info("分页查询内部单位结果条数：{}", records.size());

        PageInfo<InternalUnitVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(internalUnitConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<InternalUnitVO> getById(Long id) {
        log.info("查询内部单位详情，id：{}", id);
        InternalUnit entity = internalUnitMapper.selectById(id);
        if (entity == null) {
            return Result.error("内部单位不存在，id = " + id);
        }
        return Result.success(internalUnitConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(InternalUnitDTO dto) {
        log.info("新增内部单位，参数：{}", dto);
        if (!StringUtils.hasText(dto.getUnitName())) {
            return Result.error("单位名称不能为空");
        }
        if (StringUtils.hasText(dto.getUnitType()) && !isValidUnitType(dto.getUnitType())) {
            return Result.error("单位类型取值不合法，仅支持 branch / project / department");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        if (StringUtils.hasText(dto.getUnitCode())) {
            Long dupCount = internalUnitMapper.selectCount(Wrappers.<InternalUnit>lambdaQuery()
                    .eq(InternalUnit::getUnitCode, dto.getUnitCode()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("单位编号已存在：" + dto.getUnitCode());
            }
        }
        InternalUnit entity = internalUnitConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setCreateBy(currentOperator());
        internalUnitMapper.insert(entity);
        log.info("新增内部单位成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, InternalUnitDTO dto) {
        log.info("编辑内部单位，id：{}，参数：{}", id, dto);
        InternalUnit exist = internalUnitMapper.selectById(id);
        if (exist == null) {
            return Result.error("内部单位不存在，id = " + id);
        }
        if (dto.getUnitName() != null && !StringUtils.hasText(dto.getUnitName())) {
            return Result.error("单位名称不能为空");
        }
        if (StringUtils.hasText(dto.getUnitType()) && !isValidUnitType(dto.getUnitType())) {
            return Result.error("单位类型取值不合法，仅支持 branch / project / department");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        if (StringUtils.hasText(dto.getUnitCode())) {
            Long dupCount = internalUnitMapper.selectCount(Wrappers.<InternalUnit>lambdaQuery()
                    .eq(InternalUnit::getUnitCode, dto.getUnitCode())
                    .ne(InternalUnit::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("单位编号已存在：" + dto.getUnitCode());
            }
        }

        InternalUnit entity = internalUnitConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        internalUnitMapper.updateById(entity);
        log.info("编辑内部单位成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除内部单位，id：{}", id);
        if (internalUnitMapper.selectById(id) == null) {
            return Result.error("内部单位不存在，id = " + id);
        }
        int rows = internalUnitMapper.deleteById(id);
        log.info("删除内部单位成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 unitType 取值合法性。
     */
    private boolean isValidUnitType(String value) {
        return value == null
                || UNIT_TYPE_BRANCH.equals(value)
                || UNIT_TYPE_PROJECT.equals(value)
                || UNIT_TYPE_DEPARTMENT.equals(value);
    }

    /**
     * 校验 status 取值合法性。
     */
    private boolean isValidStatus(String value) {
        return value == null
                || STATUS_ENABLED.equals(value)
                || STATUS_DISABLED.equals(value);
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
