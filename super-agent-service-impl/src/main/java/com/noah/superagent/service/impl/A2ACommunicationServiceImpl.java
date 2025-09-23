package com.noah.superagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noah.superagent.common.config.A2APlatformConfig;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.service.A2ACommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
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
        
        // 配置RestTemplate超时设置
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        // 设置连接和读取超时 - 根据规范设置更长的超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);  // 30秒连接超时
        factory.setReadTimeout(300000);    // 300秒读取超时(5分钟)，符合规范要求
        factory.setBufferRequestBody(false); // 禁用请求体缓冲
        
        this.restTemplate.setRequestFactory(factory);
        
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
            log.info("发送到A2A平台的完整请求体: {}", requestBody);
            
            // 设置请求头 - 添加保持连接的配置
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
            headers.set("Connection", "keep-alive");
            headers.set("Cache-Control", "no-cache");
            headers.set("X-Accel-Buffering", "no"); // 禁用nginx缓冲

            // 使用execute方法直接获取响应流，实现实时传输
            RequestCallback requestCallback = clientHttpRequest -> {
                clientHttpRequest.getHeaders().addAll(headers);
                try (OutputStream os = clientHttpRequest.getBody()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            };

            ResponseExtractor<InputStream> responseExtractor = response -> {
                log.info("收到A2A平台响应 - 状态码: {}", response.getStatusCode());
                log.info("A2A平台响应头: {}", response.getHeaders());
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("流式消息发送成功 - 用户ID: {}", userId);
                    InputStream body = response.getBody();
                    if (body != null) {
                        log.info("成功获取响应流，准备开始传输数据");
                        return body;
                    } else {
                        log.error("响应体为空 - 用户ID: {}", userId);
                        return new ByteArrayInputStream("[ERROR] 响应体为空".getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    log.error("流式消息发送失败 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                    
                    // 读取错误响应内容
                    try {
                        InputStream errorStream = response.getBody();
                        if (errorStream != null) {
                            String errorContent = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                            log.error("A2A平台错误响应内容: {}", errorContent);
                            return new ByteArrayInputStream(("[ERROR] " + errorContent).getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        log.error("读取错误响应内容时发生异常", e);
                    }
                    
                    String errorResponse = "[ERROR] 流式消息发送失败，状态码: " + response.getStatusCode();
                    return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            };

            log.info("开始执行A2A平台请求...");
            InputStream result = restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
            log.info("A2A平台请求执行完成，返回流对象: {}", result);
            return result;
            
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
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            root.put("jsonrpc", "2.0");
            root.put("method", "message/stream");
            root.put("id", "request_" + System.currentTimeMillis());
            
            ObjectNode params = mapper.createObjectNode();
            ObjectNode messageNode = mapper.createObjectNode();
            
            messageNode.put("role", "user");
            messageNode.put("kind", "message");
            messageNode.put("taskId", sessionId != null ? sessionId : "task_" + System.currentTimeMillis());
            
            ArrayNode parts = mapper.createArrayNode();
            ObjectNode textPart = mapper.createObjectNode();
            textPart.put("kind", "text");
            textPart.put("text", message);
            parts.add(textPart);
            
            messageNode.set("parts", parts);
            params.set("message", messageNode);
            root.set("params", params);
            
            String json = mapper.writeValueAsString(root);
            log.info("构建的JSON-RPC请求: {}", json);
            return json;
        } catch (Exception e) {
            log.error("构建JSON-RPC请求时发生异常，使用回退方案", e);
            // 回退到简单的字符串格式，确保消息内容正确转义
            String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            return String.format(
                "{\"jsonrpc\":\"2.0\",\"method\":\"message/stream\",\"id\":\"request_%s\",\"params\":{\"message\":{\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"%s\"}],\"kind\":\"message\",\"taskId\":\"%s\"}}}",
                System.currentTimeMillis(), 
                escapedMessage, 
                sessionId != null ? sessionId : "task_" + System.currentTimeMillis()
            );
        }
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
        long startTime = System.currentTimeMillis();
        log.info("开始发送JSON-RPC消息到A2A平台 - 能力中心编码: {}, 实体编码: {}, 用户ID: {}, 任务ID: {}, 上下文ID: {}, 消息长度: {}",
                abilityCode, entityCode, userId, taskId, contextId, message != null ? message.length() : 0);

        try {
            // 参数验证
            if (abilityCode == null || abilityCode.trim().isEmpty()) {
                log.warn("能力中心编码为空，使用默认值 - 用户ID: {}", userId);
                abilityCode = getDefaultAbilityCode();
            }
            if (entityCode == null || entityCode.trim().isEmpty()) {
                log.warn("实体编码为空，使用默认值 - 用户ID: {}", userId);
                entityCode = getDefaultEntityCode();
            }
            if (userId == null || userId.trim().isEmpty()) {
                log.error("用户ID不能为空");
                throw new IllegalArgumentException("用户ID不能为空");
            }
            if (message == null || message.trim().isEmpty()) {
                log.warn("消息内容为空 - 用户ID: {}", userId);
                message = "";
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            root.put("jsonrpc", "2.0");
            root.put("method", "message/stream");
            root.put("id", "requestId_" + System.currentTimeMillis());
            
            ObjectNode params = mapper.createObjectNode();
            ObjectNode messageNode = mapper.createObjectNode();
            
            messageNode.put("role", "user");
            messageNode.put("kind", "message");
            
            // 处理taskId和contextId的默认值
            String actualTaskId = (taskId == null || taskId.isEmpty()) ? "task_" + System.currentTimeMillis() : taskId;
            String actualContextId = (contextId == null || "null".equals(contextId) || contextId.isEmpty()) ? "" : contextId;
            
            messageNode.put("taskId", actualTaskId);
            if (!actualContextId.isEmpty()) {
                messageNode.put("contextId", actualContextId);
            }
            
            ArrayNode parts = mapper.createArrayNode();
            ObjectNode textPart = mapper.createObjectNode();
            textPart.put("kind", "text");
            textPart.put("text", message);
            parts.add(textPart);
            
            messageNode.set("parts", parts);
            params.set("message", messageNode);
            root.set("params", params);
            
            String requestBody = mapper.writeValueAsString(root);
            log.debug("构建的JSON-RPC请求体: {}", requestBody);

            // 设置请求头 - 添加保持连接的配置
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
            headers.set("Connection", "keep-alive");
            headers.set("Cache-Control", "no-cache");
            headers.set("X-Accel-Buffering", "no"); // 禁用nginx缓冲
            headers.set("User-Agent", "SuperAgent/1.0");

            log.debug("请求头信息: Content-Type={}, Accept={}", 
                    headers.getContentType(), headers.getAccept());

            // 创建请求回调
            RequestCallback requestCallback = clientHttpRequest -> {
                clientHttpRequest.getHeaders().addAll(headers);
                try (OutputStream os = clientHttpRequest.getBody()) {
                    byte[] requestBytes = requestBody.getBytes(StandardCharsets.UTF_8);
                    log.debug("写入请求体，字节数: {}", requestBytes.length);
                    os.write(requestBytes);
                    os.flush();
                } catch (IOException e) {
                    log.error("写入请求体时发生IO异常 - 用户ID: {}", userId, e);
                    throw e;
                }
            };

            // 创建响应提取器
            ResponseExtractor<InputStream> responseExtractor = response -> {
                long responseTime = System.currentTimeMillis() - startTime;
                log.info("收到A2A平台响应 - 状态码: {}, 响应时间: {}ms", response.getStatusCode(), responseTime);
                log.debug("A2A平台响应头: {}", response.getHeaders());
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("流式消息发送成功 - 用户ID: {}, 响应时间: {}ms", userId, responseTime);
                    InputStream body = response.getBody();
                    if (body != null) {
                        log.debug("成功获取响应流，准备开始传输数据 - 用户ID: {}", userId);
                        // 直接读取数据到缓冲区，避免连接关闭问题
                        try {
                            // 创建一个新的ByteArrayInputStream包装原始数据
                            // 这样可以避免底层连接关闭的问题
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int nRead;
                            byte[] data = new byte[1024];
                            while ((nRead = body.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }
                            buffer.flush();
                            byte[] byteArray = buffer.toByteArray();
                            log.info("预读取数据成功 - 大小: {} 字节", byteArray.length);
                            return new ByteArrayInputStream(byteArray);
                        } catch (IOException e) {
                            log.error("预读取响应流时发生异常 - 用户ID: {}", userId, e);
                            return new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        log.error("响应体为空 - 用户ID: {}", userId);
                        return new ByteArrayInputStream("[ERROR] 响应体为空".getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    log.error("流式消息发送失败 - 用户ID: {}, 状态码: {}, 响应时间: {}ms", 
                            userId, response.getStatusCode(), responseTime);
                    
                    // 读取错误响应内容
                    try {
                        InputStream errorStream = response.getBody();
                        if (errorStream != null) {
                            String errorContent = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                            log.error("A2A平台错误响应内容: {}", errorContent);
                            return new ByteArrayInputStream(("[ERROR] " + errorContent).getBytes(StandardCharsets.UTF_8));
                        } else {
                            log.warn("错误响应的响应体为空 - 用户ID: {}", userId);
                        }
                    } catch (Exception e) {
                        log.error("读取错误响应内容时发生异常 - 用户ID: {}", userId, e);
                    }
                    
                    String errorResponse = String.format("[ERROR] 流式消息发送失败，状态码: %s，耗时: %dms", 
                            response.getStatusCode(), responseTime);
                    return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            };

            // 构建并验证URL
            String url = String.format("%s/kunlun/a2a/api/%s/entity/%s/userid/%s",
                    a2aPlatformBaseUrl, abilityCode, entityCode, userId);
                    
            if (a2aPlatformBaseUrl == null || a2aPlatformBaseUrl.trim().isEmpty()) {
                log.error("A2A平台基础URL未配置");
                throw new IllegalStateException("A2A平台基础URL未配置");
            }
                    
            log.info("准备向A2A平台发送请求 - URL: {}, 方法: POST", url);
            log.debug("完整请求URL: {}", url);
            
            // 执行请求并返回输入流
            InputStream result = restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("成功执行A2A平台请求 - 总耗时: {}ms, 返回流对象: {}", totalTime, result);
            return result;
            
        } catch (IllegalArgumentException e) {
            log.error("参数验证失败 - 用户ID: {}, 异常: {}", userId, e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.error("系统状态异常 - 用户ID: {}, 异常: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("发送JSON-RPC消息时发生异常 - 用户ID: {}, 总耗时: {}ms", userId, totalTime, e);
            String errorResponse = String.format("[ERROR] 发送请求异常: %s，耗时: %dms", 
                    e.getMessage(), totalTime);
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
     * 统一处理从A2A平台获取的流数据并传输到客户端
     * 
     * @param inputStream 从A2A平台获取的输入流
     * @param outputStream 输出到客户端的流
     * @param userId 用户ID
     * @param contextId 上下文ID
     * @param responseType 响应类型（用于日志）
     */
    public void handleA2AStreamResponse(InputStream inputStream, OutputStream outputStream, 
            String userId, String contextId, String responseType) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("开始处理{}格式流式响应 - 用户ID: {}, 上下文ID: {}", responseType, userId, contextId);
        
        if (inputStream != null) {
            log.info("成功获取A2A平台输入流，开始流式传输数据 - 用户ID: {}, 上下文ID: {}", userId, contextId);
            
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                int totalBytes = 0;
                int chunkCount = 0;
                boolean hasReceivedData = false;
                
                // 设置读取超时，避免无限等待
                log.info("准备处理输入流 - 用户ID: {}, 上下文ID: {}", userId, contextId);
                
                // 使用BufferedInputStream提高性能
                BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // 检查流是否可用
                        if (bufferedInput.available() > 0) {
                            log.debug("流中有可用数据: {} 字节", bufferedInput.available());
                        }
                        
                        // 读取数据
                        bytesRead = bufferedInput.read(buffer);
                        
                        if (bytesRead == -1) {
                            // 流正常结束
                            log.info("A2A平台输入流正常结束 - 用户ID: {}, 总传输字节数: {}, 数据块数: {}", 
                                    userId, totalBytes, chunkCount);
                            break;
                        }
                        
                        if (bytesRead == 0) {
                            // 没有数据，短暂等待
                            Thread.sleep(100);
                            continue;
                        }
                        
                        // 标记已收到数据
                        hasReceivedData = true;
                        
                        // 记录接收到的数据块
                        String chunkData = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        log.info("=== 接收到数据块开始 ===");
                        log.info("块序号: {}", chunkCount);
                        log.info("大小: {} 字节", bytesRead);
                        log.info("数据内容:\n{}", chunkData);
                        log.info("=== 接收到数据块结束 ===");
                        
                        // 立即转发到客户端
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                        totalBytes += bytesRead;
                        chunkCount++;
                        
                        // 每5个数据块记录一次详细日志
                        if (chunkCount % 5 == 0) {
                            long currentTime = System.currentTimeMillis();
                            log.info("已传输数据块数量: {}, 总字节数: {}, 已用时间: {}ms - 用户ID: {}", 
                                    chunkCount, totalBytes, (currentTime - startTime), userId);
                        }
                        
                        // 小延迟避免CPU占用过高
                        Thread.sleep(10);
                        
                    } catch (java.net.SocketTimeoutException e) {
                        log.warn("读取超时 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}", 
                                userId, chunkCount, totalBytes);
                        break;
                    } catch (IOException e) {
                        if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                            log.info("A2A平台主动关闭连接或客户端断开 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}", 
                                    userId, chunkCount, totalBytes);
                            break;
                        }
                        
                        // 检查是否是连接重置
                        if (e.getMessage() != null && (e.getMessage().contains("Connection reset") || 
                                                     e.getMessage().contains("Connection refused"))) {
                            log.warn("连接被重置或拒绝 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}", 
                                    userId, chunkCount, totalBytes);
                            break;
                        }
                        
                        log.error("IO异常，停止数据传输 - 用户ID: {}, 已传输数据块数: {}, 总字节数: {}, 异常: {}", 
                                userId, chunkCount, totalBytes, e.getMessage());
                        break;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("数据传输线程被中断 - 用户ID: {}", userId);
                        break;
                    }
                }
                
                long endTime = System.currentTimeMillis();
                
                if (hasReceivedData) {
                    log.info("{}格式流式传输完成 - 总数据块数: {}, 总字节数: {}, 总耗时: {}ms - 用户ID: {}", 
                            responseType, chunkCount, totalBytes, (endTime - startTime), userId);
                } else {
                    log.warn("{}格式流式传输完成，但未收到任何数据 - 总耗时: {}ms - 用户ID: {}", 
                            responseType, (endTime - startTime), userId);
                }
                
            } finally {
                // 确保流被正确关闭
                try {
                    inputStream.close();
                    log.debug("已关闭A2A平台输入流 - 用户ID: {}", userId);
                } catch (IOException e) {
                    log.debug("关闭输入流时发生异常: {}", e.getMessage());
                }
            }
        } else {
            long errorTime = System.currentTimeMillis();
            log.error("从A2A平台获取的输入流为空，耗时: {}ms - 用户ID: {}, 上下文ID: {}", 
                    (errorTime - startTime), userId, contextId);
            throw new IOException("无法从A2A平台获取响应流");
        }
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