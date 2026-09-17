package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SafetyCheckItemDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyCheckItemQueryParam;
import com.pidms.pidmsbackend.vo.SafetyCheckItemVO;

/**
 * 安全检查项 服务接口
 */
public interface SafetyCheckItemService {

    /**
     * 分页查询安全检查项列表
     */
    Result<PageInfo<SafetyCheckItemVO>> pageQuery(SafetyCheckItemQueryParam queryParam);

    /**
     * 查询安全检查项详情
     */
    Result<SafetyCheckItemVO> getById(Long id);

    /**
     * 新增安全检查项
     */
    Result<Long> add(SafetyCheckItemDTO dto);

    /**
     * 编辑安全检查项
     */
    Result<Long> update(Long id, SafetyCheckItemDTO dto);

    /**
     * 删除安全检查项
     */
    Result<Boolean> delete(Long id);

    /**
     * 封存安全检查项（封存后不可再被新的检查单选用）
     */
    Result<Boolean> seal(Long id);
}
