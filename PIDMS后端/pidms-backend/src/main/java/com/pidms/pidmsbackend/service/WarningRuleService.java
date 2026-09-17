package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.WarningRuleDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.WarningRuleQueryParam;
import com.pidms.pidmsbackend.vo.WarningRuleVO;

/**
 * 预警规则 服务接口
 */
public interface WarningRuleService {

    /**
     * 分页查询预警规则列表
     */
    Result<PageInfo<WarningRuleVO>> pageQuery(WarningRuleQueryParam queryParam);

    /**
     * 查询预警规则详情
     */
    Result<WarningRuleVO> getById(Long id);

    /**
     * 新增预警规则
     */
    Result<Long> add(WarningRuleDTO dto);

    /**
     * 编辑预警规则
     */
    Result<Long> update(Long id, WarningRuleDTO dto);

    /**
     * 删除预警规则
     */
    Result<Boolean> delete(Long id);
}
