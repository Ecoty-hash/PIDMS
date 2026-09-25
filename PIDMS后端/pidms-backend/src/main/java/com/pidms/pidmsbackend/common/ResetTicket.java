package com.pidms.pidmsbackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 忘记密码的「待重置用户」——随 resetTicket 存在 Redis 中。
 * 只存 userId/username，不存明文手机号与密码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID（sys_user.id） */
    private Long userId;

    /** 用户名（仅用于日志） */
    private String username;
}
