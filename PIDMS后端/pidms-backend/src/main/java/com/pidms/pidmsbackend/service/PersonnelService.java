package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.PersonnelDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.PersonnelQueryParam;
import com.pidms.pidmsbackend.vo.PersonnelVO;

/**
 * 人员管理 服务接口
 */
public interface PersonnelService {

    /**
     * 分页查询人员列表
     */
    Result<PageInfo<PersonnelVO>> pageQuery(PersonnelQueryParam queryParam);

    /**
     * 查询人员详情
     */
    Result<PersonnelVO> getById(Long id);

    /**
     * 新增人员
     */
    Result<Long> add(PersonnelDTO dto);

    /**
     * 编辑人员
     */
    Result<Long> update(Long id, PersonnelDTO dto);

    /**
     * 删除人员
     */
    Result<Boolean> delete(Long id);
}
