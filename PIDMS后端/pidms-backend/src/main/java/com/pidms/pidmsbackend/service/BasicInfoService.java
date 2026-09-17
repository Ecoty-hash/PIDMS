package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.BasicInfoDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.BasicInfoQueryParam;
import com.pidms.pidmsbackend.vo.BasicInfoVO;

/**
 * 基础资料 服务接口
 */
public interface BasicInfoService {

    /**
     * 分页查询基础资料列表
     */
    Result<PageInfo<BasicInfoVO>> pageQuery(BasicInfoQueryParam queryParam);

    /**
     * 查询基础资料详情
     */
    Result<BasicInfoVO> getById(Long id);

    /**
     * 新增基础资料
     */
    Result<Long> add(BasicInfoDTO dto);

    /**
     * 编辑基础资料
     */
    Result<Long> update(Long id, BasicInfoDTO dto);

    /**
     * 删除基础资料
     */
    Result<Boolean> delete(Long id);
}
