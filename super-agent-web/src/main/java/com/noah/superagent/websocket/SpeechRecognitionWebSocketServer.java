package com.noah.superagent.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音识别WebSocket服务端
 * 使用Spring Boot官方推荐的@ServerEndpoint注解方式
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
@ServerEndpoint(value = "/api/v1/ws/speech-recognition", configurator = SpeechWebSocketConfig.class)
public class SpeechRecognitionWebSocketServer {

    /**
     * 存储会话信息
     */
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    
    /**
     * Jackson对象映射器
     */
    private static ObjectMapper objectMapper;
    
    /**
     * 语音识别服务
     */
    private static SpeechRecognitionService speechRecognitionService;
    

    /**
     * 依赖注入（静态注入方式）
     */
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        SpeechRecognitionWebSocketServer.objectMapper = objectMapper;
    }

    @Autowired
    public void setSpeechRecognitionService(SpeechRecognitionService speechRecognitionService) {
        SpeechRecognitionWebSocketServer.speechRecognitionService = speechRecognitionService;
    }


    /**
     * WebSocket连接建立
     */
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        sessionMap.put(sessionId, session);
        
        // 获取用户信息
        String userId = (String) session.getUserProperties().get("userId");
        String userName = (String) session.getUserProperties().get("userName");
        
        log.info("语音识别WebSocket连接建立: sessionId={}, userId={}, userName={}", 
                sessionId, userId, userName);
        
        try {
            // 验证用户是否已认证
            if (userId == null || userId.isEmpty()) {
                log.warn("用户未认证，拒绝WebSocket连接: {}", sessionId);
                sendMessage(session, Map.of(
                    "type", "error",
                    "message", "用户未认证，请先登录"
                ));
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "用户未认证"));
                return;
            }
            
            // 启动语音识别会话
            boolean started = speechRecognitionService.startRecognitionSession(sessionId, result -> {
                sendRecognitionResult(session, result);
            });
            
            if (!started) {
                log.error("启动语音识别会话失败: sessionId={}, userId={}", sessionId, userId);
                sendMessage(session, Map.of(
                    "type", "error",
                    "message", "启动识别服务失败"
                ));
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "启动识别服务失败"));
                return;
            }
            
            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", "connection",
                "status", "connected",
                "sessionId", sessionId,
                "userId", userId,
                "userName", userName != null ? userName : "未知用户",
                "message", "语音识别服务已准备就绪"
            ));
            
        } catch (Exception e) {
            log.error("处理WebSocket连接失败: sessionId={}, userId={}", sessionId, userId, e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "服务异常"));
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败: {}", sessionId, ex);
            }
        }
    }

    /**
     * 接收文本消息（控制命令）
     */
    @OnMessage
    public void onTextMessage(Session session, String message) {
        String sessionId = session.getId();
        log.debug("收到文本消息: 会话={}, 消息={}", sessionId, message);
        
        try {
            JsonNode command = objectMapper.readTree(message);
            String type = command.get("type").asText();
            
            switch (type) {
                case "start":
                    log.info("收到开始录音命令: {}", sessionId);
                    sendMessage(session, Map.of("type", "started", "message", "开始录音"));
                    break;
                    
                case "stop":
                    log.info("收到停止录音命令: {}", sessionId);
                    speechRecognitionService.endRecognitionSession(sessionId);
                    sendMessage(session, Map.of("type", "stopped", "message", "停止录音"));
                    break;
                    
                case "ping":
                    // 心跳检测
                    sendMessage(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
                    break;
                    
                default:
                    log.warn("未知的命令类型: {}, 会话: {}", type, sessionId);
                    sendMessage(session, Map.of("type", "error", "message", "未知命令: " + type));
            }
            
        } catch (Exception e) {
            log.error("处理文本消息失败: {}, 会话: {}", message, sessionId, e);
            sendMessage(session, Map.of("type", "error", "message", "处理命令失败"));
        }
    }

    /**
     * 接收二进制消息（音频数据）
     */
    @OnMessage
    public void onBinaryMessage(Session session, ByteBuffer message) {
        String sessionId = session.getId();
        byte[] audioData = message.array();
        
        log.debug("接收到音频数据: {} bytes, 会话: {}", audioData.length, sessionId);
        
        try {
            // 发送音频数据到识别服务
            boolean sent = speechRecognitionService.sendAudioData(sessionId, audioData);
            
            if (!sent) {
                log.warn("发送音频数据失败: {}", sessionId);
                sendMessage(session, Map.of(
                    "type", "warning",
                    "message", "音频数据处理失败"
                ));
            }
        } catch (Exception e) {
            log.error("处理音频数据失败: 会话: {}", sessionId, e);
            sendMessage(session, Map.of(
                "type", "error",
                "message", "音频处理异常"
            ));
        }
    }

    /**
     * WebSocket连接关闭
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String sessionId = session.getId();
        log.info("语音识别WebSocket连接关闭: 会话={}, 原因={}", sessionId, closeReason);
        
        try {
            // 清理识别会话
            speechRecognitionService.endRecognitionSession(sessionId);
        } catch (Exception e) {
            log.error("清理识别会话失败: {}", sessionId, e);
        } finally {
            // 清理会话映射
            sessionMap.remove(sessionId);
        }
    }

    /**
     * WebSocket连接错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String sessionId = session.getId();
        log.error("WebSocket连接错误: 会话={}", sessionId, error);
        
        try {
            sendMessage(session, Map.of(
                "type", "error",
                "message", "连接异常: " + error.getMessage()
            ));
        } catch (Exception e) {
            log.error("发送错误消息失败: {}", sessionId, e);
        }
        
        try {
            // 清理识别会话
            speechRecognitionService.endRecognitionSession(sessionId);
            sessionMap.remove(sessionId);
        } catch (Exception e) {
            log.error("清理异常会话失败: {}", sessionId, e);
        }
    }

    /**
     * 发送识别结果到前端
     */
    private void sendRecognitionResult(Session session, SpeechRecognitionResponse result) {
        if (!session.isOpen()) {
            log.warn("WebSocket会话已关闭，无法发送识别结果: {}", session.getId());
            return;
        }
        
        try {
            Map<String, Object> response = Map.of(
                "type", "recognition",
                "sessionId", result.getSessionId(),
                "status", result.getStatus().toString(),
                "text", result.getText() != null ? result.getText() : "",
                "isFinal", Boolean.TRUE.equals(result.getIsFinal()),
                "confidence", result.getConfidence() != null ? result.getConfidence() : 0.0,
                "timestamp", System.currentTimeMillis()
            );
            
            sendMessage(session, response);
            
            log.debug("发送识别结果: 会话={}, 文本={}, 最终={}", 
                    result.getSessionId(), result.getText(), result.getIsFinal());
                    
        } catch (Exception e) {
            log.error("发送识别结果失败: {}", session.getId(), e);
        }
    }

    /**
     * 发送消息到WebSocket客户端
     */
    private void sendMessage(Session session, Object message) {
        if (!session.isOpen()) {
            log.warn("WebSocket会话已关闭，无法发送消息: {}", session.getId());
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: {}", session.getId(), e);
        }
    }

    /**
     * 获取当前活跃会话数量
     */
    public static int getActiveSessionCount() {
        return sessionMap.size();
    }

    /**
     * 广播消息到所有活跃会话
     */
    public static void broadcastMessage(Object message) {
        if (objectMapper == null) {
            log.warn("ObjectMapper未初始化，无法广播消息");
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            for (Session session : sessionMap.values()) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(json);
                    } catch (IOException e) {
                        log.error("广播消息失败: {}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("序列化广播消息失败", e);
        }
    }
}
