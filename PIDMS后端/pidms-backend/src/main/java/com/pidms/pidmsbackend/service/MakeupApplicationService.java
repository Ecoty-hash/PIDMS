package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.MakeupApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.MakeupApplicationQueryParam;
import com.pidms.pidmsbackend.vo.MakeupApplicationVO;

/**
 * 补卡申请 服务接口
 */
public interface MakeupApplicationService {

    /**
     * 分页查询补卡申请列表
     */
    Result<PageInfo<MakeupApplicationVO>> pageQuery(MakeupApplicationQueryParam queryParam);

    /**
     * 查询补卡申请详情
     */
    Result<MakeupApplicationVO> getById(Long id);

    /**
     * 新增补卡申请
     */
    Result<Long> add(MakeupApplicationDTO dto);

    /**
     * 编辑补卡申请
     */
    Result<Long> update(Long id, MakeupApplicationDTO dto);

    /**
     * 删除补卡申请
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
