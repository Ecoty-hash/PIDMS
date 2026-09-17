package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.RoleDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.RoleQueryParam;
import com.pidms.pidmsbackend.vo.RoleVO;

/**
 * 角色管理 服务接口
 */
public interface RoleService {

    /**
     * 分页查询角色列表
     */
    Result<PageInfo<RoleVO>> pageQuery(RoleQueryParam queryParam);

    /**
     * 查询角色详情
     */
    Result<RoleVO> getById(Long id);

    /**
     * 新增角色
     */
    Result<Long> add(RoleDTO dto);

    /**
     * 编辑角色
     */
    Result<Long> update(Long id, RoleDTO dto);

    /**
     * 删除角色
     */
    Result<Boolean> delete(Long id);
}
