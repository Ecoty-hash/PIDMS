package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ConstructionLogDTO;
import com.pidms.pidmsbackend.entity.ConstructionLogQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.vo.ConstructionLogVO;

/**
 * 施工日志 服务接口
 */
public interface ConstructionLogService {

    /**
     * 分页查询施工日志列表
     */
    Result<PageInfo<ConstructionLogVO>> pageQuery(ConstructionLogQueryParam queryParam);

    /**
     * 查询施工日志详情
     */
    Result<ConstructionLogVO> getById(Long id);

    /**
     * 新增施工日志
     */
    Result<Long> add(ConstructionLogDTO dto);

    /**
     * 编辑施工日志
     */
    Result<Long> update(Long id, ConstructionLogDTO dto);

    /**
     * 删除施工日志
     */
    Result<Boolean> delete(Long id);
}
