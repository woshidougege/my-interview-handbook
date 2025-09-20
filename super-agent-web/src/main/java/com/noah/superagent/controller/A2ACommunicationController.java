package com.noah.superagent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.dto.request.A2AJsonRpcRequest;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.request.A2AMessageRequest;
import com.noah.superagent.common.dto.request.A2ASupplementInfoRequest;
import com.noah.superagent.common.util.SSEventFormatter;
import com.noah.superagent.service.A2ACommunicationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * A2A通信控制器
 * 用于处理与上游智平台的Agent-to-Agent通信
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/a2a")
@RequiredArgsConstructor
@Tag(name = "A2A通信接口", description = "与上游智平台进行Agent-to-Agent通信的接口")
public class A2ACommunicationController {
    
    private final A2ACommunicationService a2aCommunicationService;
    
    /**
     * 发送消息到A2A平台
     */
    @PostMapping("/send-message")
    @Operation(summary = "发送消息到A2A平台", description = "将用户消息发送到上游智平台的协同规划智能体")
    public ApiResponse<String> sendMessageToA2APlatform(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request) {
        log.info("接收发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());
        
        return a2aCommunicationService.sendMessageToA2APlatform(
                request.getUserId(), request.getMessage(), request.getSessionId());
    }
    
    /**
     * 异步发送消息到A2A平台
     */
    @PostMapping("/send-message-async")
    @Operation(summary = "异步发送消息到A2A平台", description = "异步将用户消息发送到上游智平台的协同规划智能体")
    public CompletableFuture<ApiResponse<String>> sendMessageToA2APlatformAsync(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request) {
        log.info("接收异步发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());
        
        return a2aCommunicationService.sendMessageToA2APlatformAsync(
                request.getUserId(), request.getMessage(), request.getSessionId());
    }

    /**
     * 流式发送消息到A2A平台（JSON-RPC格式）
     */
    @PostMapping("/stream-message")
    @Operation(summary = "流式发送消息到A2A平台", description = "将用户消息流式发送到上游智平台的协同规划智能体")
    public StreamingResponseBody streamMessageToA2APlatform(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request) {
        log.info("接收流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());

        // 所有参数都由后端生成
        String abilityCode = a2aCommunicationService.getDefaultAbilityCode();
        String entityCode = a2aCommunicationService.getDefaultEntityCode();
        String userId = request.getUserId();
        String message = request.getMessage();
        // 由后端生成默认值
        String taskId = "task_" + System.currentTimeMillis();
        String contextId = "";

        return outputStream -> {
            long startTime = System.currentTimeMillis();
            log.info("开始处理JSON-RPC格式流式响应 - 用户ID: {}, 消息: {}", userId, message);

            try (InputStream inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                    abilityCode, entityCode, userId, message, taskId, contextId)) {

                if (inputStream != null) {
                    log.info("成功获取A2A平台输入流，开始流式传输数据");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytes = 0;
                    int chunkCount = 0;

                    // 简化流传输逻辑，直接转发数据
                    while (!Thread.currentThread().isInterrupted() && (bytesRead = inputStream.read(buffer)) != -1) {
                        try {
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                            totalBytes += bytesRead;
                            chunkCount++;

                            // 每10个数据块记录一次详细日志
                            if (chunkCount % 10 == 0) {
                                long currentTime = System.currentTimeMillis();
                                log.debug("已传输数据块数量: {}, 总字节数: {}, 已用时间: {}ms",
                                        chunkCount, totalBytes, (currentTime - startTime));
                            }

                            // 添加短暂延迟以确保数据能被及时发送
                            Thread.sleep(1);
                        } catch (IOException e) {
                            // 客户端可能已经断开连接
                            log.warn("客户端连接已断开或发生IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}", 
                                userId, chunkCount, totalBytes, e.getMessage());
                            break;
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    log.info("JSON-RPC格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                            chunkCount, totalBytes, (endTime - startTime));
                } else {
                    long errorTime = System.currentTimeMillis();
                    log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                    writeErrorToStream(outputStream, "错误：无法从A2A平台获取响应流");
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                log.error("JSON-RPC格式流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                writeErrorToStream(outputStream, "错误：" + e.getMessage());
            }
        };
    }
    
    /**
     * 处理补充信息
     */
    @PostMapping("/supplement-info")
    @Operation(summary = "处理补充信息", description = "处理用户补充的信息并发送到A2A代理")
    public ApiResponse<String> handleSupplementInfo(
            @Parameter(description = "A2A补充信息请求参数") @Valid @RequestBody A2ASupplementInfoRequest request) {
        log.info("接收处理补充信息请求 - 用户ID: {}, 任务ID: {}", request.getUserId(), request.getTaskId());
        
        return a2aCommunicationService.handleSupplementInfo(
                request.getUserId(), request.getTaskId(), request.getUserInput(), request.getSessionId());
    }
    
    /**
     * 异步处理补充信息
     */
    @PostMapping("/supplement-info-async")
    @Operation(summary = "异步处理补充信息", description = "异步处理用户补充的信息并发送到A2A代理")
    public CompletableFuture<ApiResponse<String>> handleSupplementInfoAsync(
            @Parameter(description = "A2A补充信息请求参数") @Valid @RequestBody A2ASupplementInfoRequest request) {
        log.info("接收异步处理补充信息请求 - 用户ID: {}, 任务ID: {}", request.getUserId(), request.getTaskId());
        
        return a2aCommunicationService.handleSupplementInfoAsync(
                request.getUserId(), request.getTaskId(), request.getUserInput(), request.getSessionId());
    }
    
    /**
     * 流式发送消息到A2A平台（SSE格式）
     */
    @PostMapping("/stream-message-sse")
    @Operation(summary = "流式发送消息到A2A平台（SSE格式）", description = "将用户消息流式发送到上游智平台的协同规划智能体，使用SSE格式返回")
    public ResponseEntity<StreamingResponseBody> streamMessageToA2APlatformWithSSE(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request) {
        log.info("接收SSE格式流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());
        
        StreamingResponseBody responseBody = outputStream -> {
            long startTime = System.currentTimeMillis();
            log.info("开始处理SSE格式流式响应 - 用户ID: {}, 消息: {}, 开始时间: {}", 
                request.getUserId(), request.getMessage(), startTime);
            
            try (InputStream inputStream = a2aCommunicationService.streamMessageToA2APlatform(
                    request.getUserId(), request.getMessage(), request.getSessionId())) {
                
                if (inputStream != null) {
                    log.info("成功获取A2A平台输入流，开始读取数据");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytes = 0;
                    int chunkCount = 0;
                    
                    // 用于检测重复内容的缓冲区
                    Set<String> processedMessages = new HashSet<>();
                    StringBuilder sseBuffer = new StringBuilder(); // 用于组装完整的SSE事件

                    // 循环读取数据直到流结束
                    while (!Thread.currentThread().isInterrupted() && (bytesRead = inputStream.read(buffer)) != -1) {
                        try {
                            String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                            sseBuffer.append(chunk);
                            
                            // 处理SSE事件
                            String bufferContent = sseBuffer.toString();
                            String[] events = bufferContent.split("\n\n"); // SSE事件以\n\n分隔
                            
                            // 检查是否是完整的事件（以\n\n结尾）
                            boolean hasIncompleteEvent = !bufferContent.endsWith("\n\n");
                            int eventCount = hasIncompleteEvent ? events.length - 1 : events.length;
                            
                            // 处理完整的事件
                            for (int i = 0; i < eventCount; i++) {
                                String event = events[i];
                                if (event.trim().isEmpty()) continue;
                                
                                log.debug("处理SSE事件: {}", event);
                                
                                // 提取data部分
                                String[] lines = event.split("\n");
                                for (String line : lines) {
                                    if (line.startsWith("data:")) {
                                        String jsonData = line.substring(5).trim();
                                        if (!jsonData.isEmpty() && !jsonData.equals("ping")) {
                                            // 尝试解析JSON数据以检查messageId
                                            try {
                                                JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
                                                JsonNode messageIdNode = jsonNode.path("result").path("status").path("message").path("messageId");
                                                
                                                if (!messageIdNode.isMissingNode()) {
                                                    String messageId = messageIdNode.asText();
                                                    log.debug("收到消息ID: {}", messageId);
                                                    
                                                    // 如果消息已处理过，则跳过
                                                    if (processedMessages.contains(messageId)) {
                                                        log.debug("检测到重复消息，ID: {}，已跳过", messageId);
                                                        continue;
                                                    }
                                                    
                                                    // 标记消息为已处理
                                                    processedMessages.add(messageId);
                                                    log.debug("处理新消息，ID: {}", messageId);
                                                }
                                                
                                                // 将数据写入输出流
                                                outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                                                outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                totalBytes += line.getBytes(StandardCharsets.UTF_8).length + 2;
                                                chunkCount++;
                                            } catch (Exception e) {
                                                log.warn("JSON解析失败: {}", e.getMessage());
                                                // 即使解析失败也传输数据
                                                outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                                                outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                totalBytes += line.getBytes(StandardCharsets.UTF_8).length + 2;
                                                chunkCount++;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 保留不完整的事件在缓冲区中
                            if (hasIncompleteEvent && events.length > 0) {
                                sseBuffer = new StringBuilder(events[events.length - 1]);
                            } else {
                                sseBuffer = new StringBuilder();
                            }

                            if (chunkCount % 10 == 0) {
                                long currentTime = System.currentTimeMillis();
                                log.debug("已传输数据块数量: {}, 总字节数: {}, 已用时间: {}ms",
                                        chunkCount, totalBytes, (currentTime - startTime));
                            }

                            Thread.sleep(1);
                        } catch (IOException e) {
                            // 客户端可能已经断开连接
                            log.warn("客户端连接已断开，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}", 
                                request.getUserId(), chunkCount, totalBytes);
                            break;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("数据传输线程被中断");
                            break;
                        }
                    }
                    
                    // 处理剩余的不完整事件
                    if (sseBuffer.length() > 0) {
                        log.debug("处理剩余的SSE事件: {}", sseBuffer.toString());
                        try {
                            String[] lines = sseBuffer.toString().split("\n");
                            for (String line : lines) {
                                if (line.startsWith("data:")) {
                                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                                    outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    totalBytes += line.getBytes(StandardCharsets.UTF_8).length + 2;
                                    chunkCount++;
                                }
                            }
                        } catch (IOException e) {
                            log.warn("写入剩余事件时发生IO异常", e);
                        }
                    }
                    
                    long endTime = System.currentTimeMillis();
                    log.info("SSE格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms", 
                            chunkCount, totalBytes, (endTime - startTime));
                    log.info("总共处理了 {} 条唯一消息", processedMessages.size());
                } else {
                    long errorTime = System.currentTimeMillis();
                    log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                    // 如果输入流为空，返回错误信息
                    sendSSEEvent(outputStream, SSEventFormatter.formatEvent("error", "错误：无法从A2A平台获取响应流"));
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                log.error("SSE格式流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                try {
                    sendSSEEvent(outputStream, SSEventFormatter.formatEvent("error", "错误：" + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("写入错误信息时发生异常", ioException);
                }
            } finally {
                try {
                    // 发送结束标记
                    sendSSEEvent(outputStream, SSEventFormatter.formatEvent("end", "end"));
                } catch (IOException e) {
                    log.warn("发送结束标记失败", e);
                }
            }
        };
        
        log.info("准备返回SSE格式流式响应给客户端");
        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream;charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("Access-Control-Allow-Origin", "*")
                .body(responseBody);
    }
    
    /**
     * 标准JSON-RPC格式接口 - 根据用户提供的URL格式
     * URL: /kunlun/a2a/api/{abilityCode}/entity/{entityCode}/userid/{userId}
     */
    @PostMapping(value = {"/kunlun/a2a/api/{abilityCode}/entity/{entityCode}/userid/{userId}", 
                 "/api/v1/a2a/kunlun/a2a/api/{abilityCode}/entity/{entityCode}/userid/{userId}",
                 "/a2a/kunlun/a2a/api/{abilityCode}/entity/{entityCode}/userid/{userId}"}, 
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "标准JSON-RPC格式接口", description = "按照标准JSON-RPC 2.0格式与A2A平台通信")
    public ResponseEntity<StreamingResponseBody> jsonRpcStreamMessage(
            @Parameter(description = "能力中心编码") @PathVariable String abilityCode,
            @Parameter(description = "实体编码") @PathVariable String entityCode,
            @Parameter(description = "用户ID") @PathVariable String userId,
            @Parameter(description = "JSON-RPC请求参数") @Valid @RequestBody A2AJsonRpcRequest request) {
        
        log.info("接收标准JSON-RPC格式请求 - 能力中心编码: {}, 实体编码: {}, 用户ID: {}, 请求ID: {}", 
                abilityCode, entityCode, userId, request.getId());
        
        // 提取消息内容
        String message = "";
        String taskId = "";
        String contextId = "";
        
        if (request.getParams() != null && request.getParams().getMessage() != null) {
            A2AJsonRpcRequest.A2ARequestParams.A2AMessage msg = request.getParams().getMessage();
            taskId = msg.getTaskId() != null ? msg.getTaskId() : "";
            contextId = msg.getContextId() != null ? msg.getContextId() : "";
            
            // 提取消息文本内容
            if (msg.getParts() != null && msg.getParts().length > 0) {
                for (A2AJsonRpcRequest.A2ARequestParams.A2AMessagePart part : msg.getParts()) {
                    if ("text".equals(part.getKind()) && part.getText() != null) {
                        message = part.getText();
                        break;
                    }
                }
            }
        }
        
        log.info("提取的消息内容 - 消息: {}, 任务ID: {}, 上下文ID: {}", message, taskId, contextId);
        
        final String finalMessage = message;
        final String finalTaskId = taskId;
        final String finalContextId = contextId;
        
        StreamingResponseBody responseBody = outputStream -> {
            long startTime = System.currentTimeMillis();
            log.info("开始处理JSON-RPC格式流式响应 - 用户ID: {}, 消息: {}", userId, finalMessage);
            
            try (InputStream inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                    abilityCode, entityCode, userId, finalMessage, finalTaskId, finalContextId)) {
                
                if (inputStream != null) {
                    log.info("成功获取A2A平台输入流，开始流式传输数据");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytes = 0;
                    int chunkCount = 0;
                    
                    // 用于收集所有转发到前端的数据
                    ByteArrayOutputStream allTransmittedData = new ByteArrayOutputStream();
                    
                    // 简化流传输逻辑，直接转发数据
                    while (!Thread.currentThread().isInterrupted() && (bytesRead = inputStream.read(buffer)) != -1) {
                        try {
                            // 将数据添加到总传输数据收集中
                            allTransmittedData.write(buffer, 0, bytesRead);
                            
                            // 记录接收到的数据块
                            String chunkData = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                            log.debug("接收到数据块 - 块序号: {}, 大小: {} 字节, 数据预览: {}", 
                                chunkCount, bytesRead, chunkData.substring(0, Math.min(chunkData.length(), 100)).replace("\n", "\\n").replace("\r", "\\r"));
                            
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                            totalBytes += bytesRead;
                            chunkCount++;

                            // 每10个数据块记录一次详细日志
                            if (chunkCount % 10 == 0) {
                                long currentTime = System.currentTimeMillis();
                                log.info("已传输数据块数量: {}, 总字节数: {}, 已用时间: {}ms",
                                        chunkCount, totalBytes, (currentTime - startTime));
                            }

                            // 添加短暂延迟以确保数据能被及时发送
                            Thread.sleep(1);
                        } catch (IOException e) {
                            // 客户端可能已经断开连接
                            log.warn("客户端连接已断开或发生IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}", 
                                userId, chunkCount, totalBytes, e.getMessage());
                            break;
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    log.info("JSON-RPC格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                            chunkCount, totalBytes, (endTime - startTime));
                    
                    // 在结束时输出完整的传输数据到日志
                    String completeTransmittedData = allTransmittedData.toString(StandardCharsets.UTF_8.name());
                    log.info("转发到前端的完整数据长度: {} 字符", completeTransmittedData.length());
                    if (completeTransmittedData.length() > 0) {
                        // 只记录前2000个字符以避免日志过大
                        String transmittedDataPreview = completeTransmittedData.substring(0, Math.min(completeTransmittedData.length(), 2000));
                        log.info("转发到前端的完整数据预览: \n{}", transmittedDataPreview);
                        
                        // 检查是否有明显的重复模式
                        if (completeTransmittedData.length() > 100) {
                            String firstPart = completeTransmittedData.substring(0, Math.min(100, completeTransmittedData.length()));
                            int secondOccurrence = completeTransmittedData.indexOf(firstPart, 100);
                            if (secondOccurrence > 0) {
                                log.warn("检测到转发数据中的重复模式: 相同内容在位置 {} 出现第二次", secondOccurrence);
                                // 记录重复部分的预览
                                int repeatEnd = Math.min(secondOccurrence + 100, completeTransmittedData.length());
                                String repeatedPart = completeTransmittedData.substring(secondOccurrence, repeatEnd);
                                log.warn("转发数据中的重复内容预览: {}", repeatedPart.replace("\n", "\\n").replace("\r", "\\r"));
                            }
                        }
                    }
                } else {
                    long errorTime = System.currentTimeMillis();
                    log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                    writeErrorToStream(outputStream, "错误：无法从A2A平台获取响应流");
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                log.error("JSON-RPC格式流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                writeErrorToStream(outputStream, "错误：" + e.getMessage());
            }
        };
        
        log.info("准备返回JSON-RPC格式流式响应给客户端");
        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream;charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("Access-Control-Allow-Origin", "*")
                .body(responseBody);
    }
    
    /**
     * 向输出流发送SSE事件
     * 
     * @param outputStream 输出流
     * @param event SSE事件内容
     * @throws IOException IO异常
     */
    private void sendSSEEvent(OutputStream outputStream, String event) throws IOException {
        outputStream.write(event.getBytes());
        outputStream.flush();
    }
    
    /**
     * 向输出流写入错误信息
     * 
     * @param outputStream 输出流
     * @param errorMessage 错误信息
     */
    private void writeErrorToStream(OutputStream outputStream, String errorMessage) {
        try {
            if (outputStream != null) {
                byte[] errorBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
                outputStream.write(errorBytes);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.debug("向客户端写入错误信息失败，可能客户端已断开连接", e);
        }
    }
}