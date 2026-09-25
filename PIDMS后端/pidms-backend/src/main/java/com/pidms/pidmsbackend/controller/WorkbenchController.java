package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.service.WorkbenchService;
import com.pidms.pidmsbackend.vo.WorkbenchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台 接口（个人视角，前端 / 首页使用）
 */
@Slf4j
@RestController
@RequestMapping("/api/workbench")
@Tag(name = "工作台接口")
public class WorkbenchController {

    @Resource
    private WorkbenchService workbenchService;

    @GetMapping("/overview")
    @Operation(summary = "工作台总览：待我审批、我的项目、逾期预警、最近动态、我发起的申请")
    public Result<WorkbenchVO> overview() {
        log.info("查询工作台总览");
        return workbenchService.overview();
    }
}
