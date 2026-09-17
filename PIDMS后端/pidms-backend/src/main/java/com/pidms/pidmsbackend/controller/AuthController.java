package com.pidms.pidmsbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pidms.pidmsbackend.common.LoginUser;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.TokenStore;
import com.pidms.pidmsbackend.dto.LoginDTO;
import com.pidms.pidmsbackend.entity.SysUser;
import com.pidms.pidmsbackend.mapper.SysUserMapper;
import com.pidms.pidmsbackend.vo.LoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证（简易版：UUID token + 内存存储 + 拦截器校验）。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TokenStore tokenStore;

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
