package com.pidms.pidmsbackend.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易内存 token 存储：token(UUID) → 登录用户。
 * 服务重启后全部失效，需重新登录（符合"不复杂"的诉求）。
 */
@Component
public class TokenStore {

    private final Map<String, LoginUser> tokens = new ConcurrentHashMap<>();

    /** 生成 UUID token 并保存用户信息 */
    public String put(LoginUser user) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, user);
        return token;
    }

    public LoginUser get(String token) {
        return token == null ? null : tokens.get(token);
    }

    public void remove(String token) {
        if (token != null) tokens.remove(token);
    }
}
