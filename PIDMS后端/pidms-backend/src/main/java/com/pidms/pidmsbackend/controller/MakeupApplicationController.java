package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ApprovalDTO;
import com.pidms.pidmsbackend.dto.MakeupApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.MakeupApplicationQueryParam;
import com.pidms.pidmsbackend.service.MakeupApplicationService;
import com.pidms.pidmsbackend.vo.MakeupApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 补卡申请 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/makeup-applications")
@Tag(name = "补卡申请接口")
public class MakeupApplicationController {

    @Resource
    private MakeupApplicationService makeupApplicationService;

    /**
     * 分页查询补卡申请列表
     */
    @GetMapping
    @Operation(summary = "分页查询补卡申请列表")
    public Result<PageInfo<MakeupApplicationVO>> pageQuery(MakeupApplicationQueryParam queryParam) {
        log.info("分页查询补卡申请，参数：{}", queryParam);
        return makeupApplicationService.pageQuery(queryParam);
    }

    /**
     * 查询补卡申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询补卡申请详情")
    public Result<MakeupApplicationVO> getById(@PathVariable Long id) {
        log.info("查询补卡申请详情，id：{}", id);
        return makeupApplicationService.getById(id);
    }

    /**
     * 新增补卡申请
     */
    @PostMapping
    @Operation(summary = "新增补卡申请")
    public Result<Long> add(@RequestBody MakeupApplicationDTO dto) {
        log.info("新增补卡申请，参数：{}", dto);
        return makeupApplicationService.add(dto);
    }

    /**
     * 编辑补卡申请
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑补卡申请")
    public Result<Long> update(@PathVariable Long id, @RequestBody MakeupApplicationDTO dto) {
        log.info("编辑补卡申请，id：{}，参数：{}", id, dto);
        return makeupApplicationService.update(id, dto);
    }

    /**
     * 删除补卡申请
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除补卡申请")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除补卡申请，id：{}", id);
        return makeupApplicationService.delete(id);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Boolean> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalDTO approvalDTO) {
        String opinion = approvalDTO == null ? null : approvalDTO.getOpinion();
        log.info("审批通过补卡申请，id：{}，审批意见：{}", id, opinion);
        return makeupApplicationService.approve(id, opinion);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批驳回")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalDTO approvalDTO) {
        String opinion = approvalDTO == null ? null : approvalDTO.getOpinion();
        log.info("驳回补卡申请，id：{}，驳回原因：{}", id, opinion);
        return makeupApplicationService.reject(id, opinion);
    }
}
