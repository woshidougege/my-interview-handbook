package com.noah.superagent.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.service.AiService;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AI服务实现
 * 基于阿里云百炼SDK实现AI功能，包括标题生成和对话功能，使用统一的配置类管理
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
        boolean enabled = aiProperties.getEnabled() && aiProperties.getFeatures().getTitleGeneration().getEnabled();
        boolean apiKeyConfigured = aiProperties.getApi().getApiKey() != null && !aiProperties.getApi().getApiKey().isEmpty();
        
        log.info("AI标题生成服务初始化 - 启用状态: {}, API密钥配置: {}", enabled, apiKeyConfigured ? "已配置" : "未配置");
        
        if (!apiKeyConfigured) {
            log.warn("DashScope API Key未配置，请设置环境变量ALICLOUD_AI_API_KEY");
        } else {
            log.info("AI标题生成服务配置完成 - 模型: {}, 温度: {}, 最大Token: {}", 
                    aiProperties.getModels().getTitleGeneration().getModel(),
                    aiProperties.getModels().getTitleGeneration().getParameters().getTemperature(),
                    aiProperties.getModels().getTitleGeneration().getParameters().getMaxTokens());
        }
    }

    @Override
    public String generateChatTitle(String question) {
        if (!isServiceAvailable()) {
            log.warn("AI服务不可用，返回默认标题");
            return aiProperties.getFeatures().getTitleGeneration().getDefaultTitle();
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
            return aiProperties.getFeatures().getTitleGeneration().getDefaultTitle();
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
        return aiProperties.getEnabled() && 
               aiProperties.getApi().getApiKey() != null && 
               !aiProperties.getApi().getApiKey().trim().isEmpty() &&
               aiProperties.getApi().getEndpoint() != null &&
               !aiProperties.getApi().getEndpoint().trim().isEmpty();
    }

    // ========== 对话功能实现 ==========

    @Override
    public String getAiResponse(String message, String workspaceId, String sessionId) {
        if (!isServiceAvailable() || !aiProperties.getFeatures().getChat().getEnabled()) {
            return "AI服务暂不可用，请稍后再试。";
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            String response = callDashScopeApiForChat(message, false, null, null, null);
            long duration = System.currentTimeMillis() - startTime;
            log.info("AI对话成功 - 工作空间: {}, 会话: {}, 耗时: {}ms, 响应长度: {}", 
                    workspaceId, sessionId, duration, response.length());
            return response;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("AI对话失败 - 工作空间: {}, 会话: {}, 耗时: {}ms, 错误: {}", 
                     workspaceId, sessionId, duration, e.getMessage(), e);
            return "抱歉，AI服务遇到了问题，请稍后再试。";
        }
    }

    @Override
    public void getAiResponseStream(String message, String workspaceId, String sessionId,
                                   Consumer<String> onChunk, Runnable onComplete, Consumer<String> onError) {
        
        log.info("开始AI流式对话 - 工作空间: {}, 会话: {}, 消息长度: {}", workspaceId, sessionId, message.length());
        
        if (!isServiceAvailable() || !aiProperties.getFeatures().getChat().getEnabled()) {
            log.warn("AI对话服务不可用 - 工作空间: {}, 会话: {}", workspaceId, sessionId);
            if (onError != null) {
                onError.accept("AI对话服务暂不可用，请稍后再试。");
            }
            return;
        }
        
        // 检查API Key配置
        String apiKey = aiProperties.getApi().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("DashScope API Key未配置 - 工作空间: {}, 会话: {}", workspaceId, sessionId);
            if (onError != null) {
                onError.accept("AI服务配置错误，请联系管理员。");
            }
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                log.debug("调用DashScope流式API - 模型: {}, 温度: {}", 
                         aiProperties.getModels().getChat().getModel(),
                         aiProperties.getModels().getChat().getParameters().getTemperature());
                         
                callDashScopeApiForChat(message, true, onChunk, onComplete, onError);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("AI流式对话完成 - 工作空间: {}, 会话: {}, 耗时: {}ms", 
                        workspaceId, sessionId, duration);
                        
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("AI流式对话失败 - 工作空间: {}, 会话: {}, 耗时: {}ms, 错误类型: {}, 错误信息: {}", 
                         workspaceId, sessionId, duration, e.getClass().getSimpleName(), e.getMessage(), e);
                if (onError != null) {
                    onError.accept("AI服务遇到了问题：" + e.getMessage());
                }
            }
        }).exceptionally(throwable -> {
            log.error("AI流式对话异步执行失败 - 工作空间: {}, 会话: {}, 错误: {}", 
                     workspaceId, sessionId, throwable.getMessage(), throwable);
            if (onError != null) {
                onError.accept("AI服务异步执行失败：" + throwable.getMessage());
            }
            return null;
        });
    }

    /**
     * 调用阿里云百炼SDK生成标题
     */
    private String callDashScopeApi(String question) {
        try {
            // 构建提示词
            String prompt = aiProperties.getFeatures().getTitleGeneration().getPromptTemplate().replace("{question}", question);
            
            // 构建消息
            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();
            
            // 构建生成参数
            GenerationParam param = GenerationParam.builder()
                    .apiKey(aiProperties.getApi().getApiKey())  // 显式设置API key
                    .model(aiProperties.getModels().getTitleGeneration().getModel())
                    .messages(Collections.singletonList(userMessage))
                    .temperature(aiProperties.getModels().getTitleGeneration().getParameters().getTemperature().floatValue())
                    .topP(aiProperties.getModels().getTitleGeneration().getParameters().getTopP())
                    .maxTokens(aiProperties.getModels().getTitleGeneration().getParameters().getMaxTokens())
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)  // 设置结果格式
                    .build();
            
            // 调用生成接口
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            // 提取生成的标题
            String generatedText = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            // 清理和截断标题
            String title = generatedText.trim();
            if (title.length() > aiProperties.getFeatures().getTitleGeneration().getMaxLength()) {
                title = title.substring(0, aiProperties.getFeatures().getTitleGeneration().getMaxLength());
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

    /**
     * 调用阿里云百炼SDK进行对话（增强版本，包含详细日志）
     */
    private String callDashScopeApiForChat(String message, boolean stream,
                                           Consumer<String> onChunk, Runnable onComplete, Consumer<String> onError) {
        try {
            log.debug("构建DashScope API对话请求参数...");
            Generation gen = new Generation();
            
            // 构建消息列表
            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(aiProperties.getFeatures().getChat().getSystemPrompt())
                    .build();
            
            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(message)
                    .build();
            
            log.debug("系统提示词: {}", aiProperties.getFeatures().getChat().getSystemPrompt());
            log.debug("用户消息: {}", message);
            
            // 构建生成参数（参考官方示例）
            if (stream && aiProperties.getFeatures().getChat().getStreamEnabled()) {
                log.info("使用流式输出模式");
                
                // 流式输出参数构建（参考官方示例）
                GenerationParam param = GenerationParam.builder()
                        .apiKey(aiProperties.getApi().getApiKey())
                        .model(aiProperties.getModels().getChat().getModel())
                        .messages(Arrays.asList(systemMessage, userMessage))
                        .temperature(aiProperties.getModels().getChat().getParameters().getTemperature().floatValue())
                        .topP(aiProperties.getModels().getChat().getParameters().getTopP())
                        .maxTokens(aiProperties.getModels().getChat().getParameters().getMaxTokens())
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(true)  // 关键：启用增量输出
                        .build();
                
                log.debug("开始调用DashScope流式API...");
                Flowable<GenerationResult> resultFlowable = gen.streamCall(param);
                
                // 使用类似官方示例的处理方式
                final int[] chunkCount = {0}; // 使用数组来在lambda中修改值
                try {
                    resultFlowable.blockingForEach(result -> {
                        log.trace("收到流式结果: {}", result);
                        
                        if (result != null && result.getOutput() != null && 
                            result.getOutput().getChoices() != null && 
                            !result.getOutput().getChoices().isEmpty()) {
                            
                            String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                            if (content != null && !content.isEmpty()) {
                                chunkCount[0]++;
                                log.debug("流式内容块 #{}: {}", chunkCount[0], content);
                                if (onChunk != null) {
                                    onChunk.accept(content);
                                }
                            }
                        }
                    });
                    
                    log.info("流式调用完成，共收到 {} 个数据块", chunkCount[0]);
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    
                    return "STREAM_COMPLETE";
                    
                } catch (Exception streamException) {
                    log.error("流式处理过程中出现异常: {}", streamException.getMessage(), streamException);
                    if (onError != null) {
                        onError.accept("流式处理失败: " + streamException.getMessage());
                    }
                    throw streamException;
                }
                
            } else {
                log.info("使用非流式输出模式");
                
                // 非流式输出
                GenerationParam param = GenerationParam.builder()
                        .apiKey(aiProperties.getApi().getApiKey())
                        .model(aiProperties.getModels().getChat().getModel())
                        .messages(Arrays.asList(systemMessage, userMessage))
                        .temperature(aiProperties.getModels().getChat().getParameters().getTemperature().floatValue())
                        .topP(aiProperties.getModels().getChat().getParameters().getTopP())
                        .maxTokens(aiProperties.getModels().getChat().getParameters().getMaxTokens())
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .build();
                        
                GenerationResult result = gen.call(param);
                
                if (result.getOutput() != null && result.getOutput().getChoices() != null && !result.getOutput().getChoices().isEmpty()) {
                    String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                    log.debug("从DashScope API获取对话回复，长度: {}", content.length());
                    return content;
                } else {
                    log.warn("AI对话回复为空");
                    return "抱歉，我现在无法回答这个问题，请稍后再试。";
                }
            }
            
        } catch (NoApiKeyException e) {
            log.error("API密钥未配置或无效: {}", e.getMessage());
            String errorMsg = "AI服务配置有误，请联系管理员检查API密钥。";
            if (onError != null) {
                onError.accept(errorMsg);
            }
            throw new RuntimeException(errorMsg, e);
            
        } catch (InputRequiredException e) {
            log.error("请求参数不完整: {}", e.getMessage());
            String errorMsg = "请求参数有误，请重新发送消息。";
            if (onError != null) {
                onError.accept(errorMsg);
            }
            throw new RuntimeException(errorMsg, e);
            
        } catch (ApiException e) {
            log.error("API调用异常，状态码: {}, 错误信息: {}", e.getStatus().getStatusCode(), e.getMessage());
            String errorMsg = "AI服务调用失败，请稍后再试。状态码: " + e.getStatus().getStatusCode();
            if (onError != null) {
                onError.accept(errorMsg);
            }
            throw new RuntimeException(errorMsg, e);
            
        } catch (Exception e) {
            log.error("调用DashScope API失败: {}", e.getMessage(), e);
            String errorMsg = "AI服务暂时不可用，请稍后再试。";
            if (onError != null) {
                onError.accept(errorMsg);
            }
            throw new RuntimeException(errorMsg, e);
        }
    }
}
