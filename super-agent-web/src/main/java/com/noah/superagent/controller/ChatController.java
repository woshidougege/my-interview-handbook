package com.noah.superagent.controller;

import com.noah.superagent.ai.service.AiService;
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
     * 流式聊天接口 - HTTP Streaming版本（推荐）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "流式聊天", description = "与AI进行流式对话，使用HTTP streaming实时返回内容")
    public void streamChatHttp(
            @Parameter(description = "聊天请求") 
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("🚀 HTTP Streaming - 接收流式聊天请求，消息数量: {}", 
                request.getMessages() != null ? request.getMessages().size() : 0);
        
        // 🔧 设置HTTP streaming响应头
        response.setContentType("text/plain; charset=utf-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("X-Accel-Buffering", "no"); // 禁用Nginx缓冲
        
        // 获取输出流
        ServletOutputStream outputStream = response.getOutputStream();
        
        try {
            // 打印完整的消息历史用于调试
            log.info("=== 后端收到的消息历史调试 ===");
            for (int i = 0; i < request.getMessages().size(); i++) {
                MessageDto msg = request.getMessages().get(i);
                log.info("消息 {}: [{}] {}", i+1, msg.getRole(), msg.getContent());
            }
            log.info("==========================");
            
            // 转换消息格式
            List<AiService.ChatMessage> chatMessages = request.getMessages().stream()
                    .map(msg -> new AiService.ChatMessage(msg.getRole(), msg.getContent()))
                    .collect(Collectors.toList());
            
            // 🔧 关键修复：改回同步处理，保持HTTP连接上下文
            // HTTP Streaming必须在同一个请求线程中处理，异步会导致连接断开
            aiService.streamChat(chatMessages, 
                // 🚀 流式数据回调 - 直接在HTTP线程中处理
                (chunk) -> {
                    try {
                        // 🔧 只检查outputStream是否可用
                        if (outputStream == null) {
                            log.warn("⚠️ OutputStream为null，停止发送数据");
                            return; // 优雅退出，不抛异常
                        }
                        
                        // 生成唯一时间戳
                        long currentTimeMillis = System.currentTimeMillis();
                        long nanoTime = System.nanoTime();
                        String uniqueTimestamp = currentTimeMillis + "." + String.format("%06d", nanoTime % 1_000_000);
                        
                        // 🔥 立即发送纯文本数据
                        String dataToSend = chunk + "\n";
                        outputStream.write(dataToSend.getBytes("UTF-8"));
                        outputStream.flush(); // 立即刷新
                        
                        log.info("✅ HTTP流式数据已发送: [{}] 长度={} 时间戳={}", 
                                chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk,
                                chunk.length(), uniqueTimestamp);
                        
                    } catch (IOException e) {
                        if (e.getMessage() != null && e.getMessage().contains("Stream is closed")) {
                            log.warn("🔌 客户端断开连接，停止发送数据: {}", e.getMessage());
                        } else {
                            log.error("❌ HTTP流式数据发送失败", e);
                        }
                        // 抛出RuntimeException停止流式处理
                        throw new RuntimeException("连接断开", e);
                    } catch (Exception e) {
                        log.error("❌ 流式数据处理异常", e);
                        throw new RuntimeException("流式处理失败", e);
                    }
                },
                // 🏁 完成回调
                () -> {
                    try {
                        // 🔧 发送结束标记
                        if (outputStream != null) {
                            outputStream.write("[DONE]\n".getBytes("UTF-8"));
                            outputStream.flush();
                            outputStream.close();
                            log.info("✅ HTTP流式聊天正常完成");
                        } else {
                            log.warn("⚠️ OutputStream为null，无法发送完成标记");
                        }
                    } catch (IOException e) {
                        if (e.getMessage() != null && e.getMessage().contains("Stream is closed")) {
                            log.info("🔌 连接在完成时已断开: {}", e.getMessage());
                        } else {
                            log.error("❌ 完成HTTP流式聊天时发生异常", e);
                        }
                    } catch (Exception e) {
                        log.error("❌ 完成流式聊天时发生异常", e);
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("HTTP流式聊天处理失败", e);
            try {
                if (outputStream != null) {
                    String errorMsg = "ERROR: 抱歉，AI服务暂时出现问题，请稍后再试。\n[DONE]\n";
                    outputStream.write(errorMsg.getBytes("UTF-8"));
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
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
