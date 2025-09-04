package com.noah.superagent.token.service;

import com.noah.superagent.common.dto.request.TokenUsageRequest;
import com.noah.superagent.common.dto.response.TokenUsageResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Token使用量记录服务接口
 *
 * @author Noah
 * @since 1.0.0
 */
public interface TokenUsageService {

    /**
     * 异步记录Token使用量
     * 避免阻塞下游智能体，支持幂等性
     *
     * @param request Token使用请求
     * @return 异步处理结果
     */
    CompletableFuture<TokenUsageResponse> recordTokenUsageAsync(TokenUsageRequest request);
}
