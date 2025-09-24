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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天控制器
 * 提供流式聊天功能
 *
 * @author 任相鹏
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
    @PostMapping(value = "/stream")
    @Operation(summary = "流式聊天", description = "与AI进行流式对话，使用HTTP streaming实时返回内容")
    public ResponseEntity<StreamingResponseBody> streamChatHttp(
            @Parameter(description = "聊天请求") 
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse servletResponse) {
        
        log.info("🚀 HTTP Streaming - 接收流式聊天请求，消息数量: {}", 
                request.getMessages() != null ? request.getMessages().size() : 0);
        
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
        
        // 🔧 创建StreamingResponseBody
        StreamingResponseBody responseBody = outputStream -> {
            try {
                // 🔧 立即发送一个字节来建立流式连接
                outputStream.write(" ".getBytes("UTF-8"));
                outputStream.flush();
                try { servletResponse.flushBuffer(); } catch (Exception ignore) {}
                log.info("✅ 流式连接已建立");
                
                // 使用闩锁在当前线程阻塞，直到流式完成，避免提前关闭连接
                CountDownLatch doneLatch = new CountDownLatch(1);

                // 🚀 调用AI服务进行流式聊天
                aiService.streamChat(chatMessages, 
                    // 流式数据回调
                    (chunk) -> {
                        try {
                            // 🔥 立即发送纯文本数据
                            String dataToSend = chunk + "\n";
                            outputStream.write(dataToSend.getBytes("UTF-8"));
                            outputStream.flush(); // 立即刷新
                            try { servletResponse.flushBuffer(); } catch (Exception ignore) {}
                            
                            log.info("✅ HTTP流式数据已发送: [{}] 长度={}", 
                                    chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk,
                                    chunk.length());
                            
                        } catch (IOException e) {
                            if (e.getMessage() != null && (e.getMessage().contains("Stream is closed") || 
                                e.getMessage().contains("Connection reset") ||
                                e.getMessage().contains("Broken pipe"))) {
                                log.info("🔌 客户端主动断开连接（可能是用户取消或发送新请求）: {}", e.getMessage());
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
                    // 完成回调
                    () -> {
                        try {
                            // 🔧 发送结束标记
                            outputStream.write("[DONE]\n".getBytes("UTF-8"));
                            outputStream.flush();
                            try { servletResponse.flushBuffer(); } catch (Exception ignore) {}
                            log.info("✅ HTTP流式聊天正常完成");
                        } catch (IOException e) {
                            if (e.getMessage() != null && e.getMessage().contains("Stream is closed")) {
                                log.info("🔌 连接在完成时已断开: {}", e.getMessage());
                            } else {
                                log.error("❌ 完成HTTP流式聊天时发生异常", e);
                            }
                        } catch (Exception e) {
                            log.error("❌ 完成流式聊天时发生异常", e);
                        } finally {
                            // 无论成功或失败都释放等待，避免阻塞
                            doneLatch.countDown();
                        }
                    }
                );
                // 阻塞当前响应线程，直到AI流结束或超时，避免连接被提前关闭
                try {
                    boolean finished = doneLatch.await(300, TimeUnit.SECONDS);
                    if (!finished) {
                        log.warn("⏰ 流式聊天等待超时，主动结束连接");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠️ 流式聊天线程被中断");
                }

            } catch (Exception e) {
                log.error("HTTP流式聊天处理失败", e);
                
                // 尝试发送错误信息
                try {
                    String errorMsg = "ERROR: " + e.getMessage() + "\n";
                    outputStream.write(errorMsg.getBytes("UTF-8"));
                    outputStream.flush();
                } catch (Exception writeError) {
                    log.warn("发送错误信息失败", writeError);
                }
                throw new RuntimeException("流式聊天失败", e);
            }
        };
        
        // 🔧 返回ResponseEntity with StreamingResponseBody
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("Cache-Control", "no-cache, no-transform")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no") // 禁用nginx缓冲
                .header("Access-Control-Allow-Origin", "*")
                .body(responseBody);
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
