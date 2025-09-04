package com.noah.superagent.token.service.impl;

import cn.hutool.core.util.IdUtil;
import com.noah.superagent.common.dto.request.TokenUsageRequest;
import com.noah.superagent.common.dto.response.TokenUsageResponse;
import com.noah.superagent.dao.entity.TokenUsageRecordEntity;
import com.noah.superagent.dao.mapper.TokenUsageRecordMapper;
import com.noah.superagent.token.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token使用量记录服务实现 - 简化版
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageServiceImpl implements TokenUsageService {

    private final TokenUsageRecordMapper tokenUsageRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenUsageResponse recordTokenUsage(TokenUsageRequest request) {
        try {
            log.info("记录Token使用量 - requestId: {}, userKey: {}, agentId: {}, inputTokens: {}, outputTokens: {}", 
                request.getRequestId(), request.getUserExternalKey(), request.getAgentId(),
                request.getInputTokens(), request.getOutputTokens());

            // 1. 幂等性检查
            TokenUsageRecordEntity existingRecord = tokenUsageRecordMapper.findByRequestId(request.getRequestId());
            if (existingRecord != null) {
                log.info("重复请求，已存在记录 - requestId: {}, reportId: {}", 
                    request.getRequestId(), existingRecord.getReportId());
                return TokenUsageResponse.duplicate(existingRecord.getReportId());
            }

            // 2. 创建并保存记录
            TokenUsageRecordEntity record = buildTokenUsageRecord(request);
            tokenUsageRecordMapper.insert(record);

            log.info("Token使用量记录成功 - reportId: {}", record.getReportId());
            return TokenUsageResponse.success(record.getReportId());

        } catch (Exception e) {
            log.error("记录Token使用量失败 - requestId: {}, 错误: {}", 
                request.getRequestId(), e.getMessage(), e);
            return TokenUsageResponse.failed("记录失败: " + e.getMessage());
        }
    }

    /**
     * 构建Token使用记录实体
     */
    private TokenUsageRecordEntity buildTokenUsageRecord(TokenUsageRequest request) {
        TokenUsageRecordEntity record = new TokenUsageRecordEntity();
        record.setRequestId(request.getRequestId());
        record.setReportId(generateReportId());
        record.setUserExternalKey(request.getUserExternalKey());
        record.setAgentId(request.getAgentId());
        record.setSessionId(request.getSessionId());
        record.setModelName(request.getModelName());
        record.setInputTokens(request.getInputTokens());
        record.setOutputTokens(request.getOutputTokens());
        record.setDescription(request.getDescription());
        // 直接保存，不需要状态字段
        return record;
    }

    /**
     * 生成报告ID
     */
    private String generateReportId() {
        return "report_" + IdUtil.getSnowflakeNextIdStr();
    }
}
