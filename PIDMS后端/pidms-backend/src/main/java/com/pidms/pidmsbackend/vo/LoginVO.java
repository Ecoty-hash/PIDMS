package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.util.Map;

/**
 * 登录返回：token + 用户信息。
 * 前端契约：data: { token, user: { username, realName, role } }
 */
@Data
public class LoginVO {
    private String token;
    private Map<String, Object> user;
}
