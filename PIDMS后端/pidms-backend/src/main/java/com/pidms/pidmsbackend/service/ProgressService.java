package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProgressDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProgressQueryParam;
import com.pidms.pidmsbackend.vo.ProgressVO;

/**
 * 进度管理 服务接口
 */
public interface ProgressService {

    /**
     * 分页查询进度管理列表
     */
    Result<PageInfo<ProgressVO>> pageQuery(ProgressQueryParam queryParam);

    /**
     * 查询进度管理详情
     */
    Result<ProgressVO> getById(Long id);

    /**
     * 新增进度管理
     */
    Result<Long> add(ProgressDTO dto);

    /**
     * 编辑进度管理
     */
    Result<Long> update(Long id, ProgressDTO dto);

    /**
     * 删除进度管理
     */
    Result<Boolean> delete(Long id);
}
