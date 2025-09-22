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
 * @author 任相鹏
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
    public void streamChat(List<ChatMessage> messages, Consumer<String> resultCallback, Runnable completeCallback) {
        log.info("开始流式聊天，消息数量: {}", messages != null ? messages.size() : 0);
        
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        if (!dashscopeConfig.getEnabled() || !StringUtils.hasText(dashscopeConfig.getApiKey())) {
            log.warn("AI服务未启用或API Key未配置，无法进行流式聊天");
            resultCallback.accept("AI服务暂时不可用，请稍后再试。");
            completeCallback.run();
            return;
        }
        
        if (messages == null || messages.isEmpty()) {
            log.warn("消息列表为空");
            resultCallback.accept("消息不能为空。");
            completeCallback.run();
            return;
        }
        
        try {
            callDashscopeStreamSDK(messages, resultCallback, completeCallback);
        } catch (Exception e) {
            log.error("调用阿里云百炼流式SDK失败", e);
            resultCallback.accept("抱歉，AI服务暂时出现问题，请稍后再试。");
            completeCallback.run();
        }
    }
    
    /**
     * 调用DashScope SDK进行流式聊天
     */
    private void callDashscopeStreamSDK(List<ChatMessage> chatMessages, Consumer<String> resultCallback, Runnable completeCallback) 
            throws ApiException, NoApiKeyException, InputRequiredException {
        
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        
        // 转换消息格式
        List<Message> messages = chatMessages.stream()
                .map(msg -> Message.builder()
                        .role(getRoleValue(msg.getRole()))
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());
        
        // 构建参数 - 按照官方文档的最佳实践
        GenerationParam param = GenerationParam.builder()
                .apiKey(dashscopeConfig.getApiKey())
                .model("qwen-plus") // 使用qwen-plus模型进行聊天
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true) // 关键：开启增量输出，真正的流式返回
                .maxTokens(2000) // 添加token限制，避免长时间等待
                .temperature(0.7f) // 添加温度参数，提高响应性
                .build();
        
        log.debug("调用DashScope SDK进行流式聊天，模型: qwen-plus");
        
        Generation gen = new Generation();
        
        try {
            // 🔧 关键修复：简化线程模型，避免缓冲延迟
            Flowable<GenerationResult> result = gen.streamCall(param);
            
            result
                // 🚀 重要：直接在IO线程处理，避免线程切换导致的缓冲延迟
                .subscribeOn(Schedulers.io()) 
                // 移除 observeOn，避免线程切换缓冲
                .subscribe(
                    // onNext: 处理每个响应片段 - 立即处理，无缓冲
                    message -> {
                        try {
                            // 获取增量内容
                            String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                            if (StringUtils.hasText(content)) {
                                long currentTime = System.currentTimeMillis();
                                log.info("🔥 DashScope增量数据: [{}] 长度: {} 时间: {}", 
                                    content.length() > 50 ? content.substring(0, 50) + "..." : content, 
                                    content.length(), 
                                    currentTime);
                                
                                // 🚀 立即回调，但要处理连接断开的情况
                                try {
                                    resultCallback.accept(content);
                                } catch (Exception callbackException) {
                                    // 如果回调失败（如连接断开），记录并停止处理
                                    log.warn("流式数据回调失败，可能是连接断开: {}", callbackException.getMessage());
                                    return; // 停止处理后续数据
                                }
                                
                                // 强制刷新，确保立即发送
                                Thread.yield(); // 让其他线程有机会处理
                            }
                            
                            // 检查是否完成
                            String finishReason = message.getOutput().getChoices().get(0).getFinishReason();
                            if (finishReason != null && !"null".equals(finishReason)) {
                                log.info("DashScope流式调用完成，finishReason: {}", finishReason);
                                if (message.getUsage() != null) {
                                    log.info("Token使用情况 - 输入: {}, 输出: {}, 总计: {}", 
                                        message.getUsage().getInputTokens(),
                                        message.getUsage().getOutputTokens(), 
                                        message.getUsage().getTotalTokens());
                                }
                            }
                        } catch (Exception e) {
                            log.error("处理流式响应片段失败", e);
                        }
                    },
                    // onError: 处理错误
                    error -> {
                        try {
                            log.error("DashScope流式聊天请求失败", error);
                            resultCallback.accept("\n\n抱歉，AI回答过程中出现问题，请稍后再试。");
                        } catch (Exception e) {
                            log.error("处理流式聊天错误失败", e);
                        } finally {
                            completeCallback.run();
                        }
                    },
                    // onComplete: 完成回调
                    () -> {
                        try {
                            log.info("DashScope流式聊天完成");
                            completeCallback.run();
                        } catch (Exception e) {
                            // 完成回调失败通常是因为连接断开，不需要error级别
                            log.warn("处理流式聊天完成回调失败，可能是连接断开: {}", e.getMessage());
                        }
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
