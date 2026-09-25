package com.pidms.pidmsbackend.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 忘记密码的一次性票据存储：resetTicket(UUID) → 待重置用户。
 * 存 Redis、5 分钟过期；consume() 取出后立刻删除，同一张票据只能用一次。
 */
@Component
public class ResetTicketStore {

    // redis key前缀
    private static final String TICKET_PREFIX = "pidms:reset-ticket:";
    // 票据有效期（秒）——前后端共用，接口返回的 expiresIn 取自这里
    public static final long TICKET_TTL_SECONDS = 300;

    @Autowired
    private RedisTemplate redisTemplate;

    /** 身份校验通过后签发一张票据 */
    public String issue(Long userId, String username) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TICKET_PREFIX + ticket, new ResetTicket(userId, username), TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        return ticket;
    }

    /**
     * 取出并立即删除票据；票据不存在 / 已过期 / 已用过都返回 null。
     * 极端并发下（同一张票据被同时提交两次）可能都通过，代价仅是重复设置一次新密码，可接受。
     */
    public ResetTicket consume(String ticket) {
        if (ticket == null || ticket.isBlank()) return null;
        String key = TICKET_PREFIX + ticket;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return null;
        redisTemplate.delete(key);
        return (ResetTicket) value;
    }
}
