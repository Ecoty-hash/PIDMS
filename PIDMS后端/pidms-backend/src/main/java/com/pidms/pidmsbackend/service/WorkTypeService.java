package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.WorkTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.WorkTypeQueryParam;
import com.pidms.pidmsbackend.vo.WorkTypeVO;

/**
 * 工种类型 服务接口
 */
public interface WorkTypeService {

    /**
     * 分页查询工种类型列表
     */
    Result<PageInfo<WorkTypeVO>> pageQuery(WorkTypeQueryParam queryParam);

    /**
     * 查询工种类型详情
     */
    Result<WorkTypeVO> getById(Long id);

    /**
     * 新增工种类型
     */
    Result<Long> add(WorkTypeDTO dto);

    /**
     * 编辑工种类型
     */
    Result<Long> update(Long id, WorkTypeDTO dto);

    /**
     * 删除工种类型
     */
    Result<Boolean> delete(Long id);

    /**
     * 封存（enabled / disabled → sealed）
     */
    Result<Boolean> seal(Long id);

    /**
     * 解封（sealed → enabled）
     */
    Result<Boolean> unseal(Long id);
}
