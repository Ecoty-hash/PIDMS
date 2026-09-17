package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.SupplierConvert;
import com.pidms.pidmsbackend.dto.SupplierDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Supplier;
import com.pidms.pidmsbackend.entity.SupplierQueryParam;
import com.pidms.pidmsbackend.mapper.SupplierMapper;
import com.pidms.pidmsbackend.service.SupplierService;
import com.pidms.pidmsbackend.vo.SupplierVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 供应商 服务实现
 */
@Slf4j
@Service
public class SupplierServiceImpl implements SupplierService {

    /** supplierType：material */
    private static final String SUPPLIER_TYPE_MATERIAL = "material";
    /** supplierType：equipment */
    private static final String SUPPLIER_TYPE_EQUIPMENT = "equipment";
    /** supplierType：labor */
    private static final String SUPPLIER_TYPE_LABOR = "labor";

    /** status：enabled */
    private static final String STATUS_ENABLED = "enabled";
    /** status：disabled */
    private static final String STATUS_DISABLED = "disabled";

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SupplierConvert supplierConvert;

    @Override
    public Result<PageInfo<SupplierVO>> pageQuery(SupplierQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询供应商：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<Supplier> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getSupplierType()), "supplier_type", queryParam.getSupplierType());
        queryWrapper.eq(StringUtils.hasText(queryParam.getContactPerson()), "contact_person", queryParam.getContactPerson());
        queryWrapper.like(StringUtils.hasText(queryParam.getSupplierName()), "supplier_name", queryParam.getSupplierName());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("supplier_code", keyword)
                        .or().like("supplier_name", keyword)
                        .or().like("contact_person", keyword)
                        .or().like("contact_phone", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<Supplier> page = new Page<>(pageNum, pageSize);
        Page<Supplier> resultPage = supplierMapper.selectPage(page, queryWrapper);
        List<Supplier> records = resultPage.getRecords();
        log.info("分页查询供应商结果条数：{}", records.size());

        PageInfo<SupplierVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(supplierConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<SupplierVO> getById(Long id) {
        log.info("查询供应商详情，id：{}", id);
        Supplier entity = supplierMapper.selectById(id);
        if (entity == null) {
            return Result.error("供应商不存在，id = " + id);
        }
        return Result.success(supplierConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(SupplierDTO dto) {
        log.info("新增供应商，参数：{}", dto);
        if (!StringUtils.hasText(dto.getSupplierName())) {
            return Result.error("供应商名称不能为空");
        }
        if (!StringUtils.hasText(dto.getSupplierType())) {
            return Result.error("供应商类型不能为空");
        }
        if (StringUtils.hasText(dto.getSupplierType()) && !isValidSupplierType(dto.getSupplierType())) {
            return Result.error("供应商类型取值不合法，仅支持 material / equipment / labor");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        if (StringUtils.hasText(dto.getSupplierCode())) {
            Long dupCount = supplierMapper.selectCount(Wrappers.<Supplier>lambdaQuery()
                    .eq(Supplier::getSupplierCode, dto.getSupplierCode()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("供应商编号已存在：" + dto.getSupplierCode());
            }
        }
        Supplier entity = supplierConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setCreateBy(currentOperator());
        supplierMapper.insert(entity);
        log.info("新增供应商成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, SupplierDTO dto) {
        log.info("编辑供应商，id：{}，参数：{}", id, dto);
        Supplier exist = supplierMapper.selectById(id);
        if (exist == null) {
            return Result.error("供应商不存在，id = " + id);
        }
        if (dto.getSupplierName() != null && !StringUtils.hasText(dto.getSupplierName())) {
            return Result.error("供应商名称不能为空");
        }
        if (dto.getSupplierType() != null && !StringUtils.hasText(dto.getSupplierType())) {
            return Result.error("供应商类型不能为空");
        }
        if (StringUtils.hasText(dto.getSupplierType()) && !isValidSupplierType(dto.getSupplierType())) {
            return Result.error("供应商类型取值不合法，仅支持 material / equipment / labor");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        if (StringUtils.hasText(dto.getSupplierCode())) {
            Long dupCount = supplierMapper.selectCount(Wrappers.<Supplier>lambdaQuery()
                    .eq(Supplier::getSupplierCode, dto.getSupplierCode())
                    .ne(Supplier::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("供应商编号已存在：" + dto.getSupplierCode());
            }
        }

        Supplier entity = supplierConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        supplierMapper.updateById(entity);
        log.info("编辑供应商成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除供应商，id：{}", id);
        if (supplierMapper.selectById(id) == null) {
            return Result.error("供应商不存在，id = " + id);
        }
        int rows = supplierMapper.deleteById(id);
        log.info("删除供应商成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 supplierType 取值合法性。
     */
    private boolean isValidSupplierType(String value) {
        return value == null
                || SUPPLIER_TYPE_MATERIAL.equals(value)
                || SUPPLIER_TYPE_EQUIPMENT.equals(value)
                || SUPPLIER_TYPE_LABOR.equals(value);
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
