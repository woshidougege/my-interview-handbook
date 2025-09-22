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
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    
    @Override
    public void streamChat(List<ChatMessage> messages, Consumer<String> resultCallback) {
        log.info("开始流式聊天，消息数量: {}", messages != null ? messages.size() : 0);
        
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        if (!dashscopeConfig.getEnabled() || !StringUtils.hasText(dashscopeConfig.getApiKey())) {
            log.warn("AI服务未启用或API Key未配置，无法进行流式聊天");
            resultCallback.accept("AI服务暂时不可用，请稍后再试。");
            return;
        }
        
        if (messages == null || messages.isEmpty()) {
            log.warn("消息列表为空");
            resultCallback.accept("消息不能为空。");
            return;
        }
        
        try {
            callDashscopeStreamSDK(messages, resultCallback);
        } catch (Exception e) {
            log.error("调用阿里云百炼流式SDK失败", e);
            resultCallback.accept("抱歉，AI服务暂时出现问题，请稍后再试。");
        }
    }
    
    /**
     * 调用DashScope SDK进行流式聊天
     */
    private void callDashscopeStreamSDK(List<ChatMessage> chatMessages, Consumer<String> resultCallback) 
            throws ApiException, NoApiKeyException, InputRequiredException {
        
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        
        // 转换消息格式
        List<Message> messages = chatMessages.stream()
                .map(msg -> Message.builder()
                        .role(getRoleValue(msg.getRole()))
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());
        
        // 构建参数
        GenerationParam param = GenerationParam.builder()
                .apiKey(dashscopeConfig.getApiKey())
                .model("qwen-plus") // 使用qwen-plus模型进行聊天
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true) // 开启增量输出，流式返回
                .build();
        
        log.debug("调用DashScope SDK进行流式聊天，模型: qwen-plus");
        
        Generation gen = new Generation();
        
        try {
            // 使用streamCall进行流式调用
            Flowable<GenerationResult> result = gen.streamCall(param);
            
            result
                .subscribeOn(Schedulers.io()) // IO线程执行请求
                .observeOn(Schedulers.computation()) // 计算线程处理响应
                .subscribe(
                    // onNext: 处理每个响应片段
                    message -> {
                        String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                        if (StringUtils.hasText(content)) {
                            resultCallback.accept(content);
                        }
                    },
                    // onError: 处理错误
                    error -> {
                        log.error("流式聊天请求失败", error);
                        resultCallback.accept("\n\n抱歉，AI回答过程中出现问题，请稍后再试。");
                    },
                    // onComplete: 完成回调
                    () -> {
                        log.debug("流式聊天完成");
                        // 流式结束，不需要额外操作
                    }
                );
        } catch (Exception e) {
            log.error("流式聊天调用异常", e);
            throw new RuntimeException("流式聊天调用失败", e);
        }
    }
    
    /**
     * 获取角色值
     */
    private String getRoleValue(String role) {
        if ("user".equals(role)) {
            return Role.USER.getValue();
        } else if ("assistant".equals(role)) {
            return Role.ASSISTANT.getValue();
        } else if ("system".equals(role)) {
            return Role.SYSTEM.getValue();
        }
        return Role.USER.getValue(); // 默认为用户角色
    }
}
