package com.noah.superagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.request.A2AMessageRequest;
import com.noah.superagent.common.dto.request.A2ASupplementInfoRequest;
import com.noah.superagent.service.A2ACommunicationService;
import com.noah.superagent.common.config.A2APlatformConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A2A通信服务实现类
 * 用于处理与上游智平台的Agent-to-Agent通信
 */
@Slf4j
@Service
public class A2ACommunicationServiceImpl implements A2ACommunicationService {
    
    private final String a2aPlatformBaseUrl;
    private final A2APlatformConfig a2aPlatformConfig;
    private final RestTemplate restTemplate;
    private final String defaultAbilityCode;
    private final String defaultEntityCode;
    private final Executor aiChatExecutionExecutor;
    
    public A2ACommunicationServiceImpl(
            @Value("${a2a.platform.base-url}") String a2aPlatformBaseUrl,
            A2APlatformConfig a2aPlatformConfig,
            @Qualifier("ai-chat-execution-executor") Executor aiChatExecutionExecutor) {
        this.a2aPlatformBaseUrl = a2aPlatformBaseUrl;
        this.a2aPlatformConfig = a2aPlatformConfig;
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        this.defaultAbilityCode = a2aPlatformConfig.getDefaultAbilityCode();
        this.defaultEntityCode = a2aPlatformConfig.getDefaultEntityCode();
        this.aiChatExecutionExecutor = aiChatExecutionExecutor;
    }
    
    @Override
    public String getDefaultAbilityCode() {
        // 优先使用配置中的默认能力中心编码，如果没有配置则使用写死的默认值
        String configDefault = a2aPlatformConfig.getDefaultAbilityCode();
        return configDefault != null ? configDefault : "18961714392032";
    }
    
    @Override
    public String getDefaultEntityCode() {
        // 优先使用配置中的默认实体编码，如果没有配置则使用写死的默认值
        String configDefault = a2aPlatformConfig.getDefaultEntityCode();
        return configDefault != null ? configDefault : "ENTITY_test_pp_dispatch";
    }
    
    @Override
    public ApiResponse<String> sendMessageToA2APlatform(String userId, String message, String sessionId) {
        log.info("发送消息到A2A平台 - 用户ID: {}, 消息: {}, 会话ID: {}", userId, message, sessionId);
        
        try {
            // 构建请求URL
            String url = String.format("%s/kunlun/a2a/api/message", a2aPlatformBaseUrl);
            
            // 构建请求体
            String requestBody = String.format(
                "{\"userId\":\"%s\",\"message\":\"%s\",\"sessionId\":\"%s\"}", 
                userId, message, sessionId
            );
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("消息发送成功 - 用户ID: {}, 响应: {}", userId, response.getBody());
                return ApiResponse.success(response.getBody());
            } else {
                log.error("消息发送失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                return ApiResponse.error("消息发送失败，状态码: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("发送消息到A2A平台时发生异常 - 用户ID: {}", userId, e);
            return ApiResponse.error("发送消息异常: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<ApiResponse<String>> sendMessageToA2APlatformAsync(String userId, String message, String sessionId) {
        log.info("异步发送消息到A2A平台 - 用户ID: {}, 消息: {}, 会话ID: {}", userId, message, sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建请求URL
                String url = String.format("%s/kunlun/a2a/api/message", a2aPlatformBaseUrl);
                
                // 构建请求体
                String requestBody = String.format(
                    "{\"userId\":\"%s\",\"message\":\"%s\",\"sessionId\":\"%s\"}", 
                    userId, message, sessionId
                );
                
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                
                // 发送请求
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("异步消息发送成功 - 用户ID: {}, 响应: {}", userId, response.getBody());
                    return ApiResponse.success(response.getBody());
                } else {
                    log.error("异步消息发送失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                    return ApiResponse.error("异步消息发送失败，状态码: " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("异步发送消息到A2A平台时发生异常 - 用户ID: {}", userId, e);
                return ApiResponse.error("异步发送消息异常: " + e.getMessage());
            }
        }, aiChatExecutionExecutor);
    }
    
    /**
     * 执行流式HTTP请求到A2A平台。
     * @param abilityCode 能力中心编码
     * @param entityCode 实体编码
     * @param userId 用户ID
     * @param requestBody JSON-RPC格式的请求体
     * @return 响应的InputStream
     */
    private InputStream executeStreamRequest(String abilityCode, String entityCode, String userId, String requestBody) {
        try {
            // 构建请求URL
            String url = String.format("%s/kunlun/a2a/api/%s/entity/%s/userid/%s",
                    a2aPlatformBaseUrl, abilityCode, entityCode, userId);
            log.info("发送到A2A平台的完整请求URL: {}", url);
            log.debug("发送到A2A平台的完整请求体: {}", requestBody);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));

            // 使用execute方法直接获取响应流，实现实时传输
            RequestCallback requestCallback = clientHttpRequest -> {
                clientHttpRequest.getHeaders().addAll(headers);
                try (OutputStream os = clientHttpRequest.getBody()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            };

            ResponseExtractor<InputStream> responseExtractor = response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("流式消息发送成功 - 用户ID: {}", userId);
                    return response.getBody(); // 直接返回底层输入流
                } else {
                    log.error("流式消息发送失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                    // 统一错误格式，不使用SSE data前缀，与 sendJsonRpcMessageToA2APlatform 保持一致
                    String errorResponse = "[ERROR] 流式消息发送失败，状态码: " + response.getStatusCode();
                    return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            };

            return restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
        } catch (Exception e) {
            log.error("执行流式请求时发生异常 - 用户ID: {}", userId, e);
            String errorResponse = "[ERROR] 执行流式请求异常: " + e.getMessage();
            return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public InputStream streamMessageToA2APlatform(String userId, String message, String sessionId) {
        log.info("流式发送消息到A2A平台 - 用户ID: {}, 消息: {}, 会话ID: {}", userId, message, sessionId);

        // 构建JSON-RPC格式的请求体
        String requestBody = buildJsonRpcRequest(message, sessionId);

        // 复用 executeStreamRequest 方法
        return executeStreamRequest(defaultAbilityCode, defaultEntityCode, userId, requestBody);
    }
    
    /**
     * 构建JSON-RPC格式的请求
     * @param message 消息内容
     * @param sessionId 会话ID
     * @return JSON-RPC格式的请求字符串
     */
    private String buildJsonRpcRequest(String message, String sessionId) {
        // 生成随机请求ID
        String requestId = "request_" + System.currentTimeMillis();
        
        // 构建JSON-RPC请求
        String request = String.format(
            "{" +
                "\"jsonrpc\": \"2.0\"," +
                "\"method\": \"message/stream\"," +
                "\"id\": \"%s\"," +
                "\"params\": {" +
                    "\"message\": {" +
                        "\"role\": \"user\"," +
                        "\"parts\": [{" +
                            "\"kind\": \"text\"," +
                            "\"text\": \"%s\"" +
                        "}]," +
                        "\"kind\": \"message\"," +
                        "\"taskId\": \"%s\"" +
                    "}" +
                "}" +
            "}", 
            requestId, message, sessionId != null ? sessionId : "task_" + System.currentTimeMillis()
        );
        
        log.debug("构建的JSON-RPC请求: {}", request);
        return request;
    }
    
    /**
     * 发送JSON-RPC格式消息到A2A平台
     * 
     * @param abilityCode 能力中心编码
     * @param entityCode 实体编码
     * @param userId 用户ID
     * @param message 消息内容
     * @param taskId 任务ID
     * @param contextId 上下文ID
     * @return InputStream 输入流
     */
    @Override
    public InputStream sendJsonRpcMessageToA2APlatform(
            String abilityCode, String entityCode, String userId,
            String message, String taskId, String contextId) {
        log.info("发送JSON-RPC消息到A2A平台 - 能力中心编码: {}, 实体编码: {}, 用户ID: {}, 消息: {}",
                abilityCode, entityCode, userId, message);

        try {
            // 构建JSON-RPC请求体
            String requestId = "requestId_" + System.currentTimeMillis();
            
            // 处理taskId和contextId的默认值
            String actualTaskId = (taskId == null || taskId.isEmpty()) ? "task_" + System.currentTimeMillis() : taskId;
            String actualContextId = (contextId == null || "null".equals(contextId) || contextId.isEmpty()) ? "" : contextId;

            String requestBody = String.format(
                    "{\"jsonrpc\":\"2.0\",\"method\":\"message/stream\",\"id\":\"%s\",\"params\":{\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"%s\"}],\"kind\":\"message\",\"taskId\":\"%s\",\"contextId\":\"%s\"}}}",
                    requestId, message, actualTaskId, actualContextId);
            log.debug("请求体: {}", requestBody);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));

            // 创建请求回调
            RequestCallback requestCallback = clientHttpRequest -> {
                clientHttpRequest.getHeaders().addAll(headers);
                try (OutputStream os = clientHttpRequest.getBody()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            };

            // 创建响应提取器
            ResponseExtractor<InputStream> responseExtractor = response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("成功获取A2A平台响应流 - 用户ID: {}", userId);
                    return response.getBody();
                } else {
                    log.error("获取A2A平台响应失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                    String errorResponse = "[ERROR] 获取响应失败，状态码: " + response.getStatusCode();
                    return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            };

            // 执行请求并返回输入流
            String url = String.format("%s/kunlun/a2a/api/%s/entity/%s/userid/%s",
                    a2aPlatformBaseUrl, abilityCode, entityCode, userId);
                    
            return restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
            
        } catch (Exception e) {
            log.error("发送JSON-RPC消息时发生异常 - 用户ID: {}", userId, e);
            String errorResponse = "[ERROR] 发送请求异常: " + e.getMessage();
            return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    @Override
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> streamMessageToA2APlatformAsync(
            String userId, String message, String sessionId, String contextId) {
        log.info("异步流式发送消息到A2A平台 - 用户ID: {}, 消息: {}, 会话ID: {}, 上下文ID: {}", 
                userId, message, sessionId, contextId);

        return CompletableFuture.supplyAsync(() -> {
            StreamingResponseBody responseBody = outputStream -> {
                long startTime = System.currentTimeMillis();
                log.info("开始处理异步流式响应 - 用户ID: {}, 消息: {}", userId, message);

                // 所有参数都由后端生成
                String abilityCode = getDefaultAbilityCode();
                String entityCode = getDefaultEntityCode();
                // 由后端生成默认值
                String taskId = "task_" + System.currentTimeMillis();
                // 修改contextId从请求中获取，如果请求中没有则使用默认值
                String actualContextId = contextId != null ? contextId : "";

                try (InputStream inputStream = sendJsonRpcMessageToA2APlatform(
                        abilityCode, entityCode, userId, message, taskId, actualContextId)) {

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
                        log.info("异步流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms",
                                chunkCount, totalBytes, (endTime - startTime));
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
     * 向输出流写入错误信息
     * @param outputStream 输出流
     * @param errorMessage 错误信息
     */
    private void writeErrorToStream(OutputStream outputStream, String errorMessage) {
        try {
            outputStream.write(errorMessage.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            log.error("写入错误信息到输出流时发生异常", e);
        }
    }
    
    @Override
    public ApiResponse<String> handleSupplementInfo(String userId, String taskId, String userInput, String sessionId) {
        log.info("处理补充信息 - 用户ID: {}, 任务ID: {}, 用户输入: {}, 会话ID: {}", userId, taskId, userInput, sessionId);
        
        try {
            // 使用配置的能力中心编码和实体编码
            String abilityCode = a2aPlatformConfig.getDefaultAbilityCode();
            String entityCode = a2aPlatformConfig.getDefaultEntityCode();

            // 构建请求URL
            String url = String.format("%s/kl-dt-gateway/gateway/%s/twin/A2AProtocolProxy/entity/%s/service/executeTaskBlock",
                    a2aPlatformBaseUrl, abilityCode, entityCode);

            // 构建请求体
            String requestBody = String.format(
                "{\"userId\":\"%s\",\"taskId\":\"%s\",\"userInput\":\"%s\",\"sessionId\":\"%s\"}",
                userId, taskId, userInput, sessionId
            );
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("补充信息处理成功 - 用户ID: {}, 响应: {}", userId, response.getBody());
                return ApiResponse.success(response.getBody());
            } else {
                log.error("补充信息处理失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                return ApiResponse.error("补充信息处理失败，状态码: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("处理补充信息时发生异常 - 用户ID: {}", userId, e);
            return ApiResponse.error("处理补充信息异常: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<ApiResponse<String>> handleSupplementInfoAsync(String userId, String taskId, String userInput, String sessionId) {
        log.info("异步处理补充信息 - 用户ID: {}, 任务ID: {}, 用户输入: {}, 会话ID: {}", userId, taskId, userInput, sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 使用配置的能力中心编码和实体编码
                String abilityCode = a2aPlatformConfig.getDefaultAbilityCode();
                String entityCode = a2aPlatformConfig.getDefaultEntityCode();

                // 构建请求URL
                String url = String.format("%s/kl-dt-gateway/gateway/%s/twin/A2AProtocolProxy/entity/%s/service/executeTaskBlock",
                        a2aPlatformBaseUrl, abilityCode, entityCode);

                // 构建请求体
                String requestBody = String.format(
                    "{\"userId\":\"%s\",\"taskId\":\"%s\",\"userInput\":\"%s\",\"sessionId\":\"%s\"}",
                    userId, taskId, userInput, sessionId
                );
                
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                
                // 发送请求
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("异步补充信息处理成功 - 用户ID: {}, 响应: {}", userId, response.getBody());
                    return ApiResponse.success(response.getBody());
                } else {
                    log.error("异步补充信息处理失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                    return ApiResponse.error("异步补充信息处理失败，状态码: " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("异步处理补充信息时发生异常 - 用户ID: {}", userId, e);
                return ApiResponse.error("异步处理补充信息异常: " + e.getMessage());
            }
        }, aiChatExecutionExecutor);
    }
}