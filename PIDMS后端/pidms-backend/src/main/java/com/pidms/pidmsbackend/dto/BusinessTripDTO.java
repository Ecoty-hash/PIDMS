package com.pidms.pidmsbackend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出差申请 新增 / 编辑 请求体
 */
@Data
public class BusinessTripDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 申请单号（未传时由后端生成） */
    private String applyNo;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 申请人 */
    private String applicant;

    /** 出差地点 */
    private String tripDestination;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 出差事由 */
    private String tripReason;

    /** 附件（文件名，多个以顿号分隔） */
    private String attachments;

    /** 申请时间（由后端补全） */
    private LocalDateTime applyTime;

    /** 审批状态：pending未审批 / approved已通过 / rejected已驳回 */
    private String approvalStatus;

    /** 审批人（由审批动作补全） */
    private String approver;

    /** 审批意见（由审批动作补全） */
    private String approvalOpinion;

    /** 审批时间（由审批动作补全） */
    private LocalDateTime approvalTime;
}
