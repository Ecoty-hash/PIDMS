package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.BasicInfoDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.BasicInfoQueryParam;
import com.pidms.pidmsbackend.service.BasicInfoService;
import com.pidms.pidmsbackend.vo.BasicInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 基础资料 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/basic-infos")
@Tag(name = "基础资料接口")
public class BasicInfoController {

    @Resource
    private BasicInfoService basicInfoService;

    /**
     * 分页查询基础资料列表
     */
    @GetMapping
    @Operation(summary = "分页查询基础资料列表")
    public Result<PageInfo<BasicInfoVO>> pageQuery(BasicInfoQueryParam queryParam) {
        log.info("分页查询基础资料，参数：{}", queryParam);
        return basicInfoService.pageQuery(queryParam);
    }

    /**
     * 查询基础资料详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询基础资料详情")
    public Result<BasicInfoVO> getById(@PathVariable Long id) {
        log.info("查询基础资料详情，id：{}", id);
        return basicInfoService.getById(id);
    }

    /**
     * 新增基础资料
     */
    @PostMapping
    @Operation(summary = "新增基础资料")
    public Result<Long> add(@RequestBody BasicInfoDTO dto) {
        log.info("新增基础资料，参数：{}", dto);
        return basicInfoService.add(dto);
    }

    /**
     * 编辑基础资料
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑基础资料")
    public Result<Long> update(@PathVariable Long id, @RequestBody BasicInfoDTO dto) {
        log.info("编辑基础资料，id：{}，参数：{}", id, dto);
        return basicInfoService.update(id, dto);
    }

    /**
     * 删除基础资料
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除基础资料")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除基础资料，id：{}", id);
        return basicInfoService.delete(id);
    }
}
