package com.noah.superagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.annotation.SaToken;
import com.noah.superagent.common.config.KunlunProperties;
import com.noah.superagent.common.dto.request.A2AMessageRequest;
import com.noah.superagent.common.dto.request.Attachment;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.response.FileUploadResponse;
import com.noah.superagent.common.util.SSEventFormatter;
import com.noah.superagent.common.util.SecurityUtils;
import com.noah.superagent.service.A2ACommunicationService;
import com.noah.superagent.service.FileRepositoryService;
import com.noah.superagent.util.UserEntityCodeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A2A通信控制器
 * 用于处理与上游智平台的Agent-to-Agent通信
 * <p>
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

    private final KunlunProperties kunlunProperties;
    
    private final RestTemplate restTemplate;

    private final FileRepositoryService fileRepositoryService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public A2ACommunicationController(A2ACommunicationService a2aCommunicationService,
                                      @Qualifier("ai-chat-execution-executor") Executor aiChatExecutionExecutor, 
                                      KunlunProperties kunlunProperties,
                                      FileRepositoryService fileRepositoryService) {
        this.a2aCommunicationService = a2aCommunicationService;
        this.aiChatExecutionExecutor = aiChatExecutionExecutor;
        this.kunlunProperties = kunlunProperties;
        this.fileRepositoryService = fileRepositoryService;
        this.restTemplate = new RestTemplate();
    }


    /**
     * 流式发送消息到A2A平台
     *
     * @param request A2A消息请求参数
     * @return StreamingResponseBody 流式响应体
     */
    @PostMapping(value = "/stream-message", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "流式发送消息到A2A平台", description = "将用户消息和附件流式发送到上游智平台的协同规划智能体")
    @Parameters({
            @Parameter(name = "Satoken", description = "认证令牌", in = ParameterIn.HEADER)
    })
    public StreamingResponseBody streamMessageToA2APlatform(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request,
            @Parameter(hidden = true) @SaToken String satoken) {
        
        log.info("接收流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", 
                request.getUserId(), request.getMessage());

        // 所有参数都由后端生成
        String abilityCode = kunlunProperties.getAbilityCodes().getDefaultCode();
        String entityCode = kunlunProperties.getEntityCodes().getDefaultCode();
        String userId = request.getUserId();
        String message = request.getMessage();
        // 由后端生成默认值
        String taskId = "task_" + System.currentTimeMillis();
        // 修改contextId从请求中获取，如果请求中没有则使用默认值
        String contextId = request.getContextId() != null ? request.getContextId() : "";

        return createStreamingResponse(abilityCode, entityCode, userId, message, taskId, contextId, "JSON-RPC", satoken, request.getAttachments());
    }


    /**
     * 流式发送消息到A2A平台（SSE格式）
     *
     * @param request A2A消息请求参数
     * @return ResponseEntity<StreamingResponseBody> SSE格式响应
     */
    @PostMapping("/stream-message-sse")
    @Operation(summary = "流式发送消息到A2A平台（SSE格式）", description = "将用户消息流式发送到上游智平台的协同规划智能体，使用SSE格式返回")
    @Parameters({
            @Parameter(name = "Satoken", description = "认证令牌", in = ParameterIn.HEADER)
    })
    public ResponseEntity<StreamingResponseBody> streamMessageToA2APlatformWithSSE(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request,
            @Parameter(hidden = true) @SaToken String satoken) {
        log.info("接收SSE格式流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());

        // 所有参数都由后端生成
        String abilityCode = kunlunProperties.getAbilityCodes().getDefaultCode();
        String entityCode = kunlunProperties.getEntityCodes().getDefaultCode();
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
                        abilityCode, entityCode, userId, message, taskId, contextId, satoken, request.getAttachments());

                if (inputStream != null) {
                    log.info("成功获取A2A平台输入流，开始流式传输数据");

                    // 直接流式传输数据，避免预读取所有数据
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytes = 0;
                    int chunkCount = 0;
                    boolean hasReceivedData = false;

                    try {
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            // 标记已收到数据
                            hasReceivedData = true;

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

                                // 让出CPU时间片，避免过度占用CPU
                                Thread.yield();
                            } catch (IOException e) {
                                // 客户端可能已经断开连接
                                if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                                    log.info("客户端连接已断开，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}",
                                            userId, chunkCount, totalBytes);
                                } else {
                                    log.warn("客户端连接已断开或发生IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}",
                                            userId, chunkCount, totalBytes, e.getMessage());
                                }
                                // 退出循环，不再尝试写入数据
                                break;
                            }
                        }

                        // 如果没有读取到任何数据，记录警告信息并向客户端发送错误信息
                        if (!hasReceivedData) {
                            log.warn("从未A2A平台输入流中读取到任何数据，可能上游服务未返回有效数据");
                            try {
                                writeErrorToStream(outputStream, SecurityUtils.getSafeErrorMessage("STREAM_READ_ERROR"));
                            } catch (Exception e) {
                                log.warn("向已断开的客户端写入错误信息失败: {}", e.getMessage());
                            }
                        }

                        long endTime = System.currentTimeMillis();
                        log.info("SSE格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                                chunkCount, totalBytes, (endTime - startTime));
                    } catch (IOException e) {
                        long errorTime = System.currentTimeMillis();
                        if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                            log.info("读取A2A平台响应数据时检测到流已关闭 - 耗时: {}ms，用户ID: {}",
                                    (errorTime - startTime), userId);
                        } else {
                            log.error("读取A2A平台响应数据时发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                        }

                        try {
                            writeErrorToStream(outputStream, SecurityUtils.sanitizeErrorMessage(e));
                        } catch (Exception writeException) {
                            log.warn("向已断开的客户端写入错误信息失败: {}", writeException.getMessage());
                        }
                    }
                } else {
                    long errorTime = System.currentTimeMillis();
                    log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                    // 如果输入流为空，返回错误信息
                    try {
                        writeErrorToStream(outputStream, SecurityUtils.getSafeErrorMessage("CONNECTION_ERROR"));
                    } catch (Exception e) {
                        log.warn("向已断开的客户端写入错误信息失败: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                log.error("SSE格式流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                try {
                    writeErrorToStream(outputStream, SecurityUtils.sanitizeErrorMessage(e));
                } catch (Exception writeException) {
                    log.warn("向已断开的客户端写入错误信息失败: {}", writeException.getMessage());
                }
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
                }
                // 注意：outputStream 由 Spring Boot 框架自动管理，无需手动关闭
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
     * @return CompletableFuture<ResponseEntity < StreamingResponseBody>> 异步流式响应体
     */
    @PostMapping("/stream-message-async")
    @Operation(summary = "异步流式发送消息到A2A平台", description = "异步将用户消息流式发送到上游智平台的协同规划智能体")
    @Parameters({
            @Parameter(name = "Satoken", description = "认证令牌", in = ParameterIn.HEADER, required = false)
    })
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> streamMessageToA2APlatformAsync(
            @Parameter(description = "A2A消息请求参数") @Valid @RequestBody A2AMessageRequest request,
            @Parameter(hidden = true) @SaToken String satoken) {
        log.info("接收异步流式发送消息到A2A平台请求 - 用户ID: {}, 消息: {}", request.getUserId(), request.getMessage());

        // 使用AI对话专用线程池执行异步流式任务
        return CompletableFuture.supplyAsync(() -> {
            // 所有参数都由后端生成
            String abilityCode = kunlunProperties.getAbilityCodes().getDefaultCode();
            String entityCode = kunlunProperties.getEntityCodes().getDefaultCode();
            String userId = request.getUserId();
            String message = request.getMessage();
            // 由后端生成默认值
            String taskId = "task_" + System.currentTimeMillis();
            // 修改contextId从请求中获取，如果请求中没有则使用默认值
            String contextId = request.getContextId() != null ? request.getContextId() : "";

            StreamingResponseBody responseBody = outputStream -> {
                long startTime = System.currentTimeMillis();
                log.info("开始处理异步流式响应 - 用户ID: {}, 消息: {}", userId, message);

                InputStream inputStream = null;
                try {
                    inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                            abilityCode, entityCode, userId, message, taskId, contextId, satoken, request.getAttachments());

                    if (inputStream != null) {
                        log.info("成功获取A2A平台输入流，开始流式传输数据");

                        // 直接流式传输数据，避免预读取所有数据
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        int totalBytes = 0;
                        int chunkCount = 0;
                        boolean hasReceivedData = false;

                        try {
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                // 标记已收到数据
                                hasReceivedData = true;

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

                                    // 让出CPU时间片，避免过度占用CPU
                                    Thread.yield();
                                } catch (IOException e) {
                                    // 客户端可能已经断开连接
                                    if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                                        log.info("客户端连接已断开，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}",
                                                userId, chunkCount, totalBytes);
                                    } else {
                                        log.warn("客户端连接已断开或发生IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}",
                                                userId, chunkCount, totalBytes, e.getMessage());
                                    }
                                    // 退出循环，不再尝试写入数据
                                    break;
                                }
                            }

                            // 如果没有读取到任何数据，记录警告信息并向客户端发送错误信息
                            if (!hasReceivedData) {
                                log.warn("从未A2A平台输入流中读取到任何数据，可能上游服务未返回有效数据");
                                try {
                                    writeErrorToStream(outputStream, SecurityUtils.getSafeErrorMessage("STREAM_READ_ERROR"));
                                } catch (Exception e) {
                                    log.warn("向已断开的客户端写入错误信息失败: {}", e.getMessage());
                                }
                            }

                            long endTime = System.currentTimeMillis();
                            log.info("异步流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                                    chunkCount, totalBytes, (endTime - startTime));
                        } catch (IOException e) {
                            long errorTime = System.currentTimeMillis();
                            if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                                log.info("读取A2A平台响应数据时检测到流已关闭 - 耗时: {}ms，用户ID: {}",
                                        (errorTime - startTime), userId);
                            } else {
                                log.error("读取A2A平台响应数据时发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                            }

                            try {
                                writeErrorToStream(outputStream, SecurityUtils.sanitizeErrorMessage(e));
                            } catch (Exception writeException) {
                                log.warn("向已断开的客户端写入错误信息失败: {}", writeException.getMessage());
                            }
                        }
                    } else {
                        long errorTime = System.currentTimeMillis();
                        log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                        try {
                            writeErrorToStream(outputStream, SecurityUtils.getSafeErrorMessage("CONNECTION_ERROR"));
                        } catch (Exception e) {
                            log.warn("向已断开的客户端写入错误信息失败: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    long errorTime = System.currentTimeMillis();
                    log.error("异步流式传输过程中发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                    try {
                        writeErrorToStream(outputStream, SecurityUtils.sanitizeErrorMessage(e));
                    } catch (Exception writeException) {
                        log.warn("向已断开的客户端写入错误信息失败: {}", writeException.getMessage());
                    }
                } finally {
                    // 确保输入流被关闭
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            log.debug("关闭A2A平台输入流时发生异常: {}", e.getMessage());
                        }
                    }
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
     * @param abilityCode  能力中心编码
     * @param entityCode   实体编码
     * @param userId       用户ID
     * @param message      消息内容
     * @param taskId       任务ID
     * @param contextId    上下文ID
     * @param responseType 响应类型（用于日志）
     * @param satoken      认证令牌
     * @param attachments  附件列表
     * @return StreamingResponseBody 流式响应体
     */
    private StreamingResponseBody createStreamingResponse(String abilityCode, String entityCode, String userId,
                                                          String message, String taskId, String contextId, String responseType, String satoken, List<Attachment> attachments) {
        return outputStream -> {
            long startTime = System.currentTimeMillis();
            log.info("开始处理{}格式流式响应 - 用户ID: {}, 消息: {}", responseType, userId, message);

            try (InputStream inputStream = a2aCommunicationService.sendJsonRpcMessageToA2APlatform(
                    abilityCode, entityCode, userId, message, taskId, contextId, satoken, attachments)) {

                if (inputStream != null) {
                    log.info("成功获取A2A平台输入流，开始流式传输数据");

                    // 先将数据读取到内存中
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int bytesRead;
                    byte[] data = new byte[1024];

                    try {
                        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, bytesRead);
                        }

                        buffer.flush();
                        byte[] responseData = buffer.toByteArray();
                        log.info("成功读取完整响应数据 - 总大小: {} 字节, 用户ID: {}", responseData.length, userId);

                        // 然后将数据流式传输给客户端
                        int offset = 0;
                        int chunkSize = 1024;
                        int chunkCount = 0;
                        boolean hasReadData = false;

                        while (offset < responseData.length && !Thread.currentThread().isInterrupted()) {
                            try {
                                int remaining = responseData.length - offset;
                                int currentChunkSize = Math.min(chunkSize, remaining);

                                // 写入当前块数据
                                outputStream.write(responseData, offset, currentChunkSize);
                                outputStream.flush();

                                // 记录当前块的内容
                                String chunkContent = new String(responseData, offset, currentChunkSize, StandardCharsets.UTF_8);
                                log.info("=== 发送到客户端的数据块开始 ===");
                                log.info("块序号: {}", chunkCount);
                                log.info("块大小: {} 字节", currentChunkSize);
                                log.info("数据内容:\n{}", chunkContent);
                                log.info("=== 发送到客户端的数据块结束 ===");

                                offset += currentChunkSize;
                                chunkCount++;
                                hasReadData = true;

                                // 每5个数据块记录一次传输统计信息
                                if (chunkCount % 5 == 0) {
                                    long currentTime = System.currentTimeMillis();
                                    log.info("已传输数据块数量: {}, 总字节数: {}, 已用时间: {}ms - 用户ID: {}",
                                            chunkCount, offset, (currentTime - startTime), userId);
                                }

                                // 让出CPU时间片，避免过度占用CPU
                                Thread.yield();
                            } catch (IOException e) {
                                // 客户端可能已经断开连接
                                if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                                    log.info("客户端连接已断开，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}",
                                            userId, chunkCount, offset);
                                } else {
                                    log.warn("客户端连接已断开或发生IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}",
                                            userId, chunkCount, offset, e.getMessage());
                                }
                                // 退出循环，不再尝试写入数据
                                break;
                            }
                        }

                        // 如果没有读取到任何数据，记录警告信息并向客户端发送错误信息
                        if (!hasReadData) {
                            log.warn("从未A2A平台输入流中读取到任何数据，可能上游服务未返回有效数据");
                            try {
                                writeErrorToStream(outputStream, SecurityUtils.getSafeErrorMessage("STREAM_READ_ERROR"));
                            } catch (Exception e) {
                                log.warn("向已断开的客户端写入错误信息失败: {}", e.getMessage());
                            }
                        }

                        long endTime = System.currentTimeMillis();
                        log.info("JSON-RPC格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                                chunkCount, offset, (endTime - startTime));
                    } catch (IOException e) {
                        long errorTime = System.currentTimeMillis();
                        if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                            log.info("读取A2A平台响应数据时检测到流已关闭 - 耗时: {}ms，用户ID: {}",
                                    (errorTime - startTime), userId);
                        } else {
                            log.error("读取A2A平台响应数据时发生异常 - 耗时: {}ms", (errorTime - startTime), e);
                        }

                        try {
                            writeErrorToStream(outputStream, SecurityUtils.sanitizeErrorMessage(e));
                        } catch (Exception writeException) {
                            log.warn("向已断开的客户端写入错误信息失败: {}", writeException.getMessage());
                        }
                    }
                } else {
                    long errorTime = System.currentTimeMillis();
                    log.error("从A2A平台获取的输入流为空，耗时: {}ms", (errorTime - startTime));
                    try {
                        writeErrorToStream(outputStream, SecurityUtils.getSafeErrorMessage("CONNECTION_ERROR"));
                    } catch (Exception e) {
                        log.warn("向已断开的客户端写入错误信息失败: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                log.error("{}格式流式传输过程中发生异常 - 耗时: {}ms", responseType, (errorTime - startTime), e);
                try {
                    writeErrorToStream(outputStream, SecurityUtils.sanitizeErrorMessage(e));
                } catch (Exception writeException) {
                    log.warn("向已断开的客户端写入错误信息失败: {}", writeException.getMessage());
                }
            }
            // 确保输入流被关闭
        };
    }

    /**
     * 向输出流发送SSE事件
     *
     * @param outputStream 输出流
     * @param event        SSE事件内容
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

    /**
     * 流式发送消息到A2A平台（支持文件上传）
     *
     * @param file 文件
     * @param userId 用户ID
     * @param message 消息内容
     * @param contextId 上下文ID
     * @param satoken 认证令牌
     * @return StreamingResponseBody 流式响应体
     */
    // 已删除 stream-message-with-file 接口，相关功能整合到 supplement-info 接口中

    /**
     * 补充信息接口
     * 支持文件上传，自动判断是否有文件
     */
    @PostMapping(value = "/supplement-info", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @Operation(summary = "补充信息", description = "处理前端收集的动态表单数据并发送到上游智能体，支持文件上传")
    public ApiResponse<String> handleDynamicFormSupplement(
            @Parameter(description = "用户ID") @RequestParam(value = "userId", required = false) String userId,
            @Parameter(description = "任务ID") @RequestParam(value = "taskId", required = false) String taskId,
            @Parameter(description = "上下文ID") @RequestParam(value = "contextId", required = false) String contextId,
            @Parameter(description = "A2A协议代理实体编码") @RequestParam(value = "a2aProtocolProxyEntityCode", required = false) String a2aProtocolProxyEntityCode,
            @Parameter(description = "表单数据") @RequestParam Map<String, String> allParams,
            @Parameter(description = "上传的文件") @RequestPart(value = "files", required = false) MultipartFile[] files,
            @Parameter(hidden = true) @SaToken String satoken) {

        try {
            // 处理文件上传
            Map<String, String> fileUrls = new HashMap<>();
            if (files != null && files.length > 0) {
                log.info("接收到 {} 个文件上传", files.length);
                for (int i = 0; i < files.length; i++) {
                    MultipartFile file = files[i];
                    if (!file.isEmpty()) {
                        // 上传文件到文件仓库
                        Map<String, String> fileInfo = uploadFileToRepository(file, contextId, taskId);
                        if (fileInfo != null && "true".equals(fileInfo.get("success"))) {
                            fileUrls.put("file_" + (i + 1), fileInfo.get("fileUrl"));
                            log.info("文件 {} 上传成功: {}", fileInfo.get("originalName"), fileInfo.get("fileUrl"));
                        } else {
                            log.warn("文件上传失败: {}", file.getOriginalFilename());
                        }
                    }
                }
            }

            // 构建完整的formData
            Map<String, Object> formData = new HashMap<>(allParams);
            if (!fileUrls.isEmpty()) {
                formData.put("uploadedFiles", fileUrls);
                formData.put("fileCount", String.valueOf(fileUrls.size()));
            }

            // 使用JSON格式处理参数
            String actualUserId = userId != null ? userId : allParams.get("userId");
            String actualTaskId = taskId != null ? taskId : allParams.get("taskId");
            String actualContextId = contextId != null ? contextId : allParams.get("contextId");
            String actualEntityCode = a2aProtocolProxyEntityCode != null ? 
                a2aProtocolProxyEntityCode : allParams.get("a2aProtocolProxyEntityCode");

            // 验证必填参数
            if (actualUserId == null || actualTaskId == null || 
                actualContextId == null || actualEntityCode == null) {
                return ApiResponse.error("缺少必填参数: userId, taskId, contextId, a2aProtocolProxyEntityCode");
            }

            log.info("处理补充信息请求 - 用户ID: {}, 任务ID: {}, 表单字段数: {}, 文件数: {}",
                    actualUserId, actualTaskId, formData.size(), 
                    fileUrls.size());

            // 将表单数据转换为JSON字符串作为用户输入
            String userInput = buildSupplementMessageFromForm(formData);

            // 使用现有的补充信息处理方法
            return a2aCommunicationService.handleSupplementInfo(
                    actualUserId,
                    actualTaskId,
                    userInput,
                    actualContextId,
                    actualEntityCode,
                    satoken);

        } catch (Exception e) {
            log.error("处理补充信息失败", e);
            return ApiResponse.error("发送补充信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 通用文件上传方法
     * 用于补充信息、发送对话等场景的文件上传
     * 
     * @param file 要上传的文件
     * @param contextId 上下文ID
     * @param taskId 任务ID（可选）
     * @return 上传后的文件信息
     */
    private Map<String, String> uploadFileToRepository(MultipartFile file, String contextId, String taskId) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("文件为空，跳过上传");
                return null;
            }
            
            // 获取用户实体编码
            String userEntityCode = validateAndGetUserEntityCode();
            
            // 构建目录结构: contextId/taskId/
            String directory = fileRepositoryService.buildDirectoryPath(contextId, taskId);
            
            // 调用文档库的文件上传接口
            FileUploadResponse response = fileRepositoryService.uploadFile(userEntityCode, directory, file);
            
            if (response != null && !"upload_failed".equals(response.getName())) {
                Map<String, String> fileInfo = new HashMap<>();
                fileInfo.put("originalName", file.getOriginalFilename());
                fileInfo.put("fileName", response.getName());
                fileInfo.put("fileUrl", response.getUrl());
                fileInfo.put("fileSize", String.valueOf(file.getSize()));
                fileInfo.put("success", "true");
                
                log.info("文件上传成功: originalName={}, fileName={}, fileUrl={}", 
                        file.getOriginalFilename(), response.getName(), response.getUrl());
                return fileInfo;
            } else {
                String errorMessage = (response != null && response.getUrl() != null) ? 
                    response.getUrl() : "文件上传失败";
                log.error("文件上传失败: {}", errorMessage);
                return null;
            }
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return null;
        }
    }
    
    /**
     * 将表单数据构建成JSON字符串格式的用户输入
     * 用于补充信息接口
     * 
     * @param formData 表单数据
     * @return JSON格式的字符串
     */
    private String buildSupplementMessageFromForm(Map<String, Object> formData) {
        try {
            // 使用共享的ObjectMapper实例将表单数据转换为JSON字符串
            return objectMapper.writeValueAsString(formData);
        } catch (Exception e) {
            log.error("构建补充信息JSON时发生异常", e);
            // 如果转换失败，返回空的JSON对象
            return "{}";
        }
    }

    /**
     * 验证并获取当前用户实体编码
     *
     * @return 用户实体编码
     * @throws IllegalStateException 如果无法获取用户信息
     */
    public String validateAndGetUserEntityCode() {
        // 使用现有的工具类生成用户实体编码
        String userEntityCode = UserEntityCodeUtil.getCurrentUserEntityCode();
        if (userEntityCode == null) {
            throw new IllegalStateException("无法获取当前用户信息");
        }
        return userEntityCode;
    }
    
    /**
     * 根据任务ID获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @GetMapping("/task-status")
    @Operation(summary = "获取任务状态", description = "根据任务ID获取任务状态信息")
    public ResponseEntity<Map<String, Object>> getTaskStatus(
            @Parameter(description = "任务ID", required = true)
            @RequestParam String taskId) {
        
        log.info("接收到获取任务状态请求，任务ID: {}", taskId);
        
        try {
            // 构建请求URL
            String baseUrl = kunlunProperties.getA2a().getTaskStatus().getUrl();
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("taskId", taskId)
                    .toUriString();
            
            log.info("获取任务状态，请求URL: {}", url);
            
            // 直接调用任务状态接口
            String response = restTemplate.getForObject(url, String.class);
            
            // 解析响应为Map
            Map<String, Object> result = new HashMap<>();
            if (response != null) {
                result = objectMapper.readValue(response, Map.class);
            }
            
            log.info("获取任务状态成功，任务ID: {}, 响应: {}", taskId, result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取任务状态失败，任务ID: {}", taskId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取任务状态失败: " + e.getMessage());
            errorResponse.put("data", new ArrayList<>());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}