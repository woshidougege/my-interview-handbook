package com.noah.superagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.enums.SseEventType;
import com.noah.superagent.common.dto.SseMessageDto;
import com.noah.superagent.request.ChatMessageRequest;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.response.ChatMessageResponse;
import com.noah.superagent.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 聊天控制器
 * 统一处理聊天功能，支持同步HTTP请求和SSE流式响应
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    
    // 存储活跃的SSE连接
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    
    // 心跳调度器
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * 发送聊天消息（同步响应）
     * 
     * @param request 聊天消息请求
     * @return 聊天消息响应
     */
    @PostMapping("/send")
    public ApiResponse<ChatMessageResponse> sendMessage(@Validated @RequestBody ChatMessageRequest request) {
        log.info("收到聊天消息 - 工作空间: {}, 会话: {}, 内容长度: {}", 
                request.getWorkspaceId(), request.getSessionId(), request.getContent().length());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用AI服务获取回复
            String aiResponse = aiService.getAiResponse(
                    request.getContent(),
                    request.getWorkspaceId(),
                    request.getSessionId()
            );
            
            // 构建响应
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .messageId(UUID.randomUUID().toString())
                    .content(aiResponse)
                    .senderType("assistant")
                    .workspaceId(request.getWorkspaceId())
                    .sessionId(request.getSessionId())
                    .userId(request.getUserId())
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("聊天消息处理完成 - 工作空间: {}, 会话: {}, 耗时: {}ms, 回复长度: {}", 
                    request.getWorkspaceId(), request.getSessionId(), duration, aiResponse.length());
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("聊天消息处理失败 - 工作空间: {}, 会话: {}, 耗时: {}ms, 错误: {}", 
                     request.getWorkspaceId(), request.getSessionId(), duration, e.getMessage(), e);
            
            ChatMessageResponse errorResponse = ChatMessageResponse.builder()
                    .messageId(UUID.randomUUID().toString())
                    .content("抱歉，AI服务暂时不可用，请稍后再试。")
                    .senderType("system")
                    .workspaceId(request.getWorkspaceId())
                    .sessionId(request.getSessionId())
                    .userId(request.getUserId())
                    .timestamp(LocalDateTime.now())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            
            return ApiResponse.success("聊天服务异常", errorResponse);
        }
    }

    // ========== SSE流式聊天功能 ==========

    /**
     * 建立SSE连接
     * 
     * @param sessionId 会话ID
     * @param workspaceId 工作空间ID
     * @return SSE发射器
     */
    @GetMapping(value = "/sse/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String sessionId,
                             @RequestParam String workspaceId,
                             @RequestParam(required = false) String userId) {
        
        log.info("建立SSE连接 - 会话: {}, 工作空间: {}, 用户: {}", sessionId, workspaceId, userId);
        
        // 创建SSE发射器，设置30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 生成连接ID
        String connectionId = sessionId + "_" + System.currentTimeMillis();
        
        // 存储连接
        activeConnections.put(connectionId, emitter);
        
        // 设置连接完成或超时回调
        emitter.onCompletion(() -> {
            log.info("SSE连接完成 - 连接ID: {}", connectionId);
            activeConnections.remove(connectionId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE连接超时 - 连接ID: {}", connectionId);
            activeConnections.remove(connectionId);
        });
        
        emitter.onError((throwable) -> {
            log.error("SSE连接错误 - 连接ID: {}, 错误: {}", connectionId, throwable.getMessage());
            activeConnections.remove(connectionId);
        });
        
        try {
            // 发送连接确认消息
            SseMessageDto connectMessage = SseMessageDto.builder()
                    .messageId(UUID.randomUUID().toString())
                    .eventType(SseEventType.CONNECTED)
                    .content("连接建立成功")
                    .workspaceId(workspaceId)
                    .sessionId(sessionId)
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
            sendSseMessage(emitter, connectMessage);
            
            // 启动心跳
            startHeartbeat(connectionId, emitter, sessionId);
            
        } catch (Exception e) {
            log.error("发送连接确认消息失败 - 连接ID: {}", connectionId, e);
            activeConnections.remove(connectionId);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * 处理AI对话流式响应
     * 
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param workspaceId 工作空间ID
     */
    @PostMapping("/stream/{sessionId}")
    public void streamAiResponse(@PathVariable String sessionId,
                                @RequestParam String message,
                                @RequestParam String workspaceId,
                                @RequestParam(required = false) String userId) {
        
        log.info("开始AI流式对话 - 会话: {}, 工作空间: {}, 用户: {}, 消息长度: {}", 
                sessionId, workspaceId, userId, message.length());
        
        // 查找对应的SSE连接
        SseEmitter emitter = findEmitterBySessionId(sessionId);
        if (emitter == null) {
            log.warn("未找到会话对应的SSE连接 - 会话: {}", sessionId);
            return;
        }
        
        try {
            // 发送AI开始思考消息
            SseMessageDto thinkingMessage = SseMessageDto.builder()
                    .messageId(UUID.randomUUID().toString())
                    .eventType(SseEventType.AI_THINKING)
                    .content("AI正在思考中...")
                    .workspaceId(workspaceId)
                    .sessionId(sessionId)
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
            sendSseMessage(emitter, thinkingMessage);
            
            // 调用AI服务进行流式响应
            aiService.getAiResponseStream(
                message,
                workspaceId,
                sessionId,
                // onChunk: 处理流式数据片段
                (chunk) -> {
                    try {
                        SseMessageDto chunkMessage = SseMessageDto.builder()
                                .messageId(UUID.randomUUID().toString())
                                .eventType(SseEventType.AI_CHUNK)
                                .content(chunk)
                                .workspaceId(workspaceId)
                                .sessionId(sessionId)
                                .userId(userId)
                                .timestamp(LocalDateTime.now())
                                .build();
                                
                        sendSseMessage(emitter, chunkMessage);
                    } catch (Exception e) {
                        log.error("发送AI chunk消息失败 - 会话: {}", sessionId, e);
                    }
                },
                // onComplete: 流式响应完成
                () -> {
                    try {
                        SseMessageDto completeMessage = SseMessageDto.builder()
                                .messageId(UUID.randomUUID().toString())
                                .eventType(SseEventType.AI_COMPLETE)
                                .content("")
                                .workspaceId(workspaceId)
                                .sessionId(sessionId)
                                .userId(userId)
                                .timestamp(LocalDateTime.now())
                                .build();
                                
                        sendSseMessage(emitter, completeMessage);
                        log.info("AI流式对话完成 - 会话: {}", sessionId);
                    } catch (Exception e) {
                        log.error("发送AI完成消息失败 - 会话: {}", sessionId, e);
                    }
                },
                // onError: 错误处理
                (error) -> {
                    try {
                        SseMessageDto errorMessage = SseMessageDto.builder()
                                .messageId(UUID.randomUUID().toString())
                                .eventType(SseEventType.ERROR)
                                .content("AI服务错误")
                                .errorMessage(error)
                                .workspaceId(workspaceId)
                                .sessionId(sessionId)
                                .userId(userId)
                                .timestamp(LocalDateTime.now())
                                .build();
                                
                        sendSseMessage(emitter, errorMessage);
                        log.error("AI流式对话错误 - 会话: {}, 错误: {}", sessionId, error);
                    } catch (Exception e) {
                        log.error("发送AI错误消息失败 - 会话: {}", sessionId, e);
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("处理AI流式对话请求失败 - 会话: {}", sessionId, e);
            try {
                SseMessageDto errorMessage = SseMessageDto.builder()
                        .messageId(UUID.randomUUID().toString())
                        .eventType(SseEventType.ERROR)
                        .content("处理请求失败")
                        .errorMessage(e.getMessage())
                        .workspaceId(workspaceId)
                        .sessionId(sessionId)
                        .userId(userId)
                        .timestamp(LocalDateTime.now())
                        .build();
                        
                sendSseMessage(emitter, errorMessage);
            } catch (Exception sendException) {
                log.error("发送错误消息失败 - 会话: {}", sessionId, sendException);
            }
        }
    }

    // ========== SSE工具方法 ==========

    /**
     * 发送SSE消息
     */
    private void sendSseMessage(SseEmitter emitter, SseMessageDto message) throws IOException {
        String jsonData = objectMapper.writeValueAsString(message);
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .id(message.getMessageId())
                .name(message.getEventType().getEventType())
                .data(jsonData);
        emitter.send(eventBuilder);
    }

    /**
     * 根据会话ID查找SSE发射器
     */
    private SseEmitter findEmitterBySessionId(String sessionId) {
        return activeConnections.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionId + "_"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 启动心跳
     */
    private void startHeartbeat(String connectionId, SseEmitter emitter, String sessionId) {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (activeConnections.containsKey(connectionId)) {
                try {
                    SseMessageDto heartbeatMessage = SseMessageDto.builder()
                            .messageId(UUID.randomUUID().toString())
                            .eventType(SseEventType.HEARTBEAT)
                            .content("ping")
                            .sessionId(sessionId)
                            .timestamp(LocalDateTime.now())
                            .build();
                            
                    sendSseMessage(emitter, heartbeatMessage);
                } catch (Exception e) {
                    log.warn("发送心跳失败，移除连接 - 连接ID: {}", connectionId);
                    activeConnections.remove(connectionId);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}
