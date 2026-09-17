package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.CompanyDocDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.CompanyDocQueryParam;
import com.pidms.pidmsbackend.vo.CompanyDocVO;

/**
 * 公司文档 服务接口
 */
public interface CompanyDocService {

    /**
     * 分页查询公司文档列表
     */
    Result<PageInfo<CompanyDocVO>> pageQuery(CompanyDocQueryParam queryParam);

    /**
     * 查询公司文档详情
     */
    Result<CompanyDocVO> getById(Long id);

    /**
     * 新增公司文档
     */
    Result<Long> add(CompanyDocDTO dto);

    /**
     * 编辑公司文档
     */
    Result<Long> update(Long id, CompanyDocDTO dto);

    /**
     * 删除公司文档
     */
    Result<Boolean> delete(Long id);
}
