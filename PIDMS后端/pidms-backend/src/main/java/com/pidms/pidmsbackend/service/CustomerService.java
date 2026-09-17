package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.CustomerDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.CustomerQueryParam;
import com.pidms.pidmsbackend.vo.CustomerVO;

/**
 * 客户 服务接口
 */
public interface CustomerService {

    /**
     * 分页查询客户列表
     */
    Result<PageInfo<CustomerVO>> pageQuery(CustomerQueryParam queryParam);

    /**
     * 查询客户详情
     */
    Result<CustomerVO> getById(Long id);

    /**
     * 新增客户
     */
    Result<Long> add(CustomerDTO dto);

    /**
     * 编辑客户
     */
    Result<Long> update(Long id, CustomerDTO dto);

    /**
     * 删除客户
     */
    Result<Boolean> delete(Long id);
}
