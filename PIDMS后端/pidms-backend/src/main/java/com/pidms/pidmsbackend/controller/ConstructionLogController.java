package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ConstructionLogDTO;
import com.pidms.pidmsbackend.entity.ConstructionLogQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.service.ConstructionLogService;
import com.pidms.pidmsbackend.vo.ConstructionLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 施工日志 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/construction-logs")
@Tag(name = "施工日志接口")
public class ConstructionLogController {

    @Resource
    private ConstructionLogService constructionLogService;

    /**
     * 分页查询施工日志列表
     */
    @GetMapping
    @Operation(summary = "分页查询施工日志列表")
    public Result<PageInfo<ConstructionLogVO>> pageQuery(ConstructionLogQueryParam queryParam) {
        log.info("分页查询施工日志，参数：{}", queryParam);
        return constructionLogService.pageQuery(queryParam);
    }

    /**
     * 查询施工日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询施工日志详情")
    public Result<ConstructionLogVO> getById(@PathVariable Long id) {
        log.info("查询施工日志详情，id：{}", id);
        return constructionLogService.getById(id);
    }

    /**
     * 新增施工日志
     */
    @PostMapping
    @Operation(summary = "新增施工日志")
    public Result<Long> add(@RequestBody ConstructionLogDTO dto) {
        log.info("新增施工日志，参数：{}", dto);
        return constructionLogService.add(dto);
    }

    /**
     * 编辑施工日志
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑施工日志")
    public Result<Long> update(@PathVariable Long id, @RequestBody ConstructionLogDTO dto) {
        log.info("编辑施工日志，id：{}，参数：{}", id, dto);
        return constructionLogService.update(id, dto);
    }

    /**
     * 删除施工日志
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除施工日志")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除施工日志，id：{}", id);
        return constructionLogService.delete(id);
    }
}
