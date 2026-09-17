package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SealTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SealTypeQueryParam;
import com.pidms.pidmsbackend.service.SealTypeService;
import com.pidms.pidmsbackend.vo.SealTypeVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/seal-types")
public class SealTypeController {

    @Resource
    private SealTypeService sealTypeService;

    /**
     * 分页查询印章类型列表
     */
    @GetMapping
    public Result<PageInfo<SealTypeVO>> pageList(SealTypeQueryParam queryParam) {
        log.info("分页查询印章类型，参数：{}", queryParam);
        return sealTypeService.pageQuery(queryParam);
    }

    /**
     * 查询印章类型详情
     */
    @GetMapping("/{id}")
    public Result<SealTypeVO> getById(@PathVariable Long id) {
        log.info("根据id查询印章类型，id：{}", id);
        return sealTypeService.getById(id);
    }

    /**
     * 新增印章类型
     */
    @PostMapping
    public Result<Long> add(@RequestBody SealTypeDTO dto) {
        log.info("新增印章类型，参数：{}", dto);
        return sealTypeService.add(dto);
    }

    /**
     * 编辑印章类型
     */
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody SealTypeDTO dto) {
        log.info("编辑印章类型，id：{}，参数：{}", id, dto);
        return sealTypeService.update(id, dto);
    }

    /**
     * 删除印章类型
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除印章类型，id：{}", id);
        return sealTypeService.delete(id);
    }

    /**
     * 封存印章类型
     */
    @PostMapping("/{id}/seal")
    public Result<Boolean> seal(@PathVariable Long id) {
        log.info("封存印章类型，id：{}", id);
        return sealTypeService.seal(id);
    }

    /**
     * 解封印章类型
     */
    @PostMapping("/{id}/unseal")
    public Result<Boolean> unseal(@PathVariable Long id) {
        log.info("解封印章类型，id：{}", id);
        return sealTypeService.unseal(id);
    }
}
