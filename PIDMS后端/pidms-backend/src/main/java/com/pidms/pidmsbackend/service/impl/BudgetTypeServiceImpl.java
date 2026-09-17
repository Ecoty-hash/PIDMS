package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.BudgetTypeConvert;
import com.pidms.pidmsbackend.dto.BudgetTypeDTO;
import com.pidms.pidmsbackend.entity.BudgetType;
import com.pidms.pidmsbackend.entity.BudgetTypeQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.BudgetTypeMapper;
import com.pidms.pidmsbackend.service.BudgetTypeService;
import com.pidms.pidmsbackend.vo.BudgetTypeVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 预算类型 服务实现
 */
@Slf4j
@Service
public class BudgetTypeServiceImpl implements BudgetTypeService {

    /** budgetType：labor */
    private static final String BUDGET_TYPE_LABOR = "labor";
    /** budgetType：material */
    private static final String BUDGET_TYPE_MATERIAL = "material";
    /** budgetType：equipment */
    private static final String BUDGET_TYPE_EQUIPMENT = "equipment";
    /** budgetType：expense */
    private static final String BUDGET_TYPE_EXPENSE = "expense";
    /** budgetType：subcontract */
    private static final String BUDGET_TYPE_SUBCONTRACT = "subcontract";
    /** budgetType：other */
    private static final String BUDGET_TYPE_OTHER = "other";

    /** status：enabled */
    private static final String STATUS_ENABLED = "enabled";
    /** status：sealed */
    private static final String STATUS_SEALED = "sealed";

    @Resource
    private BudgetTypeMapper budgetTypeMapper;

    @Resource
    private BudgetTypeConvert budgetTypeConvert;

    @Override
    public Result<PageInfo<BudgetTypeVO>> pageQuery(BudgetTypeQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询预算类型：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<BudgetType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getBudgetType()), "budget_type", queryParam.getBudgetType());
        queryWrapper.eq(StringUtils.hasText(queryParam.getSecondaryBudgetTypeCode()), "secondary_budget_type_code", queryParam.getSecondaryBudgetTypeCode());
        queryWrapper.like(StringUtils.hasText(queryParam.getSecondaryBudgetType()), "secondary_budget_type", queryParam.getSecondaryBudgetType());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("secondary_budget_type", keyword)
                        .or().like("secondary_budget_type_code", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<BudgetType> page = new Page<>(pageNum, pageSize);
        Page<BudgetType> resultPage = budgetTypeMapper.selectPage(page, queryWrapper);
        List<BudgetType> records = resultPage.getRecords();
        log.info("分页查询预算类型结果条数：{}", records.size());

        PageInfo<BudgetTypeVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(budgetTypeConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<BudgetTypeVO> getById(Long id) {
        log.info("查询预算类型详情，id：{}", id);
        BudgetType entity = budgetTypeMapper.selectById(id);
        if (entity == null) {
            return Result.error("预算类型不存在，id = " + id);
        }
        return Result.success(budgetTypeConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(BudgetTypeDTO dto) {
        log.info("新增预算类型，参数：{}", dto);
        if (!StringUtils.hasText(dto.getBudgetType())) {
            return Result.error("预算类型不能为空");
        }
        if (!StringUtils.hasText(dto.getSecondaryBudgetType())) {
            return Result.error("二级预算类型名称不能为空");
        }
        if (!StringUtils.hasText(dto.getSecondaryBudgetTypeCode())) {
            return Result.error("二级预算类型编码不能为空");
        }
        if (StringUtils.hasText(dto.getBudgetType()) && !isValidBudgetType(dto.getBudgetType())) {
            return Result.error("预算类型取值不合法，仅支持 labor / material / equipment / expense / subcontract / other");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / sealed");
        }
        if (StringUtils.hasText(dto.getSecondaryBudgetTypeCode())) {
            Long dupCount = budgetTypeMapper.selectCount(Wrappers.<BudgetType>lambdaQuery()
                    .eq(BudgetType::getSecondaryBudgetTypeCode, dto.getSecondaryBudgetTypeCode()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("二级预算类型编码已存在：" + dto.getSecondaryBudgetTypeCode());
            }
        }
        if (StringUtils.hasText(dto.getSecondaryBudgetTypeCode())
                && !BUDGET_TYPE_CODE_PATTERN.matcher(dto.getSecondaryBudgetTypeCode()).matches()) {
            return Result.error("二级预算类型编码格式不合法，应为 XX-XX-XXX，如 RGF-GZ-001");
        }
        BudgetType entity = budgetTypeConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setCreateBy(currentOperator());
        budgetTypeMapper.insert(entity);
        log.info("新增预算类型成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, BudgetTypeDTO dto) {
        log.info("编辑预算类型，id：{}，参数：{}", id, dto);
        BudgetType exist = budgetTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("预算类型不存在，id = " + id);
        }
        if (dto.getBudgetType() != null && !StringUtils.hasText(dto.getBudgetType())) {
            return Result.error("预算类型不能为空");
        }
        if (dto.getSecondaryBudgetType() != null && !StringUtils.hasText(dto.getSecondaryBudgetType())) {
            return Result.error("二级预算类型名称不能为空");
        }
        if (dto.getSecondaryBudgetTypeCode() != null && !StringUtils.hasText(dto.getSecondaryBudgetTypeCode())) {
            return Result.error("二级预算类型编码不能为空");
        }
        if (StringUtils.hasText(dto.getBudgetType()) && !isValidBudgetType(dto.getBudgetType())) {
            return Result.error("预算类型取值不合法，仅支持 labor / material / equipment / expense / subcontract / other");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / sealed");
        }
        if (StringUtils.hasText(dto.getSecondaryBudgetTypeCode())) {
            Long dupCount = budgetTypeMapper.selectCount(Wrappers.<BudgetType>lambdaQuery()
                    .eq(BudgetType::getSecondaryBudgetTypeCode, dto.getSecondaryBudgetTypeCode())
                    .ne(BudgetType::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("二级预算类型编码已存在：" + dto.getSecondaryBudgetTypeCode());
            }
        }
        if (StringUtils.hasText(dto.getSecondaryBudgetTypeCode())
                && !BUDGET_TYPE_CODE_PATTERN.matcher(dto.getSecondaryBudgetTypeCode()).matches()) {
            return Result.error("二级预算类型编码格式不合法，应为 XX-XX-XXX，如 RGF-GZ-001");
        }

        BudgetType entity = budgetTypeConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        budgetTypeMapper.updateById(entity);
        log.info("编辑预算类型成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除预算类型，id：{}", id);
        if (budgetTypeMapper.selectById(id) == null) {
            return Result.error("预算类型不存在，id = " + id);
        }
        int rows = budgetTypeMapper.deleteById(id);
        log.info("删除预算类型成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 budgetType 取值合法性。
     */
    private boolean isValidBudgetType(String value) {
        return value == null
                || BUDGET_TYPE_LABOR.equals(value)
                || BUDGET_TYPE_MATERIAL.equals(value)
                || BUDGET_TYPE_EQUIPMENT.equals(value)
                || BUDGET_TYPE_EXPENSE.equals(value)
                || BUDGET_TYPE_SUBCONTRACT.equals(value)
                || BUDGET_TYPE_OTHER.equals(value);
    }

    /**
     * 校验 status 取值合法性。
     */
    private boolean isValidStatus(String value) {
        return value == null
                || STATUS_ENABLED.equals(value)
                || STATUS_SEALED.equals(value);
    }

    /** 二级预算类型编码格式：XX-XX-XXX，如 RGF-GZ-001 */
    private static final Pattern BUDGET_TYPE_CODE_PATTERN = Pattern.compile("^[A-Z]{2,4}-[A-Z]{2,4}-\\d{3}$");

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
