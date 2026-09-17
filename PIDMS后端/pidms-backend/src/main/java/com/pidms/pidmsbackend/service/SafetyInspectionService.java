package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SafetyInspectionDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyInspectionQueryParam;
import com.pidms.pidmsbackend.vo.SafetyInspectionVO;

/**
 * 安全检查 服务接口
 */
public interface SafetyInspectionService {

    /**
     * 分页查询安全检查列表
     */
    Result<PageInfo<SafetyInspectionVO>> pageQuery(SafetyInspectionQueryParam queryParam);

    /**
     * 查询安全检查详情
     */
    Result<SafetyInspectionVO> getById(Long id);

    /**
     * 新增安全检查
     */
    Result<Long> add(SafetyInspectionDTO dto);

    /**
     * 编辑安全检查
     */
    Result<Long> update(Long id, SafetyInspectionDTO dto);

    /**
     * 删除安全检查
     */
    Result<Boolean> delete(Long id);
}
