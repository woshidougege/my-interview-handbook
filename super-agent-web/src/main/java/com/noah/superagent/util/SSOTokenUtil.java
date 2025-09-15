package com.noah.superagent.util;

import com.noah.superagent.model.SSOUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * SSO Token工具类
 * 专门用于拦截器中的Token验证，保持轻量级
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class SSOTokenUtil {

    @Value("${sso.server.url:http://localhost:8080}")
    private String ssoServerUrl;

    private final RestTemplate restTemplate;

    public SSOTokenUtil(@Lazy RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 从请求中获取用户信息（用于拦截器）
     */
    public SSOUserInfo getCurrentSSOUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }

        return validateTokenAndGetUser(token);
    }

    /**
     * 获取SSO登录URL
     */
    public String getLoginUrl(String redirectUrl) {
        return ssoServerUrl + "/login?redirect=" + redirectUrl;
    }

    /**
     * 从请求中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. 从Header获取
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        // 2. 从Cookie获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SSO_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. 从参数获取
        token = request.getParameter("token");
        if (token != null && !token.trim().isEmpty()) {
            return token;
        }

        return null;
    }

    /**
     * 验证Token并获取用户信息
     */
    private SSOUserInfo validateTokenAndGetUser(String token) {
        try {
            String url = ssoServerUrl + "/api/user/current";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object data = responseBody.get("data");
                if (data instanceof Map) {
                    return parseUserInfo((Map<String, Object>) data);
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析用户信息
     */
    private SSOUserInfo parseUserInfo(Map<String, Object> userMap) {
        try {
            SSOUserInfo userInfo = new SSOUserInfo();
            userInfo.setUserId((Integer) userMap.get("userId"));
            userInfo.setUsername((String) userMap.get("username"));
            userInfo.setPhone((String) userMap.get("phone"));
            userInfo.setEmail((String) userMap.get("email"));
            userInfo.setRealName((String) userMap.get("realName"));
            userInfo.setOrgId((String) userMap.get("orgId"));
            userInfo.setOrgName((String) userMap.get("orgName"));
            userInfo.setStatus((Integer) userMap.get("status"));
            
            return userInfo;
        } catch (Exception e) {
            log.error("解析用户信息失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
