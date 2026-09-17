package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.QualityInspectionDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityInspectionQueryParam;
import com.pidms.pidmsbackend.service.QualityInspectionService;
import com.pidms.pidmsbackend.vo.QualityInspectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 质量检查 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/quality-inspections")
@Tag(name = "质量检查接口")
public class QualityInspectionController {

    @Resource
    private QualityInspectionService qualityInspectionService;

    /**
     * 分页查询质量检查列表
     */
    @GetMapping
    @Operation(summary = "分页查询质量检查列表")
    public Result<PageInfo<QualityInspectionVO>> pageQuery(QualityInspectionQueryParam queryParam) {
        log.info("分页查询质量检查，参数：{}", queryParam);
        return qualityInspectionService.pageQuery(queryParam);
    }

    /**
     * 查询质量检查详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询质量检查详情")
    public Result<QualityInspectionVO> getById(@PathVariable Long id) {
        log.info("查询质量检查详情，id：{}", id);
        return qualityInspectionService.getById(id);
    }

    /**
     * 新增质量检查
     */
    @PostMapping
    @Operation(summary = "新增质量检查")
    public Result<Long> add(@RequestBody QualityInspectionDTO dto) {
        log.info("新增质量检查，参数：{}", dto);
        return qualityInspectionService.add(dto);
    }

    /**
     * 编辑质量检查
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑质量检查")
    public Result<Long> update(@PathVariable Long id, @RequestBody QualityInspectionDTO dto) {
        log.info("编辑质量检查，id：{}，参数：{}", id, dto);
        return qualityInspectionService.update(id, dto);
    }

    /**
     * 删除质量检查
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除质量检查")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除质量检查，id：{}", id);
        return qualityInspectionService.delete(id);
    }
}
