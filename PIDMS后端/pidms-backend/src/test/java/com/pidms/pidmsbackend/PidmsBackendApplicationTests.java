package com.pidms.pidmsbackend;

import com.pidms.pidmsbackend.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class PidmsBackendApplicationTests {
    @Autowired
    private RedisTemplate redisTemplate;
    @Test
    void contextLoads() {
    }
    @Test
    public void testRedisConnection() {
        // 写入redis，10秒过期
        redisTemplate.opsForValue().set("test:msg", "redis连接成功",10, TimeUnit.SECONDS);
        // 读取值
        String val = (String) redisTemplate.opsForValue().get("test:msg");

        // 控制台打印结果
        System.out.println("redis读取结果：" + val);

        // Junit断言：判断不为空，并且内容匹配，不匹配直接测试失败
        assertNotNull(val);
        assertEquals("redis连接成功", val);
    }
}
