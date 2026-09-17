package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.CustomerConvert;
import com.pidms.pidmsbackend.dto.CustomerDTO;
import com.pidms.pidmsbackend.entity.Customer;
import com.pidms.pidmsbackend.entity.CustomerQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.CustomerMapper;
import com.pidms.pidmsbackend.service.CustomerService;
import com.pidms.pidmsbackend.vo.CustomerVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户 服务实现
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    /** customerType：government */
    private static final String CUSTOMER_TYPE_GOVERNMENT = "government";
    /** customerType：enterprise */
    private static final String CUSTOMER_TYPE_ENTERPRISE = "enterprise";
    /** customerType：state-owned */
    private static final String CUSTOMER_TYPE_STATE_OWNED = "state-owned";

    /** status：enabled */
    private static final String STATUS_ENABLED = "enabled";
    /** status：disabled */
    private static final String STATUS_DISABLED = "disabled";

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private CustomerConvert customerConvert;

    @Override
    public Result<PageInfo<CustomerVO>> pageQuery(CustomerQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询客户：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<Customer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getCustomerType()), "customer_type", queryParam.getCustomerType());
        queryWrapper.eq(StringUtils.hasText(queryParam.getContactPerson()), "contact_person", queryParam.getContactPerson());
        queryWrapper.like(StringUtils.hasText(queryParam.getCustomerName()), "customer_name", queryParam.getCustomerName());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("customer_code", keyword)
                        .or().like("customer_name", keyword)
                        .or().like("contact_person", keyword)
                        .or().like("contact_phone", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<Customer> page = new Page<>(pageNum, pageSize);
        Page<Customer> resultPage = customerMapper.selectPage(page, queryWrapper);
        List<Customer> records = resultPage.getRecords();
        log.info("分页查询客户结果条数：{}", records.size());

        PageInfo<CustomerVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(customerConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<CustomerVO> getById(Long id) {
        log.info("查询客户详情，id：{}", id);
        Customer entity = customerMapper.selectById(id);
        if (entity == null) {
            return Result.error("客户不存在，id = " + id);
        }
        return Result.success(customerConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(CustomerDTO dto) {
        log.info("新增客户，参数：{}", dto);
        if (!StringUtils.hasText(dto.getCustomerName())) {
            return Result.error("客户名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCustomerType())) {
            return Result.error("客户类型不能为空");
        }
        if (StringUtils.hasText(dto.getCustomerType()) && !isValidCustomerType(dto.getCustomerType())) {
            return Result.error("客户类型取值不合法，仅支持 government / enterprise / state-owned");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        if (StringUtils.hasText(dto.getCustomerCode())) {
            Long dupCount = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                    .eq(Customer::getCustomerCode, dto.getCustomerCode()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("客户编号已存在：" + dto.getCustomerCode());
            }
        }
        Customer entity = customerConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setCreateBy(currentOperator());
        customerMapper.insert(entity);
        log.info("新增客户成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, CustomerDTO dto) {
        log.info("编辑客户，id：{}，参数：{}", id, dto);
        Customer exist = customerMapper.selectById(id);
        if (exist == null) {
            return Result.error("客户不存在，id = " + id);
        }
        if (dto.getCustomerName() != null && !StringUtils.hasText(dto.getCustomerName())) {
            return Result.error("客户名称不能为空");
        }
        if (dto.getCustomerType() != null && !StringUtils.hasText(dto.getCustomerType())) {
            return Result.error("客户类型不能为空");
        }
        if (StringUtils.hasText(dto.getCustomerType()) && !isValidCustomerType(dto.getCustomerType())) {
            return Result.error("客户类型取值不合法，仅支持 government / enterprise / state-owned");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        if (StringUtils.hasText(dto.getCustomerCode())) {
            Long dupCount = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                    .eq(Customer::getCustomerCode, dto.getCustomerCode())
                    .ne(Customer::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("客户编号已存在：" + dto.getCustomerCode());
            }
        }

        Customer entity = customerConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        customerMapper.updateById(entity);
        log.info("编辑客户成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除客户，id：{}", id);
        if (customerMapper.selectById(id) == null) {
            return Result.error("客户不存在，id = " + id);
        }
        int rows = customerMapper.deleteById(id);
        log.info("删除客户成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 customerType 取值合法性。
     */
    private boolean isValidCustomerType(String value) {
        return value == null
                || CUSTOMER_TYPE_GOVERNMENT.equals(value)
                || CUSTOMER_TYPE_ENTERPRISE.equals(value)
                || CUSTOMER_TYPE_STATE_OWNED.equals(value);
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
