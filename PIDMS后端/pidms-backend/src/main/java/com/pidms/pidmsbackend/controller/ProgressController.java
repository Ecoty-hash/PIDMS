package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProgressDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProgressQueryParam;
import com.pidms.pidmsbackend.service.ProgressService;
import com.pidms.pidmsbackend.vo.ProgressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 进度管理 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/progress")
@Tag(name = "进度管理接口")
public class ProgressController {

    @Resource
    private ProgressService progressService;

    /**
     * 分页查询进度管理列表
     */
    @GetMapping
    @Operation(summary = "分页查询进度管理列表")
    public Result<PageInfo<ProgressVO>> pageQuery(ProgressQueryParam queryParam) {
        log.info("分页查询进度管理，参数：{}", queryParam);
        return progressService.pageQuery(queryParam);
    }

    /**
     * 查询进度管理详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询进度管理详情")
    public Result<ProgressVO> getById(@PathVariable Long id) {
        log.info("查询进度管理详情，id：{}", id);
        return progressService.getById(id);
    }

    /**
     * 新增进度管理
     */
    @PostMapping
    @Operation(summary = "新增进度管理")
    public Result<Long> add(@RequestBody ProgressDTO dto) {
        log.info("新增进度管理，参数：{}", dto);
        return progressService.add(dto);
    }

    /**
     * 编辑进度管理
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑进度管理")
    public Result<Long> update(@PathVariable Long id, @RequestBody ProgressDTO dto) {
        log.info("编辑进度管理，id：{}，参数：{}", id, dto);
        return progressService.update(id, dto);
    }

    /**
     * 删除进度管理
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除进度管理")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除进度管理，id：{}", id);
        return progressService.delete(id);
    }
}
