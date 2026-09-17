package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.OrganizationDTO;
import com.pidms.pidmsbackend.entity.OrganizationQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.vo.OrganizationVO;

import java.util.List;

/**
 * 机构管理 服务接口
 */
public interface OrganizationService {

    /**
     * 分页查询机构列表（平铺）
     */
    Result<PageInfo<OrganizationVO>> pageQuery(OrganizationQueryParam queryParam);

    /**
     * 查询机构详情
     */
    Result<OrganizationVO> getById(Long id);

    /**
     * 新增机构
     */
    Result<Long> add(OrganizationDTO dto);

    /**
     * 编辑机构
     */
    Result<Long> update(Long id, OrganizationDTO dto);

    /**
     * 删除机构
     */
    Result<Boolean> delete(Long id);

    /**
     * 查询机构树（keyword 为空时返回全部）
     */
    Result<List<OrganizationVO>> tree(String keyword);

    /**
     * 在指定机构下新增下级机构
     */
    Result<OrganizationVO> addChild(Long parentId, OrganizationDTO dto);
}
