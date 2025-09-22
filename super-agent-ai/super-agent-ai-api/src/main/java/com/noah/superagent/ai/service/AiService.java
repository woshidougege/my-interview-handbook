package com.noah.superagent.ai.service;

/**
 * AI服务接口
 * 提供各种AI相关功能
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public interface AiService {

    /**
     * 生成聊天标题
     * 根据用户的第一个问题生成简洁的标题
     *
     * @param question 用户问题
     * @return 生成的标题
     */
    String generateChatTitle(String question);
}
