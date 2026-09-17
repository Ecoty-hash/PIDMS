package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.InternalUnitDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.InternalUnitQueryParam;
import com.pidms.pidmsbackend.service.InternalUnitService;
import com.pidms.pidmsbackend.vo.InternalUnitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 内部单位 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/internal-units")
@Tag(name = "内部单位接口")
public class InternalUnitController {

    @Resource
    private InternalUnitService internalUnitService;

    /**
     * 分页查询内部单位列表
     */
    @GetMapping
    @Operation(summary = "分页查询内部单位列表")
    public Result<PageInfo<InternalUnitVO>> pageQuery(InternalUnitQueryParam queryParam) {
        log.info("分页查询内部单位，参数：{}", queryParam);
        return internalUnitService.pageQuery(queryParam);
    }

    /**
     * 查询内部单位详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询内部单位详情")
    public Result<InternalUnitVO> getById(@PathVariable Long id) {
        log.info("查询内部单位详情，id：{}", id);
        return internalUnitService.getById(id);
    }

    /**
     * 新增内部单位
     */
    @PostMapping
    @Operation(summary = "新增内部单位")
    public Result<Long> add(@RequestBody InternalUnitDTO dto) {
        log.info("新增内部单位，参数：{}", dto);
        return internalUnitService.add(dto);
    }

    /**
     * 编辑内部单位
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑内部单位")
    public Result<Long> update(@PathVariable Long id, @RequestBody InternalUnitDTO dto) {
        log.info("编辑内部单位，id：{}，参数：{}", id, dto);
        return internalUnitService.update(id, dto);
    }

    /**
     * 删除内部单位
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除内部单位")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除内部单位，id：{}", id);
        return internalUnitService.delete(id);
    }
}
