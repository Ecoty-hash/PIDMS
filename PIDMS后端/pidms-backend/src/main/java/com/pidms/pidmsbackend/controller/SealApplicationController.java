package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.SealApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SealApplicationQueryParam;
import com.pidms.pidmsbackend.service.SealApplicationService;
import com.pidms.pidmsbackend.vo.SealApplicationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/seal-applications")
public class SealApplicationController {

    @Resource
    private SealApplicationService sealApplicationService;

    /**
     * 分页查询用印申请列表
     */
    @GetMapping
    public Result<PageInfo<SealApplicationVO>> pageList(SealApplicationQueryParam queryParam) {
        log.info("分页查询用印申请，参数：{}", queryParam);
        return sealApplicationService.pageQuery(queryParam);
    }

    /**
     * 查询用印申请详情
     */
    @GetMapping("/{id}")
    public Result<SealApplicationVO> getById(@PathVariable Long id) {
        log.info("根据id查询用印申请，id：{}", id);
        return sealApplicationService.getById(id);
    }

    /**
     * 新增用印申请
     */
    @PostMapping
    public Result<Long> add(@RequestBody SealApplicationDTO dto) {
        log.info("新增用印申请，参数：{}", dto);
        return sealApplicationService.add(dto);
    }

    /**
     * 编辑用印申请
     */
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody SealApplicationDTO dto) {
        log.info("编辑用印申请，id：{}，参数：{}", id, dto);
        return sealApplicationService.update(id, dto);
    }

    /**
     * 删除用印申请
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除用印申请，id：{}", id);
        return sealApplicationService.delete(id);
    }

    /**
     * 审批通过（待审批 → 已审批）
     */
    @PostMapping("/{id}/approve")
    public Result<Boolean> approve(@PathVariable Long id) {
        log.info("审批通过用印申请，id：{}", id);
        return sealApplicationService.approve(id);
    }

    /**
     * 审批驳回（待审批 → 已拒绝）
     */
    @PostMapping("/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id) {
        log.info("驳回用印申请，id：{}", id);
        return sealApplicationService.reject(id);
    }
}
