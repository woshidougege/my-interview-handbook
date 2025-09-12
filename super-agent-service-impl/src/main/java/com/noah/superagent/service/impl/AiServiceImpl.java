package com.noah.superagent.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.noah.superagent.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * AI服务实现
 * 基于阿里云百炼SDK实现AI功能
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Value("${alicloud.ai.enabled:true}")
    private Boolean aiEnabled;

    @Value("${alicloud.ai.api.api-key:}")
    private String apiKey;

    @Value("${alicloud.ai.api.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String endpoint;

    @Value("${alicloud.ai.models.title-generation.model:qwen-turbo}")
    private String titleModel;

    @Value("${alicloud.ai.models.title-generation.parameters.temperature:0.5}")
    private Double titleTemperature;

    @Value("${alicloud.ai.models.title-generation.parameters.top-p:0.9}")
    private Double titleTopP;

    @Value("${alicloud.ai.models.title-generation.parameters.max-tokens:50}")
    private Integer titleMaxTokens;

    @Value("${alicloud.ai.features.title-generation.enabled:true}")
    private Boolean titleGenerationEnabled;

    @Value("${alicloud.ai.features.title-generation.prompt-template}")
    private String promptTemplate;

    @Value("${alicloud.ai.features.title-generation.default-title:新对话}")
    private String defaultTitle;

    @Value("${alicloud.ai.features.title-generation.max-length:20}")
    private Integer titleMaxLength;

    @PostConstruct
    public void init() {
        log.info("AI服务初始化 - 启用状态: {}, API密钥配置: {}", 
                aiEnabled, apiKey != null && !apiKey.isEmpty() ? "已配置" : "未配置");
        
        // 初始化DashScope API Key
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            System.setProperty("DASHSCOPE_API_KEY", apiKey);
            log.info("DashScope API Key已设置");
        } else {
            log.warn("DashScope API Key未配置，请设置环境变量ALICLOUD_AI_API_KEY");
        }
    }

    @Override
    public String generateChatTitle(String question) {
        if (!isServiceAvailable()) {
            log.warn("AI服务不可用，返回默认标题");
            return defaultTitle;
        }

        long startTime = System.currentTimeMillis();
        
        try {
            String title = callDashScopeApi(question);
            long duration = System.currentTimeMillis() - startTime;
            log.info("标题生成成功，耗时: {}ms，标题: {}", duration, title);
            return title;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("标题生成失败，耗时: {}ms，错误: {}", duration, e.getMessage(), e);
            return defaultTitle;
        }
    }

    @Override
    @Async("ai-service-executor")
    public void generateChatTitleAsync(String question, TitleGenerationCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String title = generateChatTitle(question);
                callback.onSuccess(title);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    @Override
    public boolean isServiceAvailable() {
        return aiEnabled && 
               titleGenerationEnabled && 
               apiKey != null && 
               !apiKey.trim().isEmpty() &&
               endpoint != null &&
               !endpoint.trim().isEmpty();
    }

    /**
     * 调用阿里云百炼SDK生成标题
     */
    private String callDashScopeApi(String question) {
        try {
            // 构建提示词
            String prompt = promptTemplate.replace("{question}", question);
            
            // 构建消息
            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();
            
            // 构建生成参数
            GenerationParam param = GenerationParam.builder()
                    .model(titleModel)
                    .messages(Collections.singletonList(userMessage))
                    .temperature(titleTemperature.floatValue())
                    .topP(titleTopP)
                    .maxTokens(titleMaxTokens)
                    .build();
            
            // 调用生成接口
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            // 提取生成的标题
            String generatedText = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            // 清理和截断标题
            String title = generatedText.trim();
            if (title.length() > titleMaxLength) {
                title = title.substring(0, titleMaxLength);
            }
            
            log.debug("从DashScope API获取标题: {}", title);
            return title;
            
        } catch (NoApiKeyException e) {
            log.error("API密钥未配置或无效: {}", e.getMessage());
            throw new RuntimeException("API密钥未配置或无效: " + e.getMessage());
        } catch (InputRequiredException e) {
            log.error("请求参数缺失: {}", e.getMessage());
            throw new RuntimeException("请求参数缺失: " + e.getMessage());
        } catch (ApiException e) {
            log.error("API调用异常，状态码: {}, 错误信息: {}", e.getStatus().getStatusCode(), e.getMessage());
            throw new RuntimeException("API调用异常: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用DashScope API失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用DashScope API失败: " + e.getMessage());
        }
    }
}
