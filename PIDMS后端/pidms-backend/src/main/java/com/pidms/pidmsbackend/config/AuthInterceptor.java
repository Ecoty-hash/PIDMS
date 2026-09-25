package com.pidms.pidmsbackend.config;

import com.pidms.pidmsbackend.common.LoginUser;
import com.pidms.pidmsbackend.common.TokenStore;
import com.pidms.pidmsbackend.common.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：校验 Authorization: Bearer {token}。
 *
 * 校验通过后把登录用户写入项目统一的 {@link UserContext}（Service 里的 createBy/updateBy、
 * Controller 里的操作人都从它取），请求结束时清理，避免线程复用导致串号。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if(token == null) {
            response.setStatus(401);
            return false;
        }
        LoginUser loginUser = tokenStore.get(token);
        if(loginUser == null) {
            response.setStatus(401);
            return false;
        }
        tokenStore.renew(token);
        UserContext.setUser(loginUser);
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
