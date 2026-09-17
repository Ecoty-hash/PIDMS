package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.QualityCheckItemDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityCheckItemQueryParam;
import com.pidms.pidmsbackend.vo.QualityCheckItemVO;

/**
 * 质量检查项 服务接口
 */
public interface QualityCheckItemService {

    /**
     * 分页查询质量检查项列表
     */
    Result<PageInfo<QualityCheckItemVO>> pageQuery(QualityCheckItemQueryParam queryParam);

    /**
     * 查询质量检查项详情
     */
    Result<QualityCheckItemVO> getById(Long id);

    /**
     * 新增质量检查项
     */
    Result<Long> add(QualityCheckItemDTO dto);

    /**
     * 编辑质量检查项
     */
    Result<Long> update(Long id, QualityCheckItemDTO dto);

    /**
     * 删除质量检查项
     */
    Result<Boolean> delete(Long id);

    /**
     * 封存质量检查项（封存后不可再被新的检查单选用）
     */
    Result<Boolean> seal(Long id);
}
