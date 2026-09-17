package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SupplierDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SupplierQueryParam;
import com.pidms.pidmsbackend.vo.SupplierVO;

/**
 * 供应商 服务接口
 */
public interface SupplierService {

    /**
     * 分页查询供应商列表
     */
    Result<PageInfo<SupplierVO>> pageQuery(SupplierQueryParam queryParam);

    /**
     * 查询供应商详情
     */
    Result<SupplierVO> getById(Long id);

    /**
     * 新增供应商
     */
    Result<Long> add(SupplierDTO dto);

    /**
     * 编辑供应商
     */
    Result<Long> update(Long id, SupplierDTO dto);

    /**
     * 删除供应商
     */
    Result<Boolean> delete(Long id);
}
