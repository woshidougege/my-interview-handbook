package com.noah.superagent.websocket;

import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.util.SSOManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * WebSocket服务端配置器
 * 用于在WebSocket握手时进行用户认证验证
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
public class SpeechWebSocketConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, 
                               HandshakeRequest request, 
                               HandshakeResponse response) {
        
        log.debug("WebSocket握手认证开始");
        
        try {
            // 获取Spring应用上下文
            ApplicationContext applicationContext = ContextLoader.getCurrentWebApplicationContext();
            if (applicationContext == null) {
                log.error("无法获取Spring应用上下文");
                return;
            }
            
            // 获取SSO Manager
            SSOManager ssoManager = applicationContext.getBean(SSOManager.class);
            if (ssoManager == null) {
                log.error("无法获取SSOManager Bean");
                return;
            }
            
            // 获取HttpServletRequest
            HttpServletRequest httpRequest = (HttpServletRequest) request.getHttpSession();
            if (httpRequest != null) {
                // 设置请求上下文
                ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest);
                RequestContextHolder.setRequestAttributes(attributes);
                
                try {
                    // 验证用户身份
                    SSOUserInfo userInfo = ssoManager.getCurrentSSOUser();
                    
                    if (userInfo != null && userInfo.getUserId() != null) {
                        // 将用户信息存储到用户属性中
                        config.getUserProperties().put("userId", userInfo.getUserId());
                        config.getUserProperties().put("userName", userInfo.getUserName());
                        config.getUserProperties().put("userInfo", userInfo);
                        
                        log.info("WebSocket握手认证成功: userId={}, userName={}", 
                                userInfo.getUserId(), userInfo.getUserName());
                    } else {
                        log.warn("用户未登录，但允许WebSocket连接（将在消息处理时验证）");
                    }
                    
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
            
            // 获取HttpSession信息并存储
            HttpSession session = (HttpSession) request.getHttpSession();
            if (session != null) {
                config.getUserProperties().put("httpSessionId", session.getId());
                log.debug("WebSocket连接关联HttpSession: {}", session.getId());
            }
            
        } catch (Exception e) {
            log.error("WebSocket握手认证异常: {}", e.getMessage(), e);
        }
    }
}
