package com.noah.superagent.service;

import java.util.function.Consumer;

/**
 * AI服务接口
 * 提供AI相关功能的统一服务接口，包括标题生成和对话功能
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public interface AiService {

    // ========== 标题生成功能 ==========
    
    /**
     * 根据用户问题生成会话标题
     *
     * @param question 用户的问题
     * @return 生成的标题
     */
    String generateChatTitle(String question);

    /**
     * 根据用户问题生成会话标题（异步）
     *
     * @param question 用户的问题
     * @param callback 回调函数
     */
    void generateChatTitleAsync(String question, TitleGenerationCallback callback);

    // ========== 对话功能 ==========
    
    /**
     * 同步获取AI回复
     *
     * @param message 用户消息
     * @param workspaceId 工作空间ID
     * @param sessionId 会话ID
     * @return AI回复内容
     */
    String getAiResponse(String message, String workspaceId, String sessionId);

    /**
     * 流式获取AI回复
     *
     * @param message 用户消息
     * @param workspaceId 工作空间ID
     * @param sessionId 会话ID
     * @param onChunk 流式数据回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    void getAiResponseStream(String message, String workspaceId, String sessionId,
                            Consumer<String> onChunk, 
                            Runnable onComplete, 
                            Consumer<String> onError);

    // ========== 通用功能 ==========
    
    /**
     * 检查AI服务是否可用
     *
     * @return 是否可用
     */
    boolean isServiceAvailable();

    /**
     * 标题生成回调接口
     */
    interface TitleGenerationCallback {
        /**
         * 生成成功回调
         *
         * @param title 生成的标题
         */
        void onSuccess(String title);

        /**
         * 生成失败回调
         *
         * @param error 错误信息
         */
        void onError(String error);
    }
}
