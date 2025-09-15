package com.noah.superagent.util;

import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.convert.SSOUserInfoConvert;
import com.norinrd.interfaces.api.CommonInterface;
import com.norinrd.interfaces.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * SSO Token工具类
 * 专门用于拦截器中的Token验证，保持轻量级
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class SSOManager {

    @Value("${sa-token.sso.server-url:}")
    private String ssoServerUrl;
    
    @Autowired
    private CommonInterface commonInterface;

    /**
     * 从请求中获取用户信息（用于拦截器）
     */
    public SSOUserInfo getCurrentSSOUser() {
        try {
            // 直接调用SDK提供的getUser方法
            UserDto userData = commonInterface.getUser();
            if (userData != null) {
                SSOUserInfo ssoUserInfo = SSOUserInfoConvert.INSTANCE.toSSOUserInfo(userData);
                log.debug("成功转换用户信息: userId={}, userName={}", ssoUserInfo.getUserId(), ssoUserInfo.getUserName());
                return ssoUserInfo;
            }
            return null;
        } catch (Exception e) {
            log.error("SDK获取用户信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取SSO登录URL
     */
    public String getLoginUrl(String redirectUrl) {
        return ssoServerUrl + "/login?redirect=" + redirectUrl;
    }

}
