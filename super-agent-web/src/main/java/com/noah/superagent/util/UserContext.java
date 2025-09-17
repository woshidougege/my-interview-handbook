package com.noah.superagent.util;

import com.noah.superagent.model.SSOUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具类
 * 提供便捷的方法获取当前登录用户信息（主动请求SSO接口）
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class UserContext {

    private static SSOManager ssoManager;

    @Autowired
    public void setSsoService(SSOManager ssoManager) {
        UserContext.ssoManager = ssoManager;
    }



    /**
     * 请求属性中用户信息的键名
     */
    private static final String USER_ATTRIBUTE_KEY = "CURRENT_SSO_USER";

    /**
     * 获取当前HTTP请求对象
     *
     * @return HttpServletRequest对象，如果不在Web上下文中返回null
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前登录用户ID（SSO用户ID转Long）
     *
     * @return 用户ID（Long类型），未登录返回null
     */
    public static Long getCurrentUserId() {
        SSOUserInfo user = getCurrentUser();
        if (user != null && user.getUserId() != null) {
            try {
                return Long.valueOf(user.getUserId());
            } catch (NumberFormatException e) {
                log.warn("用户ID转换失败: {}", user.getUserId());
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户信息
     * 主动从SSO接口获取用户信息
     * 
     * @return 当前SSO用户信息，未登录返回null
     */
    public static SSOUserInfo getCurrentUser() {
        // 1. 优先从请求属性中获取（如果拦截器已经设置）
        HttpServletRequest request = getCurrentRequest();
        
         // 2. 如果请求属性中没有，则主动请求SSO接口获取
         if (ssoManager != null) {
             try {
                 SSOUserInfo user = ssoManager.getCurrentSSOUser();
                if (user != null && request != null) {
                    // 获取到用户信息后缓存到请求属性中，避免重复请求
                    request.setAttribute(USER_ATTRIBUTE_KEY, user);
                }
                return user;
            } catch (Exception e) {
                log.warn("主动获取SSO用户信息失败: {}", e.getMessage());
            }
        }
        
        return null;
    }


    /**
     * 获取当前用户ID，如果未登录则抛出异常
     *
     * @return 用户ID（Long类型）
     * @throws IllegalStateException 用户未登录时
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userId;
    }

}
