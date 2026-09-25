package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 忘记密码第一步：用户名 + 手机号。
 * 后端要求二者在 sys_user 中一一对应，且账号为启用状态；无短信验证码。
 */
@Data
public class ForgotPasswordVerifyDTO {
    private String username;
    private String phone;
}
