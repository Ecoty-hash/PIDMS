package com.pidms.pidmsbackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户上下文（随 token 存在 Redis 中）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {
    // 序列化版本号
    @Serial
    private static final long serialVersionUID = 1L;
    private String username;
    private String realName;
    private String role;
}
