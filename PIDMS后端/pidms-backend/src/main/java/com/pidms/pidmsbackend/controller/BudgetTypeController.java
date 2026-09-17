package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.BudgetTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.BudgetTypeQueryParam;
import com.pidms.pidmsbackend.service.BudgetTypeService;
import com.pidms.pidmsbackend.vo.BudgetTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 预算类型 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/budget-types")
@Tag(name = "预算类型接口")
public class BudgetTypeController {

    @Resource
    private BudgetTypeService budgetTypeService;

    /**
     * 分页查询预算类型列表
     */
    @GetMapping
    @Operation(summary = "分页查询预算类型列表")
    public Result<PageInfo<BudgetTypeVO>> pageQuery(BudgetTypeQueryParam queryParam) {
        log.info("分页查询预算类型，参数：{}", queryParam);
        return budgetTypeService.pageQuery(queryParam);
    }

    /**
     * 查询预算类型详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询预算类型详情")
    public Result<BudgetTypeVO> getById(@PathVariable Long id) {
        log.info("查询预算类型详情，id：{}", id);
        return budgetTypeService.getById(id);
    }

    /**
     * 新增预算类型
     */
    @PostMapping
    @Operation(summary = "新增预算类型")
    public Result<Long> add(@RequestBody BudgetTypeDTO dto) {
        log.info("新增预算类型，参数：{}", dto);
        return budgetTypeService.add(dto);
    }

    /**
     * 编辑预算类型
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑预算类型")
    public Result<Long> update(@PathVariable Long id, @RequestBody BudgetTypeDTO dto) {
        log.info("编辑预算类型，id：{}，参数：{}", id, dto);
        return budgetTypeService.update(id, dto);
    }

    /**
     * 删除预算类型
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除预算类型")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除预算类型，id：{}", id);
        return budgetTypeService.delete(id);
    }
}
