package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 忘记密码第二步：凭一次性票据设置新密码。
 */
@Data
public class ResetPasswordDTO {
    /** 第一步 /forgot-password/verify 返回的票据 */
    private String resetTicket;
    /** 新密码（明文传输，落库前 BCrypt 加密） */
    private String newPassword;
}
