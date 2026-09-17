package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SealApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SealApplicationQueryParam;
import com.pidms.pidmsbackend.vo.SealApplicationVO;

public interface SealApplicationService {
    /**
     * 分页查询用印申请列表
     */
    Result<PageInfo<SealApplicationVO>> pageQuery(SealApplicationQueryParam queryParam);

    /**
     * 查询用印申请详情
     */
    Result<SealApplicationVO> getById(Long id);

    /**
     * 新增用印申请
     */
    Result<Long> add(SealApplicationDTO dto);

    /**
     * 编辑用印申请
     */
    Result<Long> update(Long id, SealApplicationDTO dto);

    /**
     * 删除用印申请
     */
    Result<Boolean> delete(Long id);

    /**
     * 审批通过（仅待审批 → 已审批）
     */
    Result<Boolean> approve(Long id);

    /**
     * 审批驳回（仅待审批 → 已拒绝）
     */
    Result<Boolean> reject(Long id);
}
