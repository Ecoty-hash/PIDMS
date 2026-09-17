package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.QualityRectificationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityRectificationQueryParam;
import com.pidms.pidmsbackend.vo.QualityRectificationVO;

/**
 * 质量整改单 服务接口
 */
public interface QualityRectificationService {

    /**
     * 分页查询质量整改单列表
     */
    Result<PageInfo<QualityRectificationVO>> pageQuery(QualityRectificationQueryParam queryParam);

    /**
     * 查询质量整改单详情
     */
    Result<QualityRectificationVO> getById(Long id);

    /**
     * 新增质量整改单
     */
    Result<Long> add(QualityRectificationDTO dto);

    /**
     * 编辑质量整改单
     */
    Result<Long> update(Long id, QualityRectificationDTO dto);

    /**
     * 删除质量整改单
     */
    Result<Boolean> delete(Long id);
}
