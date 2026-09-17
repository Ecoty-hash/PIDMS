package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SupplierDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SupplierQueryParam;
import com.pidms.pidmsbackend.service.SupplierService;
import com.pidms.pidmsbackend.vo.SupplierVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "供应商接口")
public class SupplierController {

    @Resource
    private SupplierService supplierService;

    /**
     * 分页查询供应商列表
     */
    @GetMapping
    @Operation(summary = "分页查询供应商列表")
    public Result<PageInfo<SupplierVO>> pageQuery(SupplierQueryParam queryParam) {
        log.info("分页查询供应商，参数：{}", queryParam);
        return supplierService.pageQuery(queryParam);
    }

    /**
     * 查询供应商详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询供应商详情")
    public Result<SupplierVO> getById(@PathVariable Long id) {
        log.info("查询供应商详情，id：{}", id);
        return supplierService.getById(id);
    }

    /**
     * 新增供应商
     */
    @PostMapping
    @Operation(summary = "新增供应商")
    public Result<Long> add(@RequestBody SupplierDTO dto) {
        log.info("新增供应商，参数：{}", dto);
        return supplierService.add(dto);
    }

    /**
     * 编辑供应商
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑供应商")
    public Result<Long> update(@PathVariable Long id, @RequestBody SupplierDTO dto) {
        log.info("编辑供应商，id：{}，参数：{}", id, dto);
        return supplierService.update(id, dto);
    }

    /**
     * 删除供应商
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除供应商")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除供应商，id：{}", id);
        return supplierService.delete(id);
    }
}
