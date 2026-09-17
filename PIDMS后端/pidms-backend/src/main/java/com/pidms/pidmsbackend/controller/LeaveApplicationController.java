package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ApprovalDTO;
import com.pidms.pidmsbackend.dto.LeaveApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.LeaveApplicationQueryParam;
import com.pidms.pidmsbackend.service.LeaveApplicationService;
import com.pidms.pidmsbackend.vo.LeaveApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 请假申请 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/leave-applications")
@Tag(name = "请假申请接口")
public class LeaveApplicationController {

    @Resource
    private LeaveApplicationService leaveApplicationService;

    /**
     * 分页查询请假申请列表
     */
    @GetMapping
    @Operation(summary = "分页查询请假申请列表")
    public Result<PageInfo<LeaveApplicationVO>> pageQuery(LeaveApplicationQueryParam queryParam) {
        log.info("分页查询请假申请，参数：{}", queryParam);
        return leaveApplicationService.pageQuery(queryParam);
    }

    /**
     * 查询请假申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询请假申请详情")
    public Result<LeaveApplicationVO> getById(@PathVariable Long id) {
        log.info("查询请假申请详情，id：{}", id);
        return leaveApplicationService.getById(id);
    }

    /**
     * 新增请假申请
     */
    @PostMapping
    @Operation(summary = "新增请假申请")
    public Result<Long> add(@RequestBody LeaveApplicationDTO dto) {
        log.info("新增请假申请，参数：{}", dto);
        return leaveApplicationService.add(dto);
    }

    /**
     * 编辑请假申请
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑请假申请")
    public Result<Long> update(@PathVariable Long id, @RequestBody LeaveApplicationDTO dto) {
        log.info("编辑请假申请，id：{}，参数：{}", id, dto);
        return leaveApplicationService.update(id, dto);
    }

    /**
     * 删除请假申请
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除请假申请")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除请假申请，id：{}", id);
        return leaveApplicationService.delete(id);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Boolean> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalDTO approvalDTO) {
        String opinion = approvalDTO == null ? null : approvalDTO.getOpinion();
        log.info("审批通过请假申请，id：{}，审批意见：{}", id, opinion);
        return leaveApplicationService.approve(id, opinion);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批驳回")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalDTO approvalDTO) {
        String opinion = approvalDTO == null ? null : approvalDTO.getOpinion();
        log.info("驳回请假申请，id：{}，驳回原因：{}", id, opinion);
        return leaveApplicationService.reject(id, opinion);
    }
}
