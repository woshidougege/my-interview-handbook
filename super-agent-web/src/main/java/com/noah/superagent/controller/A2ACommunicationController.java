package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.A2AMessageRequest;
import com.noah.superagent.common.dto.request.A2ASupplementInfoRequest;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.util.SSEventFormatter;
import com.noah.superagent.service.A2ACommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * A2A通信控制器
 * 用于处理与上游智平台的Agent-to-Agent通信
 * 
 * 主要功能包括：
 * 1. 发送消息到A2A平台（同步/异步/流式）
 * 2. 处理用户补充信息（同步/异步）
 * 3. 标准JSON-RPC格式通信
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/a2a")
@Tag(name = "A2A通信接口", description = "与上游智平台进行Agent-to-Agent通信的接口")
public class A2ACommunicationController {
    
    private final A2ACommunicationService a2aCommunicationService;
    
    private final Executor aiChatExecutionExecutor;
    
    public A2ACommunicationController(A2ACommunicationService a2aCommunicationService, 
                                     @Qualifier("ai-chat-execution-executor") Executor aiChatExecutionExecutor) {
        this.a2aCommunicationService = a2aCommunicationService;
        this.aiChatExecutionExecutor = aiChatExecutionExecutor;
    }
    
    /**
     * 发送消息到A2A平台（同步）
     * 
     * @param request A2A消息请求参数
     * @return ApiResponse<String> 响应结果
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
     * 
     * @param request A2A消息请求参数
     * @return CompletableFuture<ApiResponse<String>> 异步响应结果
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
     * 
     * @param request A2A消息请求参数
     * @return StreamingResponseBody 流式响应体
     */
    @PostMapping("/stream-message")
    @Operation(summary = "流式发送消息到A2A平台", description = "将用户消息流式发送到上游智平台的协同规划智能体")
    @Parameters({
        @Parameter(name = "contextId", description = "上下文ID", in = ParameterIn.QUERY),
        @Parameter(name = "message", description = "用户消息", in = ParameterIn.QUERY)
    })
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
        // 修改contextId从请求中获取，如果请求中没有则使用默认值
        String contextId = request.getContextId() != null ? request.getContextId() : "";

        return createStreamingResponse(abilityCode, entityCode, userId, message, taskId, contextId, "JSON-RPC");
    }
    
    /**
     * 处理补充信息（同步）
     * 
     * @param request A2A补充信息请求参数
     * @return ApiResponse<String> 响应结果
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
     * 
     * @param request A2A补充信息请求参数
     * @return CompletableFuture<ApiResponse<String>> 异步响应结果
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
     * 
     * @param request A2A消息请求参数
     * @return ResponseEntity<StreamingResponseBody> SSE格式响应
     */
    @PostMapping("/stream-message-sse")
    @Operation(summary = "流式发送消息到A2A平台（SSE格式）", description = "将用户消息流式发送到上游智平台的协同规划智能体，使用SSE格式返回")
    public ResponseEntity<StreamingResponseBody> streamMessageToA2APlatformWithSSE(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request) {
        log.info("接收SSE格式流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());
        
        // 所有参数都由后端生成
        String abilityCode = a2aCommunicationService.getDefaultAbilityCode();
        String entityCode = a2aCommunicationService.getDefaultEntityCode();
        String userId = request.getUserId();
        String message = request.getMessage();
        // 由后端生成默认值
        String taskId = "task_" + System.currentTimeMillis();
        // 修改contextId从请求中获取，如果请求中没有则使用默认值
        String contextId = request.getContextId() != null ? request.getContextId() : "";
        
        StreamingResponseBody responseBody = outputStream -> {
            long startTime = System.currentTimeMillis();
            log.info("开始处理SSE格式流式响应 - 用户ID: {}, 消息: {}, 开始时间: {}", 
                userId, message, startTime);
            
            InputStream inputStream = null;
            try {
                // 使用与streamMessageToA2APlatform相同的方式调用服务
                inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                        abilityCode, entityCode, userId, message, taskId, contextId);
                
                if (inputStream != null) {
                    log.info("成功获取A2A平台输入流，开始读取数据");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytes = 0;
                    int chunkCount = 0;
                    boolean hasReadData = false; // 标记是否读取到数据
                    
                    // 循环读取数据直到流结束
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            // 检查输入流是否已关闭
                            bytesRead = inputStream.read(buffer);
                            if (bytesRead == -1) {
                                // 流结束
                                log.debug("A2A平台输入流已到达末尾");
                                break;
                            }
                            
                            // 标记已读取到数据
                            hasReadData = true;
                            
                            // 直接将数据写入输出流，不做任何处理或去重
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                            totalBytes += bytesRead;
                            chunkCount++;

                            if (chunkCount % 10 == 0 && chunkCount > 0) {
                                long currentTime = System.currentTimeMillis();
                                log.debug("已传输数据块数量: {}, 总字节数: {}, 已用时间: {}ms",
                                        chunkCount, totalBytes, (currentTime - startTime));
                            }

                            Thread.sleep(1);
                        } catch (IOException e) {
                            // 检查异常是否是由于输入流关闭导致的
                            if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                                log.info("A2A平台输入流已关闭，结束数据读取 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}", 
                                    userId, chunkCount, totalBytes);
                                break;
                            }
                            
                            // 客户端可能已经断开连接
                            log.warn("IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}", 
                                userId, chunkCount, totalBytes, e.getMessage());
                            break;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("数据传输线程被中断");
                            break;
                        }
                    }
                    
                    // 如果没有读取到任何数据，记录警告信息并向客户端发送错误信息
                    if (!hasReadData) {
                        log.warn("从未A2A平台输入流中读取到任何数据，可能上游服务未返回有效数据");
                        writeErrorToStream(outputStream, "未从A2A平台获取到有效响应数据");
                    }
                    
                    long endTime = System.currentTimeMillis();
                    log.info("SSE格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms", 
                            chunkCount, totalBytes, (endTime - startTime));
                } else {
                    long errorTime = System.currentTimeMillis();
                    log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                    // 如果输入流为空，返回错误信息
                    writeErrorToStream(outputStream, "错误：无法从A2A平台获取响应流");
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                log.error("SSE格式流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                writeErrorToStream(outputStream, "错误：" + e.getMessage());
            } finally {
                // 关闭输入流
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        log.debug("已关闭A2A平台输入流");
                    } catch (IOException e) {
                        log.warn("关闭A2A平台输入流时发生异常: {}", e.getMessage());
                    }
                }
                
                try {
                    // 发送结束标记，但需要确保客户端仍在连接
                    sendSSEEvent(outputStream, SSEventFormatter.formatEvent("end", "end"));
                    // 确保数据被刷新到客户端
                    outputStream.flush();
                    log.debug("已发送结束标记并刷新输出流");
                } catch (IOException e) {
                    log.debug("发送结束标记失败，客户端可能已断开连接: {}", e.getMessage());
                } finally {
                    try {
                        // 最后尝试关闭输出流
                        outputStream.close();
                        log.debug("已关闭输出流");
                    } catch (IOException e) {
                        log.debug("关闭输出流时发生异常（客户端可能已断开连接）: {}", e.getMessage());
                    }
                }
            }
        };
        
        log.info("准备返回SSE格式流式响应给客户端");
        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream;charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no") // 禁用nginx缓冲
                .header("Access-Control-Allow-Origin", "*")
                .body(responseBody);
    }
    
    /**
     * 异步流式发送消息到A2A平台
     * 
     * @param request A2A消息请求参数
     * @return CompletableFuture<ResponseEntity<StreamingResponseBody>> 异步流式响应体
     */
    @PostMapping("/stream-message-async")
    @Operation(summary = "异步流式发送消息到A2A平台", description = "异步将用户消息流式发送到上游智平台的协同规划智能体")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> streamMessageToA2APlatformAsync(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request) {
        log.info("接收异步流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());
        
        // 使用AI对话专用线程池执行异步流式任务
        return CompletableFuture.supplyAsync(() -> {
            // 所有参数都由后端生成
            String abilityCode = a2aCommunicationService.getDefaultAbilityCode();
            String entityCode = a2aCommunicationService.getDefaultEntityCode();
            String userId = request.getUserId();
            String message = request.getMessage();
            // 由后端生成默认值
            String taskId = "task_" + System.currentTimeMillis();
            // 修改contextId从请求中获取，如果请求中没有则使用默认值
            String contextId = request.getContextId() != null ? request.getContextId() : "";

            StreamingResponseBody responseBody = outputStream -> {
                long startTime = System.currentTimeMillis();
                log.info("开始处理异步流式响应 - 用户ID: {}, 消息: {}", userId, message);

                try (InputStream inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                        abilityCode, entityCode, userId, message, taskId, contextId)) {

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
                        log.info("异步流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
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
                    log.error("异步流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                    writeErrorToStream(outputStream, "错误：" + e.getMessage());
                }
            };

            return ResponseEntity.ok()
                    .header("Content-Type", "text/event-stream;charset=UTF-8")
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .header("X-Accel-Buffering", "no") // 禁用nginx缓冲
                    .header("Access-Control-Allow-Origin", "*")
                    .body(responseBody);
        }, aiChatExecutionExecutor);
    }

    
    /**
     * 创建统一的流式响应处理逻辑
     * 
     * @param abilityCode 能力中心编码
     * @param entityCode 实体编码
     * @param userId 用户ID
     * @param message 消息内容
     * @param taskId 任务ID
     * @param contextId 上下文ID
     * @param responseType 响应类型（用于日志）
     * @return StreamingResponseBody 流式响应体
     */
    private StreamingResponseBody createStreamingResponse(String abilityCode, String entityCode, String userId, 
            String message, String taskId, String contextId, String responseType) {
        return outputStream -> {
            long startTime = System.currentTimeMillis();
            log.info("开始处理{}格式流式响应 - 用户ID: {}, 消息: {}", responseType, userId, message);

            try (InputStream inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                    abilityCode, entityCode, userId, message, taskId, contextId)) {

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
                    log.info("{}格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                            responseType, chunkCount, totalBytes, (endTime - startTime));

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
                log.error("{}格式流式传输过程中发生异常 - 耗时: {}ms", responseType, (errorTime - startTime), e);
                writeErrorToStream(outputStream, "错误：" + e.getMessage());
            }
        };
    }
    
    /**
     * 向输出流发送SSE事件
     * 
     * @param outputStream 输出流
     * @param event SSE事件内容
     * @throws IOException IO异常
     */
    private void sendSSEEvent(OutputStream outputStream, String event) throws IOException {
        if (outputStream != null) {
            try {
                outputStream.write(event.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                log.debug("向客户端写入SSE事件失败，可能客户端已断开连接: {}", e.getMessage());
                throw e; // 重新抛出异常，让调用者知道连接已断开
            }
        } else {
            log.warn("输出流为空，无法发送SSE事件");
            throw new IOException("输出流为空");
        }
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