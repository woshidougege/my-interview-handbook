package com.noah.superagent.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.noah.superagent.common.dto.request.ResourceUsageRequest;
import com.noah.superagent.common.dto.response.ResourceUsageResponse;
import com.noah.superagent.dao.entity.ResourceUsageRecordEntity;
import com.noah.superagent.dao.mapper.ResourceUsageRecordMapper;
import com.noah.superagent.service.ResourceUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 资源使用量记录服务实现
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUsageServiceImpl implements ResourceUsageService {

    private final ResourceUsageRecordMapper resourceUsageRecordMapper;

    // 计费配置
    @Value("${super-agent.billing.pricing.input-token:0.0024}")
    private BigDecimal inputTokenPrice;

    @Value("${super-agent.billing.pricing.output-token:0.0096}")
    private BigDecimal outputTokenPrice;

    @Value("${super-agent.billing.pricing.image-generation:0.25}")
    private BigDecimal imageGenerationPrice;

    @Value("${super-agent.billing.pricing.video-generation:0.24}")
    private BigDecimal videoGenerationPrice;

    @Value("${super-agent.billing.functions.deepsearch:0.001}")
    private BigDecimal deepsearchPrice;

    @Value("${super-agent.billing.functions.browseruse:0.001}")
    private BigDecimal browserusePrice;

    @Value("${super-agent.billing.functions.ppt-generation:0.001}")
    private BigDecimal pptGenerationPrice;

    /**
     * 异步记录资源使用量
     */
    @Override
    @Async("resourceReportExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<ResourceUsageResponse> recordResourceUsageAsync(ResourceUsageRequest request) {
        try {
            log.info("异步记录资源使用量 - requestId: {}, userId: {}, agentId: {}, taskType: {}", 
                request.getRequestId(), request.getUserId(), request.getAgentId(), request.getTaskType());

            // 1. 幂等性检查
            List<ResourceUsageRecordEntity> existingRecords = resourceUsageRecordMapper.findByRequestId(request.getRequestId());
            if (!existingRecords.isEmpty()) {
                String reportId = existingRecords.get(0).getReportId();
                log.info("重复请求，已存在记录 - requestId: {}, reportId: {}", 
                    request.getRequestId(), reportId);
                return CompletableFuture.completedFuture(ResourceUsageResponse.duplicate(reportId));
            }

            // 2. 处理资源使用记录
            String reportId = generateReportId();
            List<ResourceUsageRecordEntity> records = new ArrayList<>();
            BigDecimal totalBillingAmount = BigDecimal.ZERO;

            // 处理模型使用记录
            if (request.getModelUsages() != null) {
                for (ResourceUsageRequest.ModelUsage modelUsage : request.getModelUsages()) {
                    ResourceUsageRecordEntity record = buildModelUsageRecord(request, modelUsage, reportId);
                    records.add(record);
                    totalBillingAmount = totalBillingAmount.add(record.getBillingAmount());
                }
            }

            // 处理功能使用记录
            if (request.getFunctionUsages() != null) {
                for (ResourceUsageRequest.FunctionUsage functionUsage : request.getFunctionUsages()) {
                    ResourceUsageRecordEntity record = buildFunctionUsageRecord(request, functionUsage, reportId);
                    records.add(record);
                    totalBillingAmount = totalBillingAmount.add(record.getBillingAmount());
                }
            }

            // 处理媒体使用记录
            if (request.getMediaUsages() != null) {
                for (ResourceUsageRequest.MediaUsage mediaUsage : request.getMediaUsages()) {
                    ResourceUsageRecordEntity record = buildMediaUsageRecord(request, mediaUsage, reportId);
                    records.add(record);
                    totalBillingAmount = totalBillingAmount.add(record.getBillingAmount());
                }
            }

            // 3. 批量保存记录
            for (ResourceUsageRecordEntity record : records) {
                resourceUsageRecordMapper.insert(record);
            }

            log.info("资源使用量记录成功 - reportId: {}, 总计费金额: {}, 记录数: {}", 
                reportId, totalBillingAmount, records.size());

            return CompletableFuture.completedFuture(
                ResourceUsageResponse.success(reportId, totalBillingAmount, BigDecimal.ZERO, BigDecimal.ZERO)
            );

        } catch (Exception e) {
            log.error("资源使用量记录失败 - requestId: {}, 错误: {}", 
                request.getRequestId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(ResourceUsageResponse.failed("记录失败: " + e.getMessage()));
        }
    }

    /**
     * 构建模型使用记录
     */
    private ResourceUsageRecordEntity buildModelUsageRecord(ResourceUsageRequest request, 
            ResourceUsageRequest.ModelUsage modelUsage, String reportId) {
        ResourceUsageRecordEntity record = new ResourceUsageRecordEntity();
        
        // 基础信息
        setBaseFields(record, request, reportId);
        
        // 资源信息
        record.setResourceType("MODEL");
        record.setResourceName(modelUsage.getModelName());
        record.setResourceSubtype(modelUsage.getModelType());
        record.setDescription(modelUsage.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("inputTokens", modelUsage.getInputTokens());
        usageData.put("outputTokens", modelUsage.getOutputTokens());
        usageData.put("modelType", modelUsage.getModelType());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算
        BigDecimal inputCost = calculateTokenCost(modelUsage.getInputTokens(), inputTokenPrice);
        BigDecimal outputCost = calculateTokenCost(modelUsage.getOutputTokens(), outputTokenPrice);
        BigDecimal totalCost = inputCost.add(outputCost);
        
        record.setBillingUnit("TOKEN");
        record.setUsageAmount(new BigDecimal(modelUsage.getInputTokens() + modelUsage.getOutputTokens()));
        record.setUnitPrice(inputTokenPrice); // 简化，实际应该是加权平均
        record.setBillingAmount(totalCost);
        
        return record;
    }

    /**
     * 构建功能使用记录
     */
    private ResourceUsageRecordEntity buildFunctionUsageRecord(ResourceUsageRequest request, 
            ResourceUsageRequest.FunctionUsage functionUsage, String reportId) {
        ResourceUsageRecordEntity record = new ResourceUsageRecordEntity();
        
        // 基础信息
        setBaseFields(record, request, reportId);
        
        // 资源信息
        record.setResourceType("FUNCTION");
        record.setResourceName(functionUsage.getFunctionType());
        record.setResourceSubtype(functionUsage.getBillingUnit());
        record.setDescription(functionUsage.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("functionType", functionUsage.getFunctionType());
        usageData.put("usageCount", functionUsage.getUsageCount());
        usageData.put("billingUnit", functionUsage.getBillingUnit());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算
        BigDecimal unitPrice = getFunctionPrice(functionUsage.getFunctionType());
        BigDecimal totalCost = unitPrice.multiply(new BigDecimal(functionUsage.getUsageCount()));
        
        record.setBillingUnit(functionUsage.getBillingUnit());
        record.setUsageAmount(new BigDecimal(functionUsage.getUsageCount()));
        record.setUnitPrice(unitPrice);
        record.setBillingAmount(totalCost);
        
        return record;
    }

    /**
     * 构建媒体使用记录
     */
    private ResourceUsageRecordEntity buildMediaUsageRecord(ResourceUsageRequest request, 
            ResourceUsageRequest.MediaUsage mediaUsage, String reportId) {
        ResourceUsageRecordEntity record = new ResourceUsageRecordEntity();
        
        // 基础信息
        setBaseFields(record, request, reportId);
        
        // 资源信息
        record.setResourceType("MEDIA");
        record.setResourceName(mediaUsage.getMediaType());
        record.setResourceSubtype(mediaUsage.getBillingUnit());
        record.setDescription(mediaUsage.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("mediaType", mediaUsage.getMediaType());
        usageData.put("usageAmount", mediaUsage.getUsageAmount());
        usageData.put("billingUnit", mediaUsage.getBillingUnit());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算
        BigDecimal unitPrice = getMediaPrice(mediaUsage.getMediaType());
        BigDecimal totalCost = unitPrice.multiply(new BigDecimal(mediaUsage.getUsageAmount()));
        
        record.setBillingUnit(mediaUsage.getBillingUnit());
        record.setUsageAmount(new BigDecimal(mediaUsage.getUsageAmount()));
        record.setUnitPrice(unitPrice);
        record.setBillingAmount(totalCost);
        
        return record;
    }

    /**
     * 设置基础字段
     */
    private void setBaseFields(ResourceUsageRecordEntity record, ResourceUsageRequest request, String reportId) {
        record.setRequestId(request.getRequestId());
        record.setReportId(reportId);
        record.setUserId(request.getUserId());
        record.setAgentId(request.getAgentId());
        record.setSessionId(request.getSessionId());
        record.setTaskType(request.getTaskType());
        record.setTaskDescription(request.getTaskDescription());
    }

    /**
     * 计算Token费用
     */
    private BigDecimal calculateTokenCost(Long tokens, BigDecimal pricePerThousand) {
        if (tokens == null || tokens == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(tokens).divide(new BigDecimal(1000), 6, BigDecimal.ROUND_HALF_UP)
                .multiply(pricePerThousand);
    }

    /**
     * 获取功能价格
     */
    private BigDecimal getFunctionPrice(String functionType) {
        switch (functionType) {
            case "DEEPSEARCH":
                return deepsearchPrice;
            case "BROWSERUSE":
                return browserusePrice;
            case "PPT_GENERATION":
                return pptGenerationPrice;
            default:
                return BigDecimal.valueOf(0.001); // 默认价格
        }
    }

    /**
     * 获取媒体价格
     */
    private BigDecimal getMediaPrice(String mediaType) {
        switch (mediaType) {
            case "IMAGE":
                return imageGenerationPrice;
            case "VIDEO":
                return videoGenerationPrice;
            default:
                return BigDecimal.valueOf(0.1); // 默认价格
        }
    }

    /**
     * 生成报告ID
     */
    private String generateReportId() {
        return "report_" + IdUtil.getSnowflakeNextIdStr();
    }
}
