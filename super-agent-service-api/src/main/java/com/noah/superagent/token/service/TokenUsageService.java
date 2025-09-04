package com.noah.superagent.token.service;

import com.noah.superagent.common.dto.request.TokenUsageRequest;
import com.noah.superagent.common.dto.response.TokenUsageResponse;

/**
 * Token使用量记录服务接口 - 简化版
 *
 * @author Noah
 * @since 1.0.0
 */
public interface TokenUsageService {

    /**
     * 记录Token使用量
     *
     * @param request Token使用请求
     * @return 响应
     */
    TokenUsageResponse recordTokenUsage(TokenUsageRequest request);
}
