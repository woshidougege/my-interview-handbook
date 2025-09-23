package com.noah.superagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noah.superagent.common.config.A2APlatformConfig;
import com.noah.superagent.common.config.KunlunProperties;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.service.A2ACommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.*;
import java.net.SocketTimeoutException;
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

    private final A2APlatformConfig a2aPlatformConfig;
    private final KunlunProperties kunlunProperties;
    private final RestTemplate restTemplate;
    private final Executor aiChatExecutionExecutor;
    
    public A2ACommunicationServiceImpl(
            A2APlatformConfig a2aPlatformConfig,
            KunlunProperties kunlunProperties,
            @Qualifier("ai-chat-execution-executor") Executor aiChatExecutionExecutor) {
        this.a2aPlatformConfig = a2aPlatformConfig;
        this.kunlunProperties = kunlunProperties;

        // 配置RestTemplate超时设置
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        // 设置连接和读取超时 - 根据规范设置更长的超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(300000);
        factory.setBufferRequestBody(false);
        
        this.restTemplate.setRequestFactory(factory);

        this.aiChatExecutionExecutor = aiChatExecutionExecutor;
    }




    @Override
    public InputStream sendJsonRpcMessageToA2APlatform(
            String abilityCode, String entityCode, String userId,
            String message, String taskId, String contextId,String satoken) {
        long startTime = System.currentTimeMillis();
        log.info("开始发送JSON-RPC消息到A2A平台 - 能力中心编码: {}, 实体编码: {}, 用户ID: {}, 任务ID: {}, 上下文ID: {}, 消息长度: {}",
                abilityCode, entityCode, userId, taskId, contextId, message != null ? message.length() : 0);

        try {
            // 参数验证
            if (abilityCode == null || abilityCode.trim().isEmpty()) {
                log.warn("能力中心编码为空，使用默认值 - 用户ID: {}", userId);
                abilityCode =kunlunProperties.getAbilityCodes().getDefaultCode();
            }
            if (entityCode == null || entityCode.trim().isEmpty()) {
                log.warn("实体编码为空，使用默认值 - 用户ID: {}", userId);
                entityCode = kunlunProperties.getEntityCodes().getDefaultCode();
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
            headers.set("X-Accel-Buffering", "no");
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
                    log.error("流式消息发送失败 - 用户ID: {}, 状态码: {}, 响应时间: {}ms",
                            userId, response.getStatusCode(), responseTime);

                    // 读取错误响应内容
                    try {
                        InputStream errorStream = response.getBody();
                        String errorContent = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        log.error("A2A平台错误响应内容: {}", errorContent);
                        return new ByteArrayInputStream(("[ERROR] " + errorContent).getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        log.error("读取错误响应内容时发生异常 - 用户ID: {}", userId, e);
                    }

                    String errorResponse = String.format("[ERROR] 流式消息发送失败，状态码: %s，耗时: %dms",
                            response.getStatusCode(), responseTime);
                    return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            };

            // 构建并验证URL
            String a2aPlatformBaseUrl = kunlunProperties.getA2a().getSessionExecution().getUrl();

            String url;
            if (satoken != null && !satoken.isEmpty()) {
                url = String.format("%s/kunlun/a2a/api/%s/entity/%s/userid/%s/satoken/%s",
                        a2aPlatformBaseUrl, abilityCode, entityCode, userId, satoken);
            } else {
                url = String.format("%s/kunlun/a2a/api/%s/entity/%s/userid/%s",
                        a2aPlatformBaseUrl, abilityCode, entityCode, userId);
            }


            if (a2aPlatformBaseUrl == null || a2aPlatformBaseUrl.trim().isEmpty()) {
                log.error("A2A平台基础URL未配置");
                throw new IllegalStateException("A2A平台基础URL未配置");
            }

            if (satoken != null && !satoken.isEmpty()) {
                url = a2aPlatformBaseUrl
                        .replace("{abilityCode}", abilityCode)
                        .replace("{entityCode}", entityCode)
                        .replace("{userId}", userId)
                        .replace("{satoken}", satoken);
            } else {
                url = a2aPlatformBaseUrl
                        .replace("{abilityCode}", abilityCode)
                        .replace("{entityCode}", entityCode)
                        .replace("{userId}", userId)
                        .replace("/{satoken}/{satoken}", ""); // 移除satoken部分
            }

            if (url.trim().isEmpty()) {
                log.error("A2A平台URL解析后为空");
                throw new IllegalStateException("A2A平台URL解析后为空");
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
    public ApiResponse<String> handleSupplementInfo(String userId, String taskId, String userInput, String sessionId) {
        log.info("处理补充信息 - 用户ID: {}, 任务ID: {}, 用户输入: {}, 会话ID: {}", userId, taskId, userInput, sessionId);
        
        try {
            // 使用配置的能力中心编码和实体编码
            String abilityCode = a2aPlatformConfig.getDefaultAbilityCode();
            String entityCode = a2aPlatformConfig.getDefaultEntityCode();

           // 构建请求URL，使用配置的URL
            String url = null;
            /* if (satoken != null && !satoken.isEmpty()) {
                url = kunlunProperties.getA2a().getSupplementInfo().getUrl()
                    .replace("{abilityCode}", abilityCode)
                    .replace("{entityCode}", kunlunProperties.getA2a().getProtocolProxy().getEntityCode()) // 使用协议代理实体编码
                    .replace("{userId}", userId)
                    .replace("{satoken}", satoken);
            } else {
                url = kunlunProperties.getA2a().getSupplementInfo().getUrl()
                    .replace("{abilityCode}", abilityCode)
                    .replace("{entityCode}", kunlunProperties.getA2a().getProtocolProxy().getEntityCode()) // 使用协议代理实体编码
                    .replace("{userId}", userId)
                    .replace("/{satoken}/{satoken}", ""); // 移除satoken部分
            }*/

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

                // 构建请求URL，使用配置的URL
                String url = "";
               /* String satoken = getCurrentSatoken();
                if (satoken != null && !satoken.isEmpty()) {
                    url = kunlunProperties.getA2a().getSupplementInfo().getUrl()
                        .replace("{abilityCode}", abilityCode)
                        .replace("{entityCode}", kunlunProperties.getA2a().getProtocolProxy().getEntityCode()) // 使用协议代理实体编码
                        .replace("{userId}", userId)
                        .replace("{satoken}", satoken);
                } else {
                    url = kunlunProperties.getA2a().getSupplementInfo().getUrl()
                        .replace("{abilityCode}", abilityCode)
                        .replace("{entityCode}", kunlunProperties.getA2a().getProtocolProxy().getEntityCode()) // 使用协议代理实体编码
                        .replace("{userId}", userId)
                        .replace("/{satoken}/{satoken}", ""); // 移除satoken部分
                }
*/
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