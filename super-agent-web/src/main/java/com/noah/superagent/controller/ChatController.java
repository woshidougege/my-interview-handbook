package com.noah.superagent.controller;

import com.noah.superagent.ai.service.AiService;
import com.noah.superagent.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI聊天控制器
 * 提供流式聊天功能
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Tag(name = "AI聊天", description = "AI聊天相关接口")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    /**
     * 聊天消息请求
     */
    @Setter
    @Getter
    public static class ChatRequest {
        private List<MessageDto> messages;
        private String workspaceId;

    }

    /**
     * 消息DTO
     */
    @Setter
    @Getter
    public static class MessageDto {
        private String role;
        private String content;

    }

    /**
     * 流式聊天接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "与AI进行流式对话，实时返回回答内容")
    public SseEmitter streamChat(
            @Parameter(description = "聊天请求") 
            @Valid @RequestBody ChatRequest request) {
        
        log.info("接收流式聊天请求，消息数量: {}", request.getMessages() != null ? request.getMessages().size() : 0);
        
        // 创建SSE连接，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300_000L);
        
        // 异步处理聊天请求
        CompletableFuture.runAsync(() -> {
            try {
                // 转换消息格式
                List<AiService.ChatMessage> chatMessages = request.getMessages().stream()
                        .map(msg -> new AiService.ChatMessage(msg.getRole(), msg.getContent()))
                        .collect(Collectors.toList());
                
                // 调用AI服务进行流式聊天
                aiService.streamChat(chatMessages, (chunk) -> {
                    try {
                        // 发送流式数据
                        Map<String, Object> data = Map.of(
                                "content", chunk,
                                "timestamp", System.currentTimeMillis()
                        );
                        String jsonData = objectMapper.writeValueAsString(data);
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(jsonData));
                    } catch (IOException e) {
                        log.error("发送流式数据失败", e);
                        emitter.completeWithError(e);
                    }
                });
                
                // 发送结束标记
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("[DONE]"));
                
                emitter.complete();
                
            } catch (Exception e) {
                log.error("流式聊天处理失败", e);
                try {
                    // 发送错误消息
                    Map<String, Object> errorData = Map.of(
                            "content", "抱歉，AI服务暂时出现问题，请稍后再试。",
                            "error", true,
                            "timestamp", System.currentTimeMillis()
                    );
                    String jsonData = objectMapper.writeValueAsString(errorData);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(jsonData));
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            }
        });
        
        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("流式聊天超时");
            emitter.complete();
        });
        
        emitter.onError((e) -> log.error("流式聊天出现错误", e));
        
        emitter.onCompletion(() -> log.info("流式聊天完成"));
        
        return emitter;
    }

    /**
     * 生成对话标题接口 (简化版，用于ChatInterface组件)
     */
    @PostMapping("/generate-title")
    @Operation(summary = "生成对话标题", description = "根据问题生成对话标题")
    public ApiResponse<Map<String, String>> generateTitle(
            @Parameter(description = "标题生成请求")
            @RequestBody Map<String, String> request) {
        
        String question = request.get("question");
        log.info("接收简化版标题生成请求: {}", question);
        
        try {
            String title = aiService.generateChatTitle(question);
            return ApiResponse.success("标题生成成功", Map.of("title", title));
        } catch (Exception e) {
            log.error("生成标题失败", e);
            return ApiResponse.error("生成标题失败");
        }
    }
}
