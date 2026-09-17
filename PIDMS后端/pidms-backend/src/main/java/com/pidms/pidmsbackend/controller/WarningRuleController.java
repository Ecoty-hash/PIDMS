package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.WarningRuleDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.WarningRuleQueryParam;
import com.pidms.pidmsbackend.service.WarningRuleService;
import com.pidms.pidmsbackend.vo.WarningRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 预警规则 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/warning-rules")
@Tag(name = "预警规则接口")
public class WarningRuleController {

    @Resource
    private WarningRuleService warningRuleService;

    /**
     * 分页查询预警规则列表
     */
    @GetMapping
    @Operation(summary = "分页查询预警规则列表")
    public Result<PageInfo<WarningRuleVO>> pageQuery(WarningRuleQueryParam queryParam) {
        log.info("分页查询预警规则，参数：{}", queryParam);
        return warningRuleService.pageQuery(queryParam);
    }

    /**
     * 查询预警规则详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询预警规则详情")
    public Result<WarningRuleVO> getById(@PathVariable Long id) {
        log.info("查询预警规则详情，id：{}", id);
        return warningRuleService.getById(id);
    }

    /**
     * 新增预警规则
     */
    @PostMapping
    @Operation(summary = "新增预警规则")
    public Result<Long> add(@RequestBody WarningRuleDTO dto) {
        log.info("新增预警规则，参数：{}", dto);
        return warningRuleService.add(dto);
    }

    /**
     * 编辑预警规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑预警规则")
    public Result<Long> update(@PathVariable Long id, @RequestBody WarningRuleDTO dto) {
        log.info("编辑预警规则，id：{}，参数：{}", id, dto);
        return warningRuleService.update(id, dto);
    }

    /**
     * 删除预警规则
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除预警规则")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除预警规则，id：{}", id);
        return warningRuleService.delete(id);
    }
}
