package com.pidms.pidmsbackend.common;

import org.springframework.stereotype.Component;

@Component
public class UserContext {
    private static final ThreadLocal<LoginUser> USER_THREAD = new ThreadLocal<>();

    public static void setUser(LoginUser loginUser){
        USER_THREAD.set(loginUser);
    }

    public static LoginUser getUser(){
        return USER_THREAD.get();
    }

    public static String getRole(){
        LoginUser user = USER_THREAD.get();
        return user == null ? null : user.getRole();
    }

    public static void clear(){
        USER_THREAD.remove();
    }
}
