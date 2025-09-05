package com.noah.superagent.config;


import com.norinrd.interfaces.api.CommonInterface;
import com.norinrd.interfaces.dto.UserDto;
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

    @Autowired
    private CommonInterface commonInterface;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查用户是否已登录
        // 记录请求信息
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullURL = requestURI + (queryString != null ? "?" + queryString : "");
        // 对于WebSocket升级请求和静态资源，直接放行
        if (isWebSocketRequest(request) || isStaticResourceRequest(requestURI)) {
            System.out.println("WebSocket请求或静态资源请求，直接放行: " + fullURL);
            return true;
        }
        // 记录请求信息
        System.out.println("SSO拦截器处理请求: " + fullURL);

        if (!isUserLoggedIn(request)) {
            // 用户未登录，重定向到SSO登录页面
            /*String redirectUrl = commonInterface.getLoginUrl();
            response.sendRedirect(redirectUrl);*/
            commonInterface.getUsersInfoByOrgId("1");
            return false;
        }
        return true;
    }
    /**
     * 判断是否为WebSocket升级请求
     * @param request HTTP请求对象
     * @return 是否为WebSocket请求
     */
    private boolean isWebSocketRequest(HttpServletRequest request) {
        String upgradeHeader = request.getHeader("Upgrade");
        return "websocket".equalsIgnoreCase(upgradeHeader);
    }
    private boolean isStaticResourceRequest(String requestURI) {
        return requestURI.endsWith(".html") ||
                requestURI.endsWith(".css") ||
                requestURI.endsWith(".js") ||
                requestURI.startsWith("/static/") ||
                requestURI.startsWith("/ws/**");
    }

    /**
     * 判断用户是否已登录
     * @param request HTTP请求对象
     * @return 是否已登录
     */
    private boolean isUserLoggedIn(HttpServletRequest request) {
        // 使用公司提供的SSO客户端接口检查登录状态
        try {
            UserDto user = commonInterface.getUser();
            if (Objects.isNull(user)) {
                return false;
            }
        } catch (Exception e) {
            // 发生异常认为未登录
            return false;
        }
        return true;
    }
}
