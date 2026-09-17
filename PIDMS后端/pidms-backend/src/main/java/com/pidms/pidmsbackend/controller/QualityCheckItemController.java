package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.QualityCheckItemDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityCheckItemQueryParam;
import com.pidms.pidmsbackend.service.QualityCheckItemService;
import com.pidms.pidmsbackend.vo.QualityCheckItemVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 质量检查项 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/quality-check-items")
public class QualityCheckItemController {

    @Resource
    private QualityCheckItemService qualityCheckItemService;

    /**
     * 分页查询质量检查项列表
     */
    @GetMapping
    public Result<PageInfo<QualityCheckItemVO>> pageList(QualityCheckItemQueryParam queryParam) {
        log.info("分页查询质量检查项，参数：{}", queryParam);
        return qualityCheckItemService.pageQuery(queryParam);
    }

    /**
     * 查询质量检查项详情
     */
    @GetMapping("/{id}")
    public Result<QualityCheckItemVO> getById(@PathVariable Long id) {
        log.info("根据id查询质量检查项，id：{}", id);
        return qualityCheckItemService.getById(id);
    }

    /**
     * 新增质量检查项
     */
    @PostMapping
    public Result<Long> add(@RequestBody QualityCheckItemDTO dto) {
        log.info("新增质量检查项，参数：{}", dto);
        return qualityCheckItemService.add(dto);
    }

    /**
     * 编辑质量检查项
     */
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody QualityCheckItemDTO dto) {
        log.info("编辑质量检查项，id：{}，参数：{}", id, dto);
        return qualityCheckItemService.update(id, dto);
    }

    /**
     * 删除质量检查项
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除质量检查项，id：{}", id);
        return qualityCheckItemService.delete(id);
    }

    /**
     * 封存质量检查项
     */
    @PostMapping("/{id}/seal")
    public Result<Boolean> seal(@PathVariable Long id) {
        log.info("封存质量检查项，id：{}", id);
        return qualityCheckItemService.seal(id);
    }
}
