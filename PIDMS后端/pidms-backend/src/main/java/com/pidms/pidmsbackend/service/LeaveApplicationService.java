package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.LeaveApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.LeaveApplicationQueryParam;
import com.pidms.pidmsbackend.vo.LeaveApplicationVO;

/**
 * 请假申请 服务接口
 */
public interface LeaveApplicationService {

    /**
     * 分页查询请假申请列表
     */
    Result<PageInfo<LeaveApplicationVO>> pageQuery(LeaveApplicationQueryParam queryParam);

    /**
     * 查询请假申请详情
     */
    Result<LeaveApplicationVO> getById(Long id);

    /**
     * 新增请假申请
     */
    Result<Long> add(LeaveApplicationDTO dto);

    /**
     * 编辑请假申请
     */
    Result<Long> update(Long id, LeaveApplicationDTO dto);

    /**
     * 删除请假申请
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
