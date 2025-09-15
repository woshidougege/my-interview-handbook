package com.noah.superagent.util;

import com.noah.superagent.model.SSOUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具类
 * 提供便捷的方法获取当前登录用户信息（基于SSO）
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
public class UserContext {

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
        return user != null && user.getUserId() != null ? user.getUserId().longValue() : null;
    }

    /**
     * 获取当前登录用户信息
     * 
     * 注意：此方法依赖于拦截器或过滤器预先将用户信息设置到请求属性中
     * 建议在用户认证拦截器中调用 setCurrentUser 方法
     *
     * @return 当前SSO用户信息，未登录返回null
     */
    public static SSOUserInfo getCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.debug("无法获取当前HTTP请求，可能不在Web上下文中");
            return null;
        }

        Object userObj = request.getAttribute(USER_ATTRIBUTE_KEY);
        if (userObj instanceof SSOUserInfo) {
            return (SSOUserInfo) userObj;
        }

        log.debug("请求属性中未找到用户信息，用户可能未登录");
        return null;
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名，未登录返回null
     */
    public static String getCurrentUsername() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前登录用户手机号
     *
     * @return 手机号，未登录返回null
     */
    public static String getCurrentUserPhone() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getPhone() : null;
    }

    /**
     * 获取当前登录用户邮箱
     *
     * @return 邮箱，未登录返回null
     */
    public static String getCurrentUserEmail() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * 获取当前登录用户真实姓名
     *
     * @return 真实姓名，未登录返回null
     */
    public static String getCurrentUserRealName() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getRealName() : null;
    }

    /**
     * 获取当前登录用户组织ID
     *
     * @return 组织ID，未登录返回null
     */
    public static String getCurrentUserOrgId() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getOrgId() : null;
    }

    /**
     * 设置当前用户信息到请求属性中
     * 通常在认证拦截器或过滤器中调用
     *
     * @param user SSO用户信息
     */
    public static void setCurrentUser(SSOUserInfo user) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            request.setAttribute(USER_ATTRIBUTE_KEY, user);
            log.debug("设置当前用户: userId={}, username={}", 
                    user != null ? user.getUserId() : null, 
                    user != null ? user.getUsername() : null);
        } else {
            log.warn("无法设置用户信息：未找到当前HTTP请求");
        }
    }

    /**
     * 清除当前用户信息
     */
    public static void clearCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            request.removeAttribute(USER_ATTRIBUTE_KEY);
            log.debug("清除当前用户信息");
        }
    }

    /**
     * 检查是否有用户登录
     *
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn() {
        return getCurrentUser() != null;
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

    /**
     * 获取当前用户信息，如果未登录则抛出异常
     *
     * @return SSO用户信息
     * @throws IllegalStateException 用户未登录时
     */
    public static SSOUserInfo requireCurrentUser() {
        SSOUserInfo user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("用户未登录");
        }
        return user;
    }

}
