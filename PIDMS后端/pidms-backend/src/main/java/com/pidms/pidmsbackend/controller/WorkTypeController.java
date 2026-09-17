package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.WorkTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.WorkTypeQueryParam;
import com.pidms.pidmsbackend.service.WorkTypeService;
import com.pidms.pidmsbackend.vo.WorkTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工种类型 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/work-types")
@Tag(name = "工种类型接口")
public class WorkTypeController {

    @Resource
    private WorkTypeService workTypeService;

    /**
     * 分页查询工种类型列表
     */
    @GetMapping
    @Operation(summary = "分页查询工种类型列表")
    public Result<PageInfo<WorkTypeVO>> pageQuery(WorkTypeQueryParam queryParam) {
        log.info("分页查询工种类型，参数：{}", queryParam);
        return workTypeService.pageQuery(queryParam);
    }

    /**
     * 查询工种类型详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询工种类型详情")
    public Result<WorkTypeVO> getById(@PathVariable Long id) {
        log.info("查询工种类型详情，id：{}", id);
        return workTypeService.getById(id);
    }

    /**
     * 新增工种类型
     */
    @PostMapping
    @Operation(summary = "新增工种类型")
    public Result<Long> add(@RequestBody WorkTypeDTO dto) {
        log.info("新增工种类型，参数：{}", dto);
        return workTypeService.add(dto);
    }

    /**
     * 编辑工种类型
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑工种类型")
    public Result<Long> update(@PathVariable Long id, @RequestBody WorkTypeDTO dto) {
        log.info("编辑工种类型，id：{}，参数：{}", id, dto);
        return workTypeService.update(id, dto);
    }

    /**
     * 删除工种类型
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除工种类型")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除工种类型，id：{}", id);
        return workTypeService.delete(id);
    }

    /**
     * 封存
     */
    @PostMapping("/{id}/seal")
    @Operation(summary = "封存工种类型")
    public Result<Boolean> seal(@PathVariable Long id) {
        log.info("封存工种类型，id：{}", id);
        return workTypeService.seal(id);
    }

    /**
     * 解封
     */
    @PostMapping("/{id}/unseal")
    @Operation(summary = "解封工种类型")
    public Result<Boolean> unseal(@PathVariable Long id) {
        log.info("解封工种类型，id：{}", id);
        return workTypeService.unseal(id);
    }
}
