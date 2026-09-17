package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SafetyRectificationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyRectificationQueryParam;
import com.pidms.pidmsbackend.service.SafetyRectificationService;
import com.pidms.pidmsbackend.vo.SafetyRectificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 安全整改单 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/safety-rectifications")
@Tag(name = "安全整改单接口")
public class SafetyRectificationController {

    @Resource
    private SafetyRectificationService safetyRectificationService;

    /**
     * 分页查询安全整改单列表
     */
    @GetMapping
    @Operation(summary = "分页查询安全整改单列表")
    public Result<PageInfo<SafetyRectificationVO>> pageQuery(SafetyRectificationQueryParam queryParam) {
        log.info("分页查询安全整改单，参数：{}", queryParam);
        return safetyRectificationService.pageQuery(queryParam);
    }

    /**
     * 查询安全整改单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询安全整改单详情")
    public Result<SafetyRectificationVO> getById(@PathVariable Long id) {
        log.info("查询安全整改单详情，id：{}", id);
        return safetyRectificationService.getById(id);
    }

    /**
     * 新增安全整改单
     */
    @PostMapping
    @Operation(summary = "新增安全整改单")
    public Result<Long> add(@RequestBody SafetyRectificationDTO dto) {
        log.info("新增安全整改单，参数：{}", dto);
        return safetyRectificationService.add(dto);
    }

    /**
     * 编辑安全整改单
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑安全整改单")
    public Result<Long> update(@PathVariable Long id, @RequestBody SafetyRectificationDTO dto) {
        log.info("编辑安全整改单，id：{}，参数：{}", id, dto);
        return safetyRectificationService.update(id, dto);
    }

    /**
     * 删除安全整改单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除安全整改单")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除安全整改单，id：{}", id);
        return safetyRectificationService.delete(id);
    }
}
