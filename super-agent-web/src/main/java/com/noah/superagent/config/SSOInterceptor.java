package com.noah.superagent.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;


/**
 * SSO拦截器，用于拦截未登录用户并跳转到SSO登录页面
 */

@Component
public class SSOInterceptor implements HandlerInterceptor {

   /* @Autowired
    private CommonInterface commonInterface;*/

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查用户是否已登录
        // 记录请求信息
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullURL = requestURI + (queryString != null ? "?" + queryString : "");

        // 记录请求信息
        System.out.println("SSO拦截器处理请求: " + fullURL);

        if (!isUserLoggedIn(request)) {
            // 用户未登录，重定向到SSO登录页面

        /*    String redirectUrl = commonInterface.getLoginUrl();
            response.sendRedirect(redirectUrl);

            commonInterface.getUsersInfoByOrgId("1");*/
            return false;
        }
        return true;
    }


    private boolean isUserLoggedIn(HttpServletRequest request) {
        // 使用公司提供的SSO客户端接口检查登录状态
        try {
          /*  UserDto user = commonInterface.getUser();
            if (Objects.isNull(user)) {
                return false;
            }*/
        } catch (Exception e) {
            // 发生异常认为未登录
            return false;
        }
        return true;
    }
}
