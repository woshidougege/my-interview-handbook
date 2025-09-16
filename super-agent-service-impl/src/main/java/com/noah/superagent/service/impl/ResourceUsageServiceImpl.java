package com.noah.superagent.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.noah.superagent.common.config.BillingProperties;
import com.noah.superagent.common.dto.request.ResourceUsageRequest;
import com.noah.superagent.common.enums.ResourceTypeEnum;
import com.noah.superagent.common.enums.TaskTypeEnum;
import com.noah.superagent.common.dto.response.ResourceUsageResponse;
import com.noah.superagent.dao.entity.ResourceUsageRecordEntity;
import com.noah.superagent.dao.mapper.ResourceUsageRecordMapper;
import com.noah.superagent.service.ResourceUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 资源使用量记录服务实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUsageServiceImpl implements ResourceUsageService {

    private final ResourceUsageRecordMapper resourceUsageRecordMapper;
    private final BillingProperties billingProperties;

    /**
     * 异步记录资源使用量
     * 子智能体每次任务完成后上报真实消耗数据
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
            ResourceUsageRecordEntity record = buildResourceUsageRecord(request, reportId);
            
            // 3. 保存记录
            resourceUsageRecordMapper.insert(record);

            log.info("资源使用量记录成功 - reportId: {}, 计费金额: {}, taskType: {}", 
                reportId, record.getBillingAmount(), request.getTaskType());

            return CompletableFuture.completedFuture(
                ResourceUsageResponse.success(reportId, record.getBillingAmount(), BigDecimal.ZERO, BigDecimal.ZERO)
            );

        } catch (Exception e) {
            log.error("资源使用量记录失败 - requestId: {}, 错误: {}", 
                request.getRequestId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(ResourceUsageResponse.failed("记录失败: " + e.getMessage()));
        }
    }

/**
     * 构建资源使用记录
     */
    private ResourceUsageRecordEntity buildResourceUsageRecord(ResourceUsageRequest request, String reportId) {
        ResourceUsageRecordEntity record = new ResourceUsageRecordEntity();
        ResourceUsageRequest.ResourceUsageDetail usageDetail = request.getUsageDetail();
        
        // 基础信息
        setBaseFields(record, request, reportId);
        
        // 根据任务类型和资源类型构建记录
        TaskTypeEnum taskType = request.getTaskType();
        switch (taskType) {
            case TEXT_GENERATION:
                buildTextGenerationRecord(record, usageDetail);
                break;
            case IMAGE_GENERATION:
                buildImageGenerationRecord(record, usageDetail);
                break;
            case VIDEO_GENERATION:
                buildVideoGenerationRecord(record, usageDetail);
                break;
            case PPT_GENERATION:
                buildPptGenerationRecord(record, usageDetail);
                break;
            case MEETING_MINUTES:
            case DOCUMENT_WRITING:
            case CODING:
            case TRANSLATION:
            case MIND_MAP:
            case DATABASE_ANALYSIS:
            case EXCEL_ANALYSIS:
            case BROWSERUSE:
            case DEEPSEARCH:
            case SOFTWARE_OPERATION:
                buildFunctionRecord(record, usageDetail, taskType.getCode());
                break;
            default:
                buildDefaultRecord(record, usageDetail);
        }
        
        return record;
    }

    /**
     * 构建文本生成记录
     */
    private void buildTextGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.TOKEN);
        record.setResourceName("TEXT_MODEL");
        record.setResourceSubtype("TEXT_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("inputTokens", usageDetail.getInputTokens());
        usageData.put("outputTokens", usageDetail.getOutputTokens());
        usageData.put("totalTokens", (usageDetail.getInputTokens() != null ? usageDetail.getInputTokens() : 0L) + 
                                    (usageDetail.getOutputTokens() != null ? usageDetail.getOutputTokens() : 0L));
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算 - 使用实际价格配置
        record.setBillingUnit("TOKEN");
        
        long inputTokens = usageDetail.getInputTokens() != null ? usageDetail.getInputTokens() : 0L;
        long outputTokens = usageDetail.getOutputTokens() != null ? usageDetail.getOutputTokens() : 0L;
        
        // 按输入输出Token分别计费
        BigDecimal inputCost = new BigDecimal(inputTokens).multiply(BigDecimal.valueOf(billingProperties.getPricing().getInputToken())).divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = new BigDecimal(outputTokens).multiply(BigDecimal.valueOf(billingProperties.getPricing().getOutputToken())).divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        BigDecimal totalCost = inputCost.add(outputCost);
        
        record.setUsageAmount(new BigDecimal(inputTokens + outputTokens));
        record.setUnitPrice(totalCost.divide(new BigDecimal(inputTokens + outputTokens + 1), 6, RoundingMode.HALF_UP)); // +1 避免除0
        record.setBillingAmount(totalCost);
    }

    /**
     * 构建图片生成记录
     */
    private void buildImageGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.IMAGE_COUNT);
        record.setResourceName("IMAGE");
        record.setResourceSubtype("IMAGE_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("imageCount", usageDetail.getImageCount());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算 - 使用实际价格配置
        record.setBillingUnit("COUNT");
        int imageCount = usageDetail.getImageCount() != null ? usageDetail.getImageCount() : 0;
        BigDecimal unitPrice = BigDecimal.valueOf(billingProperties.getPricing().getImageGeneration());
        
        record.setUsageAmount(new BigDecimal(imageCount));
        record.setUnitPrice(unitPrice);
        record.setBillingAmount(new BigDecimal(imageCount).multiply(unitPrice));
    }

    /**
     * 构建视频生成记录
     */
    private void buildVideoGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.VIDEO_DURATION);
        record.setResourceName("VIDEO");
        record.setResourceSubtype("VIDEO_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("videoDuration", usageDetail.getVideoDuration());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算 - 使用实际价格配置
        record.setBillingUnit("SECONDS");
        int videoDuration = usageDetail.getVideoDuration() != null ? usageDetail.getVideoDuration() : 0;
        BigDecimal unitPrice = BigDecimal.valueOf(billingProperties.getPricing().getVideoGeneration());
        
        record.setUsageAmount(new BigDecimal(videoDuration));
        record.setUnitPrice(unitPrice);
        record.setBillingAmount(new BigDecimal(videoDuration).multiply(unitPrice));
    }

    /**
     * 构建PPT生成记录
     */
    private void buildPptGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.PPT_PAGES);
        record.setResourceName("PPT_GENERATION");
        record.setResourceSubtype("PPT_PAGES");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("pptPages", usageDetail.getPptPages());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算 - 使用实际价格配置
        record.setBillingUnit("PAGES");
        int pptPages = usageDetail.getPptPages() != null ? usageDetail.getPptPages() : 0;
        BigDecimal unitPrice = BigDecimal.valueOf(billingProperties.getFunctions().getPptGeneration());
        
        record.setUsageAmount(new BigDecimal(pptPages));
        record.setUnitPrice(unitPrice);
        record.setBillingAmount(new BigDecimal(pptPages).multiply(unitPrice));
    }

    /**
     * 构建功能使用记录
     */
    private void buildFunctionRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail, String taskType) {
        record.setResourceType(ResourceTypeEnum.FUNCTION_TIMES);
        record.setResourceName(taskType);
        record.setResourceSubtype("FUNCTION_TIMES");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("functionTimes", usageDetail.getFunctionTimes());
        usageData.put("taskType", taskType);
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算 - 根据功能类型获取价格
        record.setBillingUnit("TIMES");
        int functionTimes = usageDetail.getFunctionTimes() != null ? usageDetail.getFunctionTimes() : 1;
        BigDecimal unitPrice = getFunctionPrice(taskType);
        
        record.setUsageAmount(new BigDecimal(functionTimes));
        record.setUnitPrice(unitPrice);
        record.setBillingAmount(new BigDecimal(functionTimes).multiply(unitPrice));
    }
    
    /**
     * 根据功能类型获取价格
     */
    private BigDecimal getFunctionPrice(String taskType) {
        BillingProperties.FunctionsConfig functions = billingProperties.getFunctions();
        
        switch (taskType.toLowerCase()) {
            case "deepsearch":
                return BigDecimal.valueOf(functions.getDeepsearch());
            case "browseruse":
                return BigDecimal.valueOf(functions.getBrowseruse());
            case "meeting_minutes":
                return BigDecimal.valueOf(functions.getMeetingMinutes());
            case "document_writing":
                return BigDecimal.valueOf(functions.getDocumentWriting());
            case "coding":
                return BigDecimal.valueOf(functions.getCoding());
            case "translation":
                return BigDecimal.valueOf(functions.getTranslation());
            case "mind_map":
                return BigDecimal.valueOf(functions.getMindMap());
            case "database_analysis":
                return BigDecimal.valueOf(functions.getDatabaseAnalysis());
            case "excel_analysis":
                return BigDecimal.valueOf(functions.getExcelAnalysis());
            case "software_operation":
                return BigDecimal.valueOf(functions.getSoftwareOperation());
            default:
                return BigDecimal.valueOf(0.001); // 默认价格
        }
    }

    /**
     * 构建默认记录
     */
    private void buildDefaultRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.TOKEN); // 默认使用TOKEN类型
        record.setResourceName("UNKNOWN");
        record.setResourceSubtype("UNKNOWN");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("rawData", usageDetail);
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 计费计算 - 暂时按0计费，后续计费系统处理
        record.setBillingUnit("UNKNOWN");
        record.setUsageAmount(BigDecimal.ZERO);
        record.setUnitPrice(BigDecimal.ZERO);
        record.setBillingAmount(BigDecimal.ZERO);
    }

    /**
     * 设置基础字段
     */
    private void setBaseFields(ResourceUsageRecordEntity record, ResourceUsageRequest request, String reportId) {
        record.setRequestId(request.getRequestId());
        record.setReportId(reportId);
        record.setUserId(request.getUserId());
        record.setAgentId(request.getAgentId());
        record.setContextId(request.getContextId());
        record.setTaskType(request.getTaskType() != null ? request.getTaskType() : null);
        record.setTaskDescription(request.getTaskDescription());
    }

    /**
     * 生成报告ID
     */
    private String generateReportId() {
        return "report_" + IdUtil.getSnowflakeNextIdStr();
    }
}
