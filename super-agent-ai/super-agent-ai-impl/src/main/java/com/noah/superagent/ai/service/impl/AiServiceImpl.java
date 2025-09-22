package com.noah.superagent.ai.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * AI服务实现类
 * 基于阿里云百炼DashScope SDK提供AI功能
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProperties aiProperties;

    @PostConstruct
    public void init() {
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        
        // 检查API Key配置
        if (!StringUtils.hasText(dashscopeConfig.getApiKey())) {
            log.warn("阿里云百炼API Key未配置，AI服务将无法正常工作");
            return;
        }
        
        log.info("AI服务初始化完成 - 模型: {}", 
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
            return callDashscopeSDK(question.trim());
        } catch (Exception e) {
            log.error("调用阿里云百炼SDK失败", e);
            return getDefaultTitle(question);
        }
    }
    
    /**
     * 调用阿里云百炼DashScope SDK
     */
    private String callDashscopeSDK(String question) throws ApiException, NoApiKeyException, InputRequiredException {
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        AiProperties.TitleGenerationConfig titleConfig = dashscopeConfig.getTitleGeneration();
        
        // 构建消息 - 标准对话模式
        // System: 完整的角色定义和指令
        Message systemMessage = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(titleConfig.getSystemPrompt())
                .build();
                
        // User: 直接是用户的问题
        Message userMessage = Message.builder()
                .role(Role.USER.getValue())
                .content(question)
                .build();
        
        // 构建生成参数 - 按照官方示例添加resultFormat
        GenerationParam param = GenerationParam.builder()
                .apiKey(dashscopeConfig.getApiKey())
                .model(titleConfig.getModel())
                .messages(Arrays.asList(systemMessage, userMessage))
                .maxTokens(titleConfig.getMaxTokens())
                .temperature(titleConfig.getTemperature().floatValue())
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        
        log.debug("调用DashScope SDK生成标题，模型: {}", titleConfig.getModel());
        
        // 调用SDK
        Generation gen = new Generation();
        GenerationResult result = gen.call(param);
        
        if (result == null || result.getOutput() == null || result.getOutput().getChoices() == null 
            || result.getOutput().getChoices().isEmpty()) {
            log.error("DashScope SDK响应异常: result={}", result);
            throw new RuntimeException("SDK响应为空或格式错误");
        }
        
        String generatedTitle = result.getOutput().getChoices().get(0).getMessage().getContent().trim();
        
        // 清理生成的标题
        generatedTitle = cleanTitle(generatedTitle);
        
        log.info("成功生成标题: {}", generatedTitle);
        return generatedTitle;
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
