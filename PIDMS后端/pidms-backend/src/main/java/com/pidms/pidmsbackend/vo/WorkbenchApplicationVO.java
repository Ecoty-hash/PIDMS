package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作台 - 我发起的申请
 */
@Data
public class WorkbenchApplicationVO {

    /** 模块 key，与前端 modules.js 的 key 一致，用于跳转 /{moduleKey} */
    private String moduleKey;

    private String moduleName;

    /** 单号：用印取 bill_no，其余取 apply_no */
    private String billNo;

    /** 标题/摘要：仅用印申请有 title，其余模块为 null */
    private String title;

    /** 审批状态编码：pending / approved / rejected */
    private String approvalStatus;

    /** 审批人，未指派时为 null */
    private String approver;

    /** 申请时间：用印取 apply_date，其余取 apply_time */
    private LocalDateTime applyTime;
}
