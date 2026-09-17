package com.pidms.pidmsbackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录用户上下文（随 token 存在内存中）。
 */
@Data
@AllArgsConstructor
public class LoginUser {
    private String username;
    private String realName;
    private String role;
}
