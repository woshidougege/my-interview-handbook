package com.noah.superagent.websocket;

import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.util.SSOManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * WebSocket服务端配置器
 * 用于在WebSocket握手时进行用户认证验证
 *
 * @author 任相鹏
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
            // 临时跳过复杂的Spring上下文获取，直接允许连接
            // TODO: 后续需要正确实现Spring上下文获取和用户认证
            log.debug("跳过WebSocket握手认证，允许匿名连接（测试模式）");
            
            // 获取HttpSession信息并存储（如果有的话）
            Object sessionObj = request.getHttpSession();
            if (sessionObj instanceof HttpSession) {
                HttpSession session = (HttpSession) sessionObj;
                config.getUserProperties().put("httpSessionId", session.getId());
                log.debug("WebSocket连接关联HttpSession: {}", session.getId());
            }
            
            // 设置默认的测试用户信息
            config.getUserProperties().put("userId", "anonymous");
            config.getUserProperties().put("userName", "匿名用户");
            log.debug("设置匿名用户信息用于测试");
            
        } catch (Exception e) {
            log.error("WebSocket握手认证异常: {}", e.getMessage(), e);
        }
    }
}
