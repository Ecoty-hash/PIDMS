package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.OrganizationDTO;
import com.pidms.pidmsbackend.entity.OrganizationQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.service.OrganizationService;
import com.pidms.pidmsbackend.vo.OrganizationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机构管理 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations")
@Tag(name = "机构管理接口")
public class OrganizationController {

    @Resource
    private OrganizationService organizationService;

    /**
     * 分页查询机构管理列表
     */
    @GetMapping
    @Operation(summary = "分页查询机构管理列表")
    public Result<PageInfo<OrganizationVO>> pageQuery(OrganizationQueryParam queryParam) {
        log.info("分页查询机构管理，参数：{}", queryParam);
        return organizationService.pageQuery(queryParam);
    }

    /**
     * 查询机构树
     */
    @GetMapping("/tree")
    @Operation(summary = "查询机构树")
    public Result<List<OrganizationVO>> tree(@RequestParam(required = false) String keyword) {
        log.info("查询机构树，keyword：{}", keyword);
        return organizationService.tree(keyword);
    }

    /**
     * 查询机构管理详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询机构管理详情")
    public Result<OrganizationVO> getById(@PathVariable Long id) {
        log.info("查询机构管理详情，id：{}", id);
        return organizationService.getById(id);
    }

    /**
     * 新增机构管理
     */
    @PostMapping
    @Operation(summary = "新增机构管理")
    public Result<Long> add(@RequestBody OrganizationDTO dto) {
        log.info("新增机构管理，参数：{}", dto);
        return organizationService.add(dto);
    }

    /**
     * 编辑机构管理
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑机构管理")
    public Result<Long> update(@PathVariable Long id, @RequestBody OrganizationDTO dto) {
        log.info("编辑机构管理，id：{}，参数：{}", id, dto);
        return organizationService.update(id, dto);
    }

    /**
     * 删除机构管理
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除机构管理")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除机构管理，id：{}", id);
        return organizationService.delete(id);
    }

    /**
     * 添加下级机构
     */
    @PostMapping("/{id}/children")
    @Operation(summary = "添加下级机构")
    public Result<OrganizationVO> addChild(@PathVariable Long id, @RequestBody OrganizationDTO dto) {
        log.info("添加下级机构，parentId：{}，参数：{}", id, dto);
        return organizationService.addChild(id, dto);
    }
}
