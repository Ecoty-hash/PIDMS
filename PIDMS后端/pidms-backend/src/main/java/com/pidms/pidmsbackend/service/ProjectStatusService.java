package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProjectStatusDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectStatusQueryParam;
import com.pidms.pidmsbackend.vo.ProjectStatusVO;

public interface ProjectStatusService {
    /**
     * 分页查询项目状态列表
     */
    Result<PageInfo<ProjectStatusVO>> pageQuery(ProjectStatusQueryParam queryParam);

    /**
     * 查询项目状态详情
     */
    Result<ProjectStatusVO> getById(Long id);

    /**
     * 新增项目状态
     */
    Result<Long> add(ProjectStatusDTO dto);

    /**
     * 编辑项目状态
     */
    Result<Long> update(Long id, ProjectStatusDTO dto);

    /**
     * 删除项目状态
     */
    Result<Boolean> delete(Long id);

    /**
     * 封存项目状态（封存后不可再选用）
     */
    Result<Boolean> seal(Long id);

    /**
     * 解封项目状态
     */
    Result<Boolean> unseal(Long id);
}
