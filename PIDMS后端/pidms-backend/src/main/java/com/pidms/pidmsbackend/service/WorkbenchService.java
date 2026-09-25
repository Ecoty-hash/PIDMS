package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.vo.WorkbenchVO;

/**
 * 工作台 服务接口（个人视角：待我审批、我的项目、预警、动态）
 */
public interface WorkbenchService {

    /**
     * 工作台总览，全部数据以当前登录用户为中心
     */
    Result<WorkbenchVO> overview();
}
