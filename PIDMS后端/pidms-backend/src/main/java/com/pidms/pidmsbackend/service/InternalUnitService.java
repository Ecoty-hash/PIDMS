package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.InternalUnitDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.InternalUnitQueryParam;
import com.pidms.pidmsbackend.vo.InternalUnitVO;

/**
 * 内部单位 服务接口
 */
public interface InternalUnitService {

    /**
     * 分页查询内部单位列表
     */
    Result<PageInfo<InternalUnitVO>> pageQuery(InternalUnitQueryParam queryParam);

    /**
     * 查询内部单位详情
     */
    Result<InternalUnitVO> getById(Long id);

    /**
     * 新增内部单位
     */
    Result<Long> add(InternalUnitDTO dto);

    /**
     * 编辑内部单位
     */
    Result<Long> update(Long id, InternalUnitDTO dto);

    /**
     * 删除内部单位
     */
    Result<Boolean> delete(Long id);
}
