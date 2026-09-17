package com.pidms.pidmsbackend.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请 VO（列表 / 详情通用）
 */
@Data
public class LeaveApplicationVO {

    /** 主键ID */
    private Long id;

    /** 申请单号（未传时由后端生成） */
    private String applyNo;

    /** 申请人 */
    private String applicant;

    /** 请假类型：annual年假 / personal事假 / sick病假 / marriage婚假 / maternity产假 / compensatory调休 */
    private String leaveType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 请假天数（由后端按起止日期重算，忽略客户端传值） */
    private BigDecimal leaveDays;

    /** 请假原因 */
    private String leaveReason;

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

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
