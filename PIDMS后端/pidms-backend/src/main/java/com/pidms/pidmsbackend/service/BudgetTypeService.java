package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.BudgetTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.BudgetTypeQueryParam;
import com.pidms.pidmsbackend.vo.BudgetTypeVO;

/**
 * 预算类型 服务接口
 */
public interface BudgetTypeService {

    /**
     * 分页查询预算类型列表
     */
    Result<PageInfo<BudgetTypeVO>> pageQuery(BudgetTypeQueryParam queryParam);

    /**
     * 查询预算类型详情
     */
    Result<BudgetTypeVO> getById(Long id);

    /**
     * 新增预算类型
     */
    Result<Long> add(BudgetTypeDTO dto);

    /**
     * 编辑预算类型
     */
    Result<Long> update(Long id, BudgetTypeDTO dto);

    /**
     * 删除预算类型
     */
    Result<Boolean> delete(Long id);
}
