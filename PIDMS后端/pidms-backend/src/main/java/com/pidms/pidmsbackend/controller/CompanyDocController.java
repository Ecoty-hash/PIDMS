package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.CompanyDocDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.CompanyDocQueryParam;
import com.pidms.pidmsbackend.service.CompanyDocService;
import com.pidms.pidmsbackend.vo.CompanyDocVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 公司文档 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/company-docs")
@Tag(name = "公司文档接口")
public class CompanyDocController {

    @Resource
    private CompanyDocService companyDocService;

    /**
     * 分页查询公司文档列表
     */
    @GetMapping
    @Operation(summary = "分页查询公司文档列表")
    public Result<PageInfo<CompanyDocVO>> pageQuery(CompanyDocQueryParam queryParam) {
        log.info("分页查询公司文档，参数：{}", queryParam);
        return companyDocService.pageQuery(queryParam);
    }

    /**
     * 查询公司文档详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询公司文档详情")
    public Result<CompanyDocVO> getById(@PathVariable Long id) {
        log.info("查询公司文档详情，id：{}", id);
        return companyDocService.getById(id);
    }

    /**
     * 新增公司文档
     */
    @PostMapping
    @Operation(summary = "新增公司文档")
    public Result<Long> add(@RequestBody CompanyDocDTO dto) {
        log.info("新增公司文档，参数：{}", dto);
        return companyDocService.add(dto);
    }

    /**
     * 编辑公司文档
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑公司文档")
    public Result<Long> update(@PathVariable Long id, @RequestBody CompanyDocDTO dto) {
        log.info("编辑公司文档，id：{}，参数：{}", id, dto);
        return companyDocService.update(id, dto);
    }

    /**
     * 删除公司文档
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除公司文档")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除公司文档，id：{}", id);
        return companyDocService.delete(id);
    }
}
