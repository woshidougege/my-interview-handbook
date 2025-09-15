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
     * 主动从SSO接口获取用户信息，而不是依赖预设的属性
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
     * 获取当前登录用户名
     *
     * @return 用户名，未登录返回null
     */
    public static String getCurrentUsername() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getUserName() : null;
    }

    /**
     * 获取当前登录用户手机号
     *
     * @return 手机号，未登录返回null
     */
    public static String getCurrentUserPhone() {
        SSOUserInfo user = getCurrentUser();
        return user != null ? user.getPhonenumber() : null;
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
        return user != null ? user.getNickName() : null;
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
            log.debug("设置当前用户: userId={}, userName={}", 
                    user != null ? user.getUserId() : null, 
                    user != null ? user.getUserName() : null);
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
