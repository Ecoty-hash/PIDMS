package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.BusinessTripDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.BusinessTripQueryParam;
import com.pidms.pidmsbackend.vo.BusinessTripVO;

/**
 * 出差申请 服务接口
 */
public interface BusinessTripService {

    /**
     * 分页查询出差申请列表
     */
    Result<PageInfo<BusinessTripVO>> pageQuery(BusinessTripQueryParam queryParam);

    /**
     * 查询出差申请详情
     */
    Result<BusinessTripVO> getById(Long id);

    /**
     * 新增出差申请
     */
    Result<Long> add(BusinessTripDTO dto);

    /**
     * 编辑出差申请
     */
    Result<Long> update(Long id, BusinessTripDTO dto);

    /**
     * 删除出差申请
     */
    Result<Boolean> delete(Long id);

    /**
     * 审批通过（pending → approved）
     */
    Result<Boolean> approve(Long id, String opinion);

    /**
     * 审批驳回（pending → rejected）
     */
    Result<Boolean> reject(Long id, String opinion);
}
