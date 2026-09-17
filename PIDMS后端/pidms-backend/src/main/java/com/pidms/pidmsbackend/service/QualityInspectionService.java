package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.QualityInspectionDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityInspectionQueryParam;
import com.pidms.pidmsbackend.vo.QualityInspectionVO;

/**
 * 质量检查 服务接口
 */
public interface QualityInspectionService {

    /**
     * 分页查询质量检查列表
     */
    Result<PageInfo<QualityInspectionVO>> pageQuery(QualityInspectionQueryParam queryParam);

    /**
     * 查询质量检查详情
     */
    Result<QualityInspectionVO> getById(Long id);

    /**
     * 新增质量检查
     */
    Result<Long> add(QualityInspectionDTO dto);

    /**
     * 编辑质量检查
     */
    Result<Long> update(Long id, QualityInspectionDTO dto);

    /**
     * 删除质量检查
     */
    Result<Boolean> delete(Long id);
}
