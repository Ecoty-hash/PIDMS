package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SealTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SealTypeQueryParam;
import com.pidms.pidmsbackend.vo.SealTypeVO;

public interface SealTypeService {
    /**
     * 分页查询印章类型列表
     */
    Result<PageInfo<SealTypeVO>> pageQuery(SealTypeQueryParam queryParam);

    /**
     * 查询印章类型详情
     */
    Result<SealTypeVO> getById(Long id);

    /**
     * 新增印章类型
     */
    Result<Long> add(SealTypeDTO dto);

    /**
     * 编辑印章类型
     */
    Result<Long> update(Long id, SealTypeDTO dto);

    /**
     * 删除印章类型
     */
    Result<Boolean> delete(Long id);

    /**
     * 封存印章类型（封存后不可再选用）
     */
    Result<Boolean> seal(Long id);

    /**
     * 解封印章类型
     */
    Result<Boolean> unseal(Long id);
}
