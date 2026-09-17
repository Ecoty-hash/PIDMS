package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SafetyRectificationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyRectificationQueryParam;
import com.pidms.pidmsbackend.vo.SafetyRectificationVO;

/**
 * 安全整改单 服务接口
 */
public interface SafetyRectificationService {

    /**
     * 分页查询安全整改单列表
     */
    Result<PageInfo<SafetyRectificationVO>> pageQuery(SafetyRectificationQueryParam queryParam);

    /**
     * 查询安全整改单详情
     */
    Result<SafetyRectificationVO> getById(Long id);

    /**
     * 新增安全整改单
     */
    Result<Long> add(SafetyRectificationDTO dto);

    /**
     * 编辑安全整改单
     */
    Result<Long> update(Long id, SafetyRectificationDTO dto);

    /**
     * 删除安全整改单
     */
    Result<Boolean> delete(Long id);
}
