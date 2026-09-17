package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.PersonnelDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.PersonnelQueryParam;
import com.pidms.pidmsbackend.service.PersonnelService;
import com.pidms.pidmsbackend.vo.PersonnelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 人员管理 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/personnel")
@Tag(name = "人员管理接口")
public class PersonnelController {

    @Resource
    private PersonnelService personnelService;

    /**
     * 分页查询人员管理列表
     */
    @GetMapping
    @Operation(summary = "分页查询人员管理列表")
    public Result<PageInfo<PersonnelVO>> pageQuery(PersonnelQueryParam queryParam) {
        log.info("分页查询人员管理，参数：{}", queryParam);
        return personnelService.pageQuery(queryParam);
    }

    /**
     * 查询人员管理详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询人员管理详情")
    public Result<PersonnelVO> getById(@PathVariable Long id) {
        log.info("查询人员管理详情，id：{}", id);
        return personnelService.getById(id);
    }

    /**
     * 新增人员管理
     */
    @PostMapping
    @Operation(summary = "新增人员管理")
    public Result<Long> add(@RequestBody PersonnelDTO dto) {
        log.info("新增人员管理，参数：{}", dto);
        return personnelService.add(dto);
    }

    /**
     * 编辑人员管理
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑人员管理")
    public Result<Long> update(@PathVariable Long id, @RequestBody PersonnelDTO dto) {
        log.info("编辑人员管理，id：{}，参数：{}", id, dto);
        return personnelService.update(id, dto);
    }

    /**
     * 删除人员管理
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除人员管理")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除人员管理，id：{}", id);
        return personnelService.delete(id);
    }
}
