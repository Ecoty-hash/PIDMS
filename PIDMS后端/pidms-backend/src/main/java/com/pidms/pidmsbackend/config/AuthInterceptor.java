package com.pidms.pidmsbackend.config;

import com.pidms.pidmsbackend.common.LoginUser;
import com.pidms.pidmsbackend.common.TokenStore;
import com.pidms.pidmsbackend.common.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：校验 Authorization: Bearer <uuid> 是否在内存 token 表中。
 * 未登录返回 401 JSON。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private TokenStore tokenStore;
    @Resource
    private UserContext userContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        LoginUser user = tokenStore.get(token);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }

        // 当前登录用户放入 request，Controller 可取用（如记录 createBy）
        userContext.setUser(user);
        return true;
    }
}
