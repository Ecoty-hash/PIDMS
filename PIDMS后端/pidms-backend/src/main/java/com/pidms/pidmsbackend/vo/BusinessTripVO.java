package com.pidms.pidmsbackend.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出差申请 VO（列表 / 详情通用）
 */
@Data
public class BusinessTripVO {

    /** 主键ID */
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

    /** 项目名称（联查 pm_project 回填，非落库字段） */
    private String projectName;

    /** 出差日期（由起止日期拼装展示，非落库字段） */
    private String tripDate;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
