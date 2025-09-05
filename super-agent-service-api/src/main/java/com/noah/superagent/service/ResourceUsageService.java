package com.noah.superagent.service;

import com.noah.superagent.common.dto.request.ResourceUsageRequest;
import com.noah.superagent.common.dto.response.ResourceUsageResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 资源使用量记录服务接口
 *
 * @author Noah
 * @since 1.0.0
 */
public interface ResourceUsageService {

    /**
     * 异步记录资源使用量
     * 避免阻塞下游智能体，支持幂等性
     * 支持Token、功能调用、媒体生成等多种资源类型
     *
     * @param request 资源使用请求
     * @return 异步处理结果
     */
    CompletableFuture<ResourceUsageResponse> recordResourceUsageAsync(ResourceUsageRequest request);
}
