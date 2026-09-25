package com.pidms.pidmsbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pidms.pidmsbackend.common.LoginUser;
import com.pidms.pidmsbackend.common.ResetTicket;
import com.pidms.pidmsbackend.common.ResetTicketStore;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.TokenStore;
import com.pidms.pidmsbackend.dto.ForgotPasswordVerifyDTO;
import com.pidms.pidmsbackend.dto.LoginDTO;
import com.pidms.pidmsbackend.dto.ResetPasswordDTO;
import com.pidms.pidmsbackend.entity.SysUser;
import com.pidms.pidmsbackend.mapper.SysUserMapper;
import com.pidms.pidmsbackend.vo.LoginVO;
import com.pidms.pidmsbackend.vo.ResetTicketVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证（简易版：UUID token + Redis 存储 + 拦截器校验）。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 新密码长度限制（与前端提示保持一致） */
    private static final int PASSWORD_MIN_LENGTH = 4;
    private static final int PASSWORD_MAX_LENGTH = 32;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TokenStore tokenStore;

    @Resource
    private ResetTicketStore resetTicketStore;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录：校验用户名密码，成功返回 UUID token + 用户信息。
     * 前端契约：data: { token, user: { username, realName, role } }
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        String username = dto.getUsername();
        String password = dto.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error("用户名和密码不能为空");
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .eq(SysUser::getStatus, "enabled"));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败：用户名或密码错误 [{}]", username);
            return Result.error("用户名或密码错误");
        }

        String role = sysUserMapper.selectRoleName(user.getId());
        String token = tokenStore.put(new LoginUser(user.getUsername(), user.getName(), role));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getName());
        userInfo.put("role", role);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(userInfo);
        log.info("用户登录成功：[{}]", username);
        return Result.success(vo);
    }

    /**
     * 忘记密码第一步：校验「用户名 + 手机号」一一对应且账号启用（无短信验证码）。
     * 通过后签发一张 5 分钟内有效、只能用一次的票据，第二步改密时回传。
     * 前端契约：data: { resetTicket, expiresIn }
     */
    @PostMapping("/forgot-password/verify")
    public Result<ResetTicketVO> verifyForgotPassword(@RequestBody ForgotPasswordVerifyDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        if (username.isEmpty() || phone.isEmpty()) {
            return Result.error("用户名和手机号不能为空");
        }

        // 三个条件同时命中才算通过：username 匹配、phone 匹配、status 启用
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getPhone, phone)
                .eq(SysUser::getStatus, "enabled"));
        // 账号不存在 / 手机号不对 / 账号已停用，统一成一句文案，避免被拿来枚举账号
        if (user == null) {
            log.warn("重置密码身份校验失败（用户名与手机号不匹配或账号停用）：[{}]", username);
            return Result.error("用户名与手机号不匹配，或账号已停用，请核对后重试");
        }
        //校验通过，生成ticket
        String ticket = resetTicketStore.issue(user.getId(), user.getUsername());
        ResetTicketVO vo = new ResetTicketVO();
        vo.setResetTicket(ticket);
        vo.setExpiresIn((int) ResetTicketStore.TICKET_TTL_SECONDS);
        log.info("重置密码身份校验通过，已签发票据：[{}]", username);
        return Result.success(vo);
    }

    /**
     * 忘记密码第二步：凭票据设置新密码，BCrypt 加密后写回 sys_user.password。
     * 票据用后即焚，改密成功与否都不能复用。
     * 前端契约：body { resetTicket, newPassword }
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordDTO dto) {
        String ticket = dto.getResetTicket() == null ? "" : dto.getResetTicket().trim();
        String newPassword = dto.getNewPassword() == null ? "" : dto.getNewPassword();
        if (ticket.isEmpty()) {
            return Result.error("重置票据无效，请重新验证身份");
        }
        // 密码不做 trim：首尾空格属于密码本身
        if (newPassword.length() < PASSWORD_MIN_LENGTH || newPassword.length() > PASSWORD_MAX_LENGTH) {
            return Result.error("新密码长度需为 " + PASSWORD_MIN_LENGTH + "-" + PASSWORD_MAX_LENGTH + " 位");
        }

        ResetTicket resetTicket = resetTicketStore.consume(ticket);
        if (resetTicket == null) {
            return Result.error("重置票据已过期或已使用，请重新验证身份");
        }

        // 票据签发后账号可能被停用，落库前再确认一次
        SysUser user = sysUserMapper.selectById(resetTicket.getUserId());
        if (user == null || !"enabled".equals(user.getStatus())) {
            log.warn("重置密码失败，账号不存在或已停用：[{}]", resetTicket.getUsername());
            return Result.error("账号不存在或已停用，无法重置密码");
        }

        // 只更新 password / update_by 两列，其余字段留空即不参与更新
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdateBy(user.getUsername());
        sysUserMapper.updateById(update);

        log.info("用户重置密码成功：[{}]", user.getUsername());
        return Result.success(null);
    }

    /** 退出登录：使当前 token 失效 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) tokenStore.remove(token);
        return Result.success(null);
    }

    /** 从 Authorization 头解析 Bearer token */
    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
