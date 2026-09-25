package com.pidms.pidmsbackend.vo;

import lombok.Data;

/**
 * 忘记密码第一步返回：一次性重置票据。
 * 前端契约：data: { resetTicket, expiresIn }
 */
@Data
public class ResetTicketVO {
    /** 一次性票据，第二步改密时回传 */
    private String resetTicket;
    /** 票据有效期（秒） */
    private Integer expiresIn;
}
