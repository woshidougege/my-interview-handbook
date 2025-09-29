package com.noah.superagent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noah.superagent.common.config.KunlunProperties;
import com.noah.superagent.common.dto.request.Attachment;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.service.A2ACommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A2A通信服务实现类
 * 用于处理与上游智平台的Agent-to-Agent通信
 */
@Slf4j
@Service
public class A2ACommunicationServiceImpl implements A2ACommunicationService {

    private final KunlunProperties kunlunProperties;
    private final RestTemplate restTemplate;
    private final Executor aiChatExecutionExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
    
    public A2ACommunicationServiceImpl(
            KunlunProperties kunlunProperties,
            @Qualifier("ai-chat-execution-executor") Executor aiChatExecutionExecutor) {
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
    
    /**
     * 根据文件名获取MIME类型
     * 
     * @param fileName 文件名
     * @return MIME类型，如果找不到则返回application/octet-stream
     */
    private String getMimeType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        
        try {
            // 使用Apache Tika根据文件名检测MIME类型
            String mimeType = mimeTypes.forName(fileName).toString();
            if (mimeType != null && !mimeType.isEmpty()) {
                log.debug("使用Tika根据文件名 {} 检测到 MIME 类型: {}", fileName, mimeType);
                return mimeType;
            }
        } catch (Exception e) {
            log.warn("使用Tika检测MIME类型时发生异常，将使用默认处理: {}", e.getMessage());
        }
        
        // 如果Tika检测失败，使用默认处理方式
        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        // 默认MIME类型映射
        switch (extension) {
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            default:
                return "application/octet-stream"; // 默认二进制流类型
        }
    }

    @Override
    public InputStream sendJsonRpcMessageToA2APlatform(
            String abilityCode, String entityCode, String userId,
            String message, String taskId, String contextId, String satoken, List<Attachment> attachments) {
        long startTime = System.currentTimeMillis();
        log.info("开始发送JSON-RPC格式消息到A2A平台 - 用户ID: {}, 消息: {}", userId, message);

        try {
            // 参数验证
            validateParameters(userId);

            // 获取实际参数值
            String actualAbilityCode = getOrDefault(abilityCode, kunlunProperties.getAbilityCodes().getDefaultCode());
            String actualEntityCode = getOrDefault(entityCode, kunlunProperties.getEntityCodes().getDefaultCode());
            String actualTaskId = getOrDefault(taskId, "task_" + System.currentTimeMillis());
            String actualContextId = getOrDefault(contextId, "");
            String actualSatoken = getOrDefault(satoken, "");

            // 构建JSON-RPC请求体
            String requestBody = buildJsonRpcRequest(message, actualTaskId, actualContextId, attachments);
            log.debug("构建的JSON-RPC请求体: {}", requestBody);

            // 构建请求URL
            String a2aPlatformBaseUrl = kunlunProperties.getA2a().getSessionExecution().getUrl();
            String url = buildRequestUrl(a2aPlatformBaseUrl,actualAbilityCode, actualEntityCode, userId, satoken);
            log.info("准备向A2A平台发送请求 - URL: {}, 方法: POST", url);
            log.debug("完整请求URL: {}", url);

            // 使用低级API直接获取连接，以便立即返回流
            try {
                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                
                // 设置请求方法
                connection.setRequestMethod("POST");
                
                // 设置请求头
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("X-Accel-Buffering", "no");
                connection.setRequestProperty("User-Agent", "SuperAgent/1.0");
                
                // 如果有satoken，添加到请求头
                if (satoken != null && !satoken.isEmpty()) {
                    connection.setRequestProperty("Satoken", satoken);
                }
                
                // 启用输出
                connection.setDoOutput(true);
                
                // 写入请求体
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] requestBytes = requestBody.getBytes(StandardCharsets.UTF_8);
                    log.debug("写入请求体，字节数: {}", requestBytes.length);
                    os.write(requestBytes);
                    os.flush();
                }
                
                // 检查响应码
                int responseCode = connection.getResponseCode();
                long responseTime = System.currentTimeMillis() - startTime;
                log.info("收到A2A平台响应 - 状态码: {}, 响应时间: {}ms", responseCode, responseTime);
                
                if (responseCode >= 200 && responseCode < 300) {
                    log.info("流式消息发送成功 - 用户ID: {}, 响应时间: {}ms", userId, responseTime);
                    log.debug("成功获取响应流，准备开始传输数据 - 用户ID: {}", userId);
                    // 直接返回连接的输入流，实现实时流式传输
                    return connection.getInputStream();
                } else {
                    log.error("流式消息发送失败 - 用户ID: {}, 状态码: {}, 响应时间: {}ms", userId, responseCode, responseTime);
                    
                    // 读取错误响应内容
                    try (InputStream errorStream = connection.getErrorStream()) {
                        if (errorStream != null) {
                            String errorContent = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                            log.error("A2A平台错误响应内容: {}", errorContent);
                            return new ByteArrayInputStream(("[ERROR] " + errorContent).getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        log.error("读取错误响应内容时发生异常 - 用户ID: {}", userId, e);
                    }
                    
                    String errorResponse = String.format("[ERROR] 流式消息发送失败，状态码: %d，耗时: %dms", responseCode, responseTime);
                    return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                log.error("发送JSON-RPC消息时发生异常 - 用户ID: {}, 总耗时: {}ms", userId, totalTime, e);
                String errorResponse = String.format("[ERROR] 发送请求异常: %s，耗时: %dms", e.getMessage(), totalTime);
                return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
            }

        } catch (IllegalArgumentException e) {
            log.error("参数验证失败 - 用户ID: {}, 异常: {}", userId, e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.error("系统状态异常 - 用户ID: {}, 异常: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("发送JSON-RPC消息时发生异常 - 用户ID: {}, 总耗时: {}ms", userId, totalTime, e);
            String errorResponse = String.format("[ERROR] 发送请求异常: %s，耗时: %dms", e.getMessage(), totalTime);
            return new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 参数验证
     */
    private void validateParameters(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.error("用户ID不能为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }
    }
    
    /**
     * 获取默认值
     */
    private String getOrDefault(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
    
    /**
     * 构建JSON-RPC请求
     */
    private String buildJsonRpcRequest(String message, String taskId, String contextId, List<Attachment> attachments) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("jsonrpc", "2.0");
            root.put("method", "message/stream");
            root.put("id", "requestId_" + System.currentTimeMillis());

            ObjectNode params = objectMapper.createObjectNode();
            ObjectNode messageNode = objectMapper.createObjectNode();

            messageNode.put("role", "user");
            messageNode.put("kind", "message");
            messageNode.put("taskId", taskId);
            messageNode.put("messageId", java.util.UUID.randomUUID().toString());

            if (!contextId.isEmpty()) {
                messageNode.put("contextId", contextId);
            }

            ArrayNode parts = objectMapper.createArrayNode();
            
            // 添加文本部分
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("kind", "text");
            textPart.put("text", message != null ? message : "");
            parts.add(textPart);

            // 添加附件部分
            addAttachmentParts(parts, attachments);

            messageNode.set("parts", parts);
            params.set("message", messageNode);
            root.set("params", params);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("构建JSON-RPC请求时发生异常", e);
            throw new RuntimeException("构建JSON-RPC请求失败", e);
        }
    }
    
    /**
     * 添加附件部分到请求中
     */
    private void addAttachmentParts(ArrayNode parts, List<Attachment> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                ObjectNode filePart = objectMapper.createObjectNode();
                filePart.put("kind", "file");
                
                ObjectNode fileNode = objectMapper.createObjectNode();
                // 根据文件名生成MIME类型
                String mimeType = getMimeType(attachment.getName());
                fileNode.put("mimeType", mimeType);
                fileNode.put("name", attachment.getName());
                fileNode.put("uri", attachment.getUri());
                
                filePart.set("file", fileNode);
                parts.add(filePart);
            }
        }
    }

    
    /**
     * 创建HTTP请求头
     */
    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
        headers.set("Connection", "keep-alive");
        headers.set("Cache-Control", "no-cache");
        headers.set("X-Accel-Buffering", "no");
        headers.set("User-Agent", "SuperAgent/1.0");
        return headers;
    }
    
    /**
     * 创建请求回调
     */
    private RequestCallback createRequestCallback(HttpHeaders headers, String requestBody, String userId) {
        return clientHttpRequest -> {
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
    }
    
    /**
     * 创建响应提取器
     */
    private ResponseExtractor<InputStream> createResponseExtractor(String userId, long startTime) {
        return response -> {
            long responseTime = System.currentTimeMillis() - startTime;
            log.info("收到A2A平台响应 - 状态码: {}, 响应时间: {}ms", response.getStatusCode(), responseTime);
            log.debug("A2A平台响应头: {}", response.getHeaders());

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("流式消息发送成功 - 用户ID: {}, 响应时间: {}ms", userId, responseTime);
                InputStream body = response.getBody();
                log.debug("成功获取响应流，准备开始传输数据 - 用户ID: {}", userId);
                // 直接返回原始输入流，实现实时流式传输
                return body;
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
    }

    /**
     * 构建补充信息的请求URL
     * 使用A2A协议代理实体编码，而不是默认实体编码
     */
    private String buildRequestUrl(String a2aPlatformBaseUrl,String abilityCode, String entityCode, String userId, String satoken) {
        
        if (a2aPlatformBaseUrl == null || a2aPlatformBaseUrl.trim().isEmpty()) {
            log.error("A2A平台补充信息URL未配置");
            throw new IllegalStateException("A2A平台补充信息URL未配置");
        }

        String url;
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
            log.error("A2A平台补充信息URL解析后为空");
            throw new IllegalStateException("A2A平台补充信息URL解析后为空");
        }
        
        return url;
    }

    /**
     * 构建补充信息的JSON-RPC请求
     */
    private String buildSupplementJsonRpcRequest(String taskId, String contextId, String userInput) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("jsonrpc", "2.0");
            root.put("method", "message/send");
            root.put("id", "supplementId_" + System.currentTimeMillis());

            ObjectNode params = objectMapper.createObjectNode();
            ObjectNode messageNode = objectMapper.createObjectNode();

            messageNode.put("role", "user");
            messageNode.put("kind", "message");
            messageNode.put("taskId", taskId);
            messageNode.put("messageId", java.util.UUID.randomUUID().toString());
            
            if (contextId != null && !contextId.isEmpty()) {
                messageNode.put("contextId", contextId);
            }

            ArrayNode parts = objectMapper.createArrayNode();
            
            // 始终使用"data"字段并包含表单信息
            ObjectNode dataPart = objectMapper.createObjectNode();
            dataPart.put("kind", "data");
            
            if (userInput != null && !userInput.isEmpty()) {
                try {
                    // 尝试解析用户输入为JSON对象
                    JsonNode userData = objectMapper.readTree(userInput);
                    if (userData.has("kind") && "data".equals(userData.get("kind").asText()) && userData.has("data")) {
                        // 如果已经符合规范格式，直接使用data字段
                        dataPart.set("data", userData.get("data"));
                    } else {
                        // 否则将整个对象作为data字段
                        dataPart.set("data", userData);
                    }
                } catch (Exception e) {
                    // 如果不是有效的JSON，直接使用原始字符串作为data值
                    dataPart.put("data", userInput);
                }
            } else {
                // 如果没有用户输入，创建一个空的data数组
                dataPart.set("data", objectMapper.createArrayNode());
            }
            
            parts.add(dataPart);

            messageNode.set("parts", parts);
            params.set("message", messageNode);
            root.set("params", params);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("构建补充信息JSON-RPC请求时发生异常", e);
            throw new RuntimeException("构建补充信息请求失败", e);
        }
    }

    @Override
    public ApiResponse<String> handleSupplementInfo(String userId, String taskId, String userInput, String sessionId,String A2AProtocolProxyEntityCode,String satoken) {
        log.info("处理补充信息 - 用户ID: {}, 任务ID: {}, 用户输入: {}, 会话ID: {}", userId, taskId, userInput, sessionId);
        
        try {
            // 使用配置的能力中心编码和A2A协议代理实体编码
            String abilityCode = kunlunProperties.getAbilityCodes().getDefaultCode();

            // 构建补充信息请求URL - 使用补充信息接口
            String a2aPlatformBaseUrl = kunlunProperties.getA2a().getSupplementInfo().getUrl();
            String url = buildRequestUrl(a2aPlatformBaseUrl,abilityCode, A2AProtocolProxyEntityCode, userId, satoken);
            log.info("准备发送补充信息 - URL: {}", url);

            // 构建JSON-RPC格式的补充信息请求
            String requestBody = buildSupplementJsonRpcRequest(taskId, sessionId, userInput);
            log.debug("构建的补充信息请求体: {}", requestBody);

            // 设置请求头
            HttpHeaders headers = createHttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // 检查响应状态码
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("补充信息处理成功 - 用户ID: {}, 状态码: {}", userId, response.getStatusCode());
                // 不返回实际的响应体，而是返回自定义的成功消息
                return ApiResponse.success("补充信息已成功提交");
            } else {
                log.error("补充信息处理失败 - 用户ID: {}, 状态码: {}, 响应体: {}", 
                         userId, response.getStatusCode(), response.getBody());
                return ApiResponse.error("补充信息处理失败，状态码: " + response.getStatusCode() + 
                                       (response.getBody() != null ? ", 响应内容: " + response.getBody() : ""));
            }
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("处理补充信息时A2A平台发生内部服务器错误 - 用户ID: {}, 状态码: {}, 响应内容: {}", 
                     userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ApiResponse.error("A2A平台处理失败，请稍后重试");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("处理补充信息时向A2A平台发送请求出现客户端错误 - 用户ID: {}, 状态码: {}, 响应内容: {}", 
                     userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ApiResponse.error("请求参数有误: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理补充信息时发生异常 - 用户ID: {}", userId, e);
            return ApiResponse.error("补充信息处理失败: " + e.getMessage());
        }
    }

    public CompletableFuture<ApiResponse<String>> handleSupplementInfoAsync(String userId, String taskId, String userInput, String sessionId,String A2aProtocolProxyEntityCode) {
        log.info("异步处理补充信息 - 用户ID: {}, 任务ID: {}, 用户输入: {}, 会话ID: {}", userId, taskId, userInput, sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 使用配置的能力中心编码和A2A协议代理实体编码
                String abilityCode = kunlunProperties.getAbilityCodes().getDefaultCode();

                // 构建补充信息请求URL - 使用补充信息接口
                String a2aPlatformBaseUrl = kunlunProperties.getA2a().getSupplementInfo().getUrl();
                String url = buildRequestUrl(a2aPlatformBaseUrl,abilityCode, A2aProtocolProxyEntityCode, userId, null);
                log.info("准备异步发送补充信息 - URL: {}", url);

                // 构建JSON-RPC格式的补充信息请求
                String requestBody = buildSupplementJsonRpcRequest(taskId, sessionId, userInput);
                log.debug("构建的异步补充信息请求体: {}", requestBody);

                // 设置请求头
                HttpHeaders headers = createHttpHeaders();
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