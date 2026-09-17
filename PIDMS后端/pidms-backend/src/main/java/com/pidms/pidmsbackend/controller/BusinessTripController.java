package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ApprovalDTO;
import com.pidms.pidmsbackend.dto.BusinessTripDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.BusinessTripQueryParam;
import com.pidms.pidmsbackend.service.BusinessTripService;
import com.pidms.pidmsbackend.vo.BusinessTripVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 出差申请 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/business-trips")
@Tag(name = "出差申请接口")
public class BusinessTripController {

    @Resource
    private BusinessTripService businessTripService;

    /**
     * 分页查询出差申请列表
     */
    @GetMapping
    @Operation(summary = "分页查询出差申请列表")
    public Result<PageInfo<BusinessTripVO>> pageQuery(BusinessTripQueryParam queryParam) {
        log.info("分页查询出差申请，参数：{}", queryParam);
        return businessTripService.pageQuery(queryParam);
    }

    /**
     * 查询出差申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询出差申请详情")
    public Result<BusinessTripVO> getById(@PathVariable Long id) {
        log.info("查询出差申请详情，id：{}", id);
        return businessTripService.getById(id);
    }

    /**
     * 新增出差申请
     */
    @PostMapping
    @Operation(summary = "新增出差申请")
    public Result<Long> add(@RequestBody BusinessTripDTO dto) {
        log.info("新增出差申请，参数：{}", dto);
        return businessTripService.add(dto);
    }

    /**
     * 编辑出差申请
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑出差申请")
    public Result<Long> update(@PathVariable Long id, @RequestBody BusinessTripDTO dto) {
        log.info("编辑出差申请，id：{}，参数：{}", id, dto);
        return businessTripService.update(id, dto);
    }

    /**
     * 删除出差申请
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除出差申请")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除出差申请，id：{}", id);
        return businessTripService.delete(id);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Boolean> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalDTO approvalDTO) {
        String opinion = approvalDTO == null ? null : approvalDTO.getOpinion();
        log.info("审批通过出差申请，id：{}，审批意见：{}", id, opinion);
        return businessTripService.approve(id, opinion);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批驳回")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalDTO approvalDTO) {
        String opinion = approvalDTO == null ? null : approvalDTO.getOpinion();
        log.info("驳回出差申请，id：{}，驳回原因：{}", id, opinion);
        return businessTripService.reject(id, opinion);
    }
}
