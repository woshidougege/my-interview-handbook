package com.noah.superagent.service;

/**
 * AI服务接口
 * 提供AI相关功能的服务接口
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public interface AiService {

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
