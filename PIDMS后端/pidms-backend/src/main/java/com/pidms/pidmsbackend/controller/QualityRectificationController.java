package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.QualityRectificationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityRectificationQueryParam;
import com.pidms.pidmsbackend.service.QualityRectificationService;
import com.pidms.pidmsbackend.vo.QualityRectificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 质量整改单 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/quality-rectifications")
@Tag(name = "质量整改单接口")
public class QualityRectificationController {

    @Resource
    private QualityRectificationService qualityRectificationService;

    /**
     * 分页查询质量整改单列表
     */
    @GetMapping
    @Operation(summary = "分页查询质量整改单列表")
    public Result<PageInfo<QualityRectificationVO>> pageQuery(QualityRectificationQueryParam queryParam) {
        log.info("分页查询质量整改单，参数：{}", queryParam);
        return qualityRectificationService.pageQuery(queryParam);
    }

    /**
     * 查询质量整改单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询质量整改单详情")
    public Result<QualityRectificationVO> getById(@PathVariable Long id) {
        log.info("查询质量整改单详情，id：{}", id);
        return qualityRectificationService.getById(id);
    }

    /**
     * 新增质量整改单
     */
    @PostMapping
    @Operation(summary = "新增质量整改单")
    public Result<Long> add(@RequestBody QualityRectificationDTO dto) {
        log.info("新增质量整改单，参数：{}", dto);
        return qualityRectificationService.add(dto);
    }

    /**
     * 编辑质量整改单
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑质量整改单")
    public Result<Long> update(@PathVariable Long id, @RequestBody QualityRectificationDTO dto) {
        log.info("编辑质量整改单，id：{}，参数：{}", id, dto);
        return qualityRectificationService.update(id, dto);
    }

    /**
     * 删除质量整改单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除质量整改单")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除质量整改单，id：{}", id);
        return qualityRectificationService.delete(id);
    }
}
