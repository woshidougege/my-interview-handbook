package com.noah.superagent.ai.service.impl;

import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * AI服务实现类
 * 基于阿里云百炼大模型提供AI功能
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    
    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        // 根据配置初始化HTTP客户端
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        AiProperties.HttpConfig httpConfig = dashscopeConfig.getHttp();
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(httpConfig.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(httpConfig.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(httpConfig.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
                
        log.info("AI服务初始化完成 - 基础URL: {}, 模型: {}", 
                dashscopeConfig.getBaseUrl(), 
                dashscopeConfig.getTitleGeneration().getModel());
    }

    @Override
    public String generateChatTitle(String question) {
        log.info("开始生成聊天标题，问题长度: {}", question != null ? question.length() : 0);
        
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        
        // 检查服务是否启用
        if (!dashscopeConfig.getEnabled()) {
            log.warn("AI服务未启用，返回默认标题");
            return getDefaultTitle(question);
        }
        
        // 检查API Key
        if (!StringUtils.hasText(dashscopeConfig.getApiKey())) {
            log.warn("API Key未配置，返回默认标题");
            return getDefaultTitle(question);
        }
        
        // 检查问题内容
        if (!StringUtils.hasText(question)) {
            return "新对话";
        }
        
        try {
            return callDashscopeApi(question.trim());
        } catch (Exception e) {
            log.error("调用阿里云百炼大模型失败", e);
            return getDefaultTitle(question);
        }
    }
    
    /**
     * 调用阿里云百炼大模型API
     */
    private String callDashscopeApi(String question) throws IOException {
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        AiProperties.TitleGenerationConfig titleConfig = dashscopeConfig.getTitleGeneration();
        
        // 构建提示词
        String prompt = titleConfig.getPromptTemplate().replace("{question}", question);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", titleConfig.getModel());
        requestBody.put("max_tokens", titleConfig.getMaxTokens());
        requestBody.put("temperature", titleConfig.getTemperature());
        
        // 构建消息列表
        Map<String, String> messageContent = new HashMap<>();
        messageContent.put("role", "user");
        messageContent.put("content", prompt);
        requestBody.put("messages", List.of(messageContent));
        
        // 序列化请求体
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        // 构建HTTP请求
        Request request = new Request.Builder()
                .url(dashscopeConfig.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + dashscopeConfig.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();
        
        log.debug("发送请求到阿里云百炼: {}", dashscopeConfig.getBaseUrl());
        
        // 发送请求并处理响应
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("API调用失败，状态码: {}, 响应: {}", response.code(), response.body() != null ? response.body().string() : "无响应体");
                throw new IOException("API调用失败: " + response.code());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }
            
            String responseText = responseBody.string();
            log.debug("API响应: {}", responseText);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(responseText);
            JsonNode choices = responseJson.get("choices");
            
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new IOException("响应格式错误：choices为空");
            }
            
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            
            if (message == null) {
                throw new IOException("响应格式错误：message为空");
            }
            
            JsonNode content = message.get("content");
            if (content == null) {
                throw new IOException("响应格式错误：content为空");
            }
            
            String generatedTitle = content.asText().trim();
            
            // 清理生成的标题
            generatedTitle = cleanTitle(generatedTitle);
            
            log.info("成功生成标题: {}", generatedTitle);
            return generatedTitle;
        }
    }
    
    /**
     * 清理生成的标题
     */
    private String cleanTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "新对话";
        }
        
        // 移除多余的引号和换行符
        title = title.replaceAll("[\"'\\n\\r\\t]", "").trim();
        
        // 限制长度
        if (title.length() > 20) {
            title = title.substring(0, 17) + "...";
        }
        
        return title.isEmpty() ? "新对话" : title;
    }
    
    /**
     * 获取默认标题（降级方案）
     */
    private String getDefaultTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return "新对话";
        }
        
        String cleanText = question.trim();
        
        // 简单截取逻辑
        if (cleanText.length() > 10) {
            return cleanText.substring(0, 10) + "...";
        }
        
        return cleanText.isEmpty() ? "新对话" : cleanText;
    }
}
