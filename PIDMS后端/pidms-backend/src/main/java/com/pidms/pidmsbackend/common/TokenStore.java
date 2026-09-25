package com.pidms.pidmsbackend.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录 token 存储：token(UUID) → 登录用户，存 Redis 并带过期时间。
 * 服务重启不掉线；超过 TTL 无活动则需重新登录。
 */
@Component
public class TokenStore {

    // redis key前缀
    private static final String TOKEN_PREFIX = "pidms:token:";
    // token 过期时间（秒）
    private static final long TOKEN_TTL_SECONDS = 3600;

    @Autowired
    private RedisTemplate redisTemplate;

    /** 生成 UUID token 并保存用户信息 */
    public String put(LoginUser user) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, user, TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    /** 读取 token 对应的登录用户；不存在或已过期返回 null */
    public LoginUser get(String token) {
        if (token == null || token.isBlank()) return null;
        return (LoginUser) redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
    }

    /** 使 token 失效（退出登录） */
    public void remove(String token) {
        if (token == null || token.isBlank()) return;
        redisTemplate.delete(TOKEN_PREFIX + token);
    }

    /** 每次请求续期，活跃用户不掉线 */
    public boolean renew(String token) {
        if (token == null || token.isBlank()) return false;
        return Boolean.TRUE.equals(redisTemplate.expire(TOKEN_PREFIX + token, TOKEN_TTL_SECONDS, TimeUnit.SECONDS));
    }
}
