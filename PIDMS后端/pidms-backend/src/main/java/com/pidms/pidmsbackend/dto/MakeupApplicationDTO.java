package com.pidms.pidmsbackend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请 新增 / 编辑 请求体
 */
@Data
public class MakeupApplicationDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 申请单号（未传时由后端生成） */
    private String applyNo;

    /** 申请人 */
    private String applicant;

    /** 缺卡日期 */
    private LocalDate cardMissDate;

    /** 缺卡类型：on-duty上班 / off-duty下班 / whole-day全天 */
    private String cardMissType;

    /** 缺卡时间 */
    private String cardMissTime;

    /** 补卡原因 */
    private String makeupReason;

    /** 证明人 */
    private String witness;

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
