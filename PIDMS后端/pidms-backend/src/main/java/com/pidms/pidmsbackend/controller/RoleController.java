package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.RoleDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.RoleQueryParam;
import com.pidms.pidmsbackend.service.RoleService;
import com.pidms.pidmsbackend.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理 接口
 * <p>一期只提供角色 CRUD；导出 / 导入与权限分配按约定暂不实现。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@Tag(name = "角色管理接口")
public class RoleController {

    @Resource
    private RoleService roleService;

    /**
     * 分页查询角色管理列表
     */
    @GetMapping
    @Operation(summary = "分页查询角色管理列表")
    public Result<PageInfo<RoleVO>> pageQuery(RoleQueryParam queryParam) {
        log.info("分页查询角色管理，参数：{}", queryParam);
        return roleService.pageQuery(queryParam);
    }

    /**
     * 查询角色管理详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询角色管理详情")
    public Result<RoleVO> getById(@PathVariable Long id) {
        log.info("查询角色管理详情，id：{}", id);
        return roleService.getById(id);
    }

    /**
     * 新增角色管理
     */
    @PostMapping
    @Operation(summary = "新增角色管理")
    public Result<Long> add(@RequestBody RoleDTO dto) {
        log.info("新增角色管理，参数：{}", dto);
        return roleService.add(dto);
    }

    /**
     * 编辑角色管理
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑角色管理")
    public Result<Long> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        log.info("编辑角色管理，id：{}，参数：{}", id, dto);
        return roleService.update(id, dto);
    }

    /**
     * 删除角色管理
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色管理")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除角色管理，id：{}", id);
        return roleService.delete(id);
    }
}
