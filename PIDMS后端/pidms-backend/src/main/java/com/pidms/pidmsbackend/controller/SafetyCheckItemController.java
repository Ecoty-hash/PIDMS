package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SafetyCheckItemDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyCheckItemQueryParam;
import com.pidms.pidmsbackend.service.SafetyCheckItemService;
import com.pidms.pidmsbackend.vo.SafetyCheckItemVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 安全检查项 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/safety-check-items")
public class SafetyCheckItemController {

    @Resource
    private SafetyCheckItemService safetyCheckItemService;

    /**
     * 分页查询安全检查项列表
     */
    @GetMapping
    public Result<PageInfo<SafetyCheckItemVO>> pageList(SafetyCheckItemQueryParam queryParam) {
        log.info("分页查询安全检查项，参数：{}", queryParam);
        return safetyCheckItemService.pageQuery(queryParam);
    }

    /**
     * 查询安全检查项详情
     */
    @GetMapping("/{id}")
    public Result<SafetyCheckItemVO> getById(@PathVariable Long id) {
        log.info("根据id查询安全检查项，id：{}", id);
        return safetyCheckItemService.getById(id);
    }

    /**
     * 新增安全检查项
     */
    @PostMapping
    public Result<Long> add(@RequestBody SafetyCheckItemDTO dto) {
        log.info("新增安全检查项，参数：{}", dto);
        return safetyCheckItemService.add(dto);
    }

    /**
     * 编辑安全检查项
     */
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody SafetyCheckItemDTO dto) {
        log.info("编辑安全检查项，id：{}，参数：{}", id, dto);
        return safetyCheckItemService.update(id, dto);
    }

    /**
     * 删除安全检查项
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除安全检查项，id：{}", id);
        return safetyCheckItemService.delete(id);
    }

    /**
     * 封存安全检查项
     */
    @PostMapping("/{id}/seal")
    public Result<Boolean> seal(@PathVariable Long id) {
        log.info("封存安全检查项，id：{}", id);
        return safetyCheckItemService.seal(id);
    }
}
