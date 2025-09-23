package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.ApiResponse;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * A2A通信服务接口
 * 用于与上游智平台进行Agent-to-Agent通信
 */
public interface A2ACommunicationService {
    


    

    
    /**
     * 发送JSON-RPC格式消息到A2A平台
     * 
     * @param abilityCode 能力中心编码
     * @param entityCode 实体编码
     * @param userId 用户ID
     * @param message 消息内容
     * @param taskId 任务ID
     * @param contextId 上下文ID
     * @return InputStream 输入流
     */
    InputStream sendJsonRpcMessageToA2APlatform(
            String abilityCode, String entityCode, String userId, 
            String message, String taskId, String contextId);
    
    /**
     * 处理补充信息
     *
     * @param userId    用户ID
     * @param taskId    任务ID
     * @param userInput 用户补充的信息
     * @param sessionId 会话ID
     * @return ApiResponse 响应结果
     */
    ApiResponse<String> handleSupplementInfo(String userId, String taskId, String userInput, String sessionId);
    
    /**
     * 异步处理补充信息
     *
     * @param userId    用户ID
     * @param taskId    任务ID
     * @param userInput 用户补充的信息
     * @param sessionId 会话ID
     * @return CompletableFuture<ApiResponse<String>> 异步响应结果
     */
    CompletableFuture<ApiResponse<String>> handleSupplementInfoAsync(String userId, String taskId, String userInput, String sessionId);
}