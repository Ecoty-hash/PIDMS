package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SafetyInspectionDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyInspectionQueryParam;
import com.pidms.pidmsbackend.service.SafetyInspectionService;
import com.pidms.pidmsbackend.vo.SafetyInspectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 安全检查 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/safety-inspections")
@Tag(name = "安全检查接口")
public class SafetyInspectionController {

    @Resource
    private SafetyInspectionService safetyInspectionService;

    /**
     * 分页查询安全检查列表
     */
    @GetMapping
    @Operation(summary = "分页查询安全检查列表")
    public Result<PageInfo<SafetyInspectionVO>> pageQuery(SafetyInspectionQueryParam queryParam) {
        log.info("分页查询安全检查，参数：{}", queryParam);
        return safetyInspectionService.pageQuery(queryParam);
    }

    /**
     * 查询安全检查详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询安全检查详情")
    public Result<SafetyInspectionVO> getById(@PathVariable Long id) {
        log.info("查询安全检查详情，id：{}", id);
        return safetyInspectionService.getById(id);
    }

    /**
     * 新增安全检查
     */
    @PostMapping
    @Operation(summary = "新增安全检查")
    public Result<Long> add(@RequestBody SafetyInspectionDTO dto) {
        log.info("新增安全检查，参数：{}", dto);
        return safetyInspectionService.add(dto);
    }

    /**
     * 编辑安全检查
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑安全检查")
    public Result<Long> update(@PathVariable Long id, @RequestBody SafetyInspectionDTO dto) {
        log.info("编辑安全检查，id：{}，参数：{}", id, dto);
        return safetyInspectionService.update(id, dto);
    }

    /**
     * 删除安全检查
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除安全检查")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除安全检查，id：{}", id);
        return safetyInspectionService.delete(id);
    }
}
