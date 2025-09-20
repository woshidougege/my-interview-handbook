package com.noah.superagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.request.A2AMessageRequest;
import com.noah.superagent.common.dto.request.A2ASupplementInfoRequest;
import com.noah.superagent.service.A2ACommunicationService;
import com.noah.superagent.common.config.A2APlatformConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

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
    
    public A2ACommunicationServiceImpl(
            @Value("${a2a.platform.base-url}") String a2aPlatformBaseUrl,
            A2APlatformConfig a2aPlatformConfig) {
        this.a2aPlatformBaseUrl = a2aPlatformBaseUrl;
        this.a2aPlatformConfig = a2aPlatformConfig;
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        this.defaultAbilityCode = a2aPlatformConfig.getDefaultAbilityCode();
        this.defaultEntityCode = a2aPlatformConfig.getDefaultEntityCode();
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
        });
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

        PipedInputStream pipedInputStream = new PipedInputStream();
        try {
            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

            CompletableFuture.runAsync(() -> {
                try {
                    // 构建JSON-RPC请求体
                    String requestId = "requestId_" + System.currentTimeMillis();

                    // 处理taskId和contextId的默认值
                    String actualTaskId = (taskId == null || taskId.isEmpty()) ? "task_" + System.currentTimeMillis() : taskId;
                    String actualContextId = (contextId == null || "null".equals(contextId) || contextId.isEmpty()) ? "" : contextId;

                    String requestBody = String.format(
                            "{\"jsonrpc\":\"2.0\",\"method\":\"message/stream\",\"id\":\"%s\",\"params\":{\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"%s\"}],\"kind\":\"message\",\"taskId\":\"%s\",\"contextId\":\"%s\"}}}",
                            requestId, message, actualTaskId, actualContextId);

                    // 设置请求头
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
                    
                    // 使用HttpEntity封装请求
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                    // 创建请求回调，用于发送请求
                    RequestCallback requestCallback = request -> {
                        request.getHeaders().addAll(headers);
                        if (requestBody != null) {
                            request.getBody().write(requestBody.getBytes(StandardCharsets.UTF_8));
                        }
                    };

                    // 创建响应提取器，用于处理响应流
                    ResponseExtractor<Void> responseExtractor = response -> {
                        try (InputStream inputStream = response.getBody()) {
                            log.info("成功获取A2A平台输入流，开始转发数据");
                            byte[] buffer = new byte[8192]; // 增大缓冲区
                            int bytesRead;
                            int totalBytes = 0;
                            int chunkCount = 0;
                            
                            // 用于收集所有接收到的数据
                            ByteArrayOutputStream allReceivedData = new ByteArrayOutputStream();
                            
                            // 直接转发流数据，确保只处理一次
                            while (!Thread.currentThread().isInterrupted() && (bytesRead = inputStream.read(buffer)) != -1) {
                                try {
                                    // 将数据添加到总数据收集中
                                    allReceivedData.write(buffer, 0, bytesRead);
                                    
                                    // 记录原始数据块
                                    String rawData = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                                    log.debug("从A2A平台接收到原始数据块 - 块序号: {}, 大小: {} 字节, 数据预览: {}", 
                                        chunkCount, bytesRead, rawData.substring(0, Math.min(rawData.length(), 100)).replace("\n", "\\n").replace("\r", "\\r"));
                                    
                                    // 直接转发数据
                                    pipedOutputStream.write(buffer, 0, bytesRead);
                                    pipedOutputStream.flush(); // 确保数据立即发送
                                    totalBytes += bytesRead;
                                    chunkCount++;
                                    
                                    // 每50个数据块记录一次详细日志
                                    if (chunkCount % 50 == 0) {
                                        log.debug("已转发数据块数量: {}, 总字芯数: {}", chunkCount, totalBytes);
                                    }
                                } catch (IOException e) {
                                    log.warn("写入PipedOutputStream时发生异常，可能客户端已断开连接", e);
                                    break;
                                }
                            }
                            
                            log.info("A2A平台数据转发完成 - 总数据块数: {}, 总字节数: {}", chunkCount, totalBytes);
                            
                            // 在结束时输出完整的接收数据到日志
                            String completeReceivedData = allReceivedData.toString(StandardCharsets.UTF_8.name());
                            log.info("从A2A平台接收到的完整响应数据长度: {} 字符", completeReceivedData.length());
                            if (completeReceivedData.length() > 0) {
                                // 只记录前2000个字符以避免日志过大
                                String receivedDataPreview = completeReceivedData.substring(0, Math.min(completeReceivedData.length(), 2000));
                                log.info("从A2A平台接收到的完整响应数据预览: \n{}", receivedDataPreview);
                                
                                // 检查是否有明显的重复模式
                                if (completeReceivedData.length() > 100) {
                                    String firstPart = completeReceivedData.substring(0, Math.min(100, completeReceivedData.length()));
                                    int secondOccurrence = completeReceivedData.indexOf(firstPart, 100);
                                    if (secondOccurrence > 0) {
                                        log.warn("检测到重复数据模式: 相同内容在位置 {} 出现第二次", secondOccurrence);
                                        // 记录重复部分的预览
                                        int repeatEnd = Math.min(secondOccurrence + 100, completeReceivedData.length());
                                        String repeatedPart = completeReceivedData.substring(secondOccurrence, repeatEnd);
                                        log.warn("重复内容预览: {}", repeatedPart.replace("\n", "\\n").replace("\r", "\\r"));
                                    }
                                }
                            }
                        }
                        return null;
                    };

                    // 执行请求并处理响应
                    String url = String.format("%s/kunlun/a2a/api/%s/entity/%s/userid/%s",
                            a2aPlatformBaseUrl, abilityCode, entityCode, userId);
                    restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
                    
                    log.info("JSON-RPC消息发送完成 - 用户ID: {}", userId);
                } catch (Exception e) {
                    log.error("发送JSON-RPC消息时发生异常 - 用户ID: {}", userId, e);
                    try {
                        String errorResponse = "[ERROR] 发送JSON-RPC消息异常: " + e.getMessage();
                        pipedOutputStream.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ioException) {
                        log.error("写入错误信息到输出流时发生异常", ioException);
                    }
                } finally {
                    try {
                        pipedOutputStream.close();
                    } catch (IOException e) {
                        log.error("关闭PipedOutputStream时发生异常", e);
                    }
                }
            });
        } catch (IOException e) {
            log.error("创建PipedOutputStream时发生异常", e);
        }

        return pipedInputStream;
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
        });
    }
}