package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 审批动作请求体（出差 / 请假 / 补卡 三个申请类模块共用）
 * <p>对应接口文档「审批通过 / 审批驳回」的 { "opinion": "..." }。</p>
 */
@Data
public class ApprovalDTO {

    /** 审批意见（审批通过时可选，审批驳回时必填） */
    private String opinion;
}
