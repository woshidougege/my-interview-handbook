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
import com.noah.superagent.service.CreditConsumeService;
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
    private final CreditConsumeService creditConsumeService;

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

            log.info("资源使用量记录成功 - reportId: {}, 消耗积分: {}, taskType: {}", 
                reportId, record.getBillingAmount(), request.getTaskType());

            // 4. 扣除用户积分
            try {
                creditConsumeService.consumeCredits(
                    request.getUserId(), 
                    record.getBillingAmount(), 
                    String.format("资源使用扣费 - %s", request.getTaskType()), 
                    null
                );
                log.info("用户积分扣费成功 - userId: {}, 扣费积分: {}", 
                         request.getUserId(), record.getBillingAmount());
            } catch (Exception e) {
                log.error("积分扣费失败 - userId: {}, 扣费积分: {}, 错误: {}", 
                          request.getUserId(), record.getBillingAmount(), e.getMessage(), e);
                // 注意：这里不抛出异常，记录已保存，积分扣费失败可以异步重试或人工处理
            }

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
        record.setResourceName(usageDetail.getModel() != null ? usageDetail.getModel() : "DEFAULT_TEXT_MODEL");
        record.setResourceSubtype("TEXT_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("model", usageDetail.getModel() != null ? usageDetail.getModel() : null);
        usageData.put("inputTokens", usageDetail.getInputTokens());
        usageData.put("outputTokens", usageDetail.getOutputTokens());
        usageData.put("totalTokens", (usageDetail.getInputTokens() != null ? usageDetail.getInputTokens() : 0L) + 
                                    (usageDetail.getOutputTokens() != null ? usageDetail.getOutputTokens() : 0L));
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 积分计算 - 根据模型名获取配置
        record.setBillingUnit("TOKEN");
        
        long inputTokens = usageDetail.getInputTokens() != null ? usageDetail.getInputTokens() : 0L;
        long outputTokens = usageDetail.getOutputTokens() != null ? usageDetail.getOutputTokens() : 0L;
        
        // 获取Token积分消耗配置（优先使用模型特定配置，否则使用默认）
        String modelCode = usageDetail.getModel() != null ? usageDetail.getModel() : null;
        Double inputTokenPrice = getInputTokenPrice(modelCode);
        Double outputTokenPrice = getOutputTokenPrice(modelCode);
        
        // 按输入输出Token分别计算积分消耗
        BigDecimal inputCredits = new BigDecimal(inputTokens).multiply(BigDecimal.valueOf(inputTokenPrice)).divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
        BigDecimal outputCredits = new BigDecimal(outputTokens).multiply(BigDecimal.valueOf(outputTokenPrice)).divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
        BigDecimal totalCredits = inputCredits.add(outputCredits);
        
        record.setUsageAmount(new BigDecimal(inputTokens + outputTokens));
        record.setUnitPrice(totalCredits.divide(new BigDecimal(inputTokens + outputTokens + 1), 6, RoundingMode.HALF_UP)); // +1 避免除0
        record.setBillingAmount(totalCredits);
    }

    /**
     * 构建图片生成记录
     */
    private void buildImageGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.IMAGE_COUNT);
        record.setResourceName(usageDetail.getModel() != null ? usageDetail.getModel() : "DEFAULT_IMAGE_MODEL");
        record.setResourceSubtype("IMAGE_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("model", usageDetail.getModel() != null ? usageDetail.getModel() : null);
        usageData.put("imageCount", usageDetail.getImageCount());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 积分计算 - 根据模型获取积分消耗配置
        record.setBillingUnit("COUNT");
        int imageCount = usageDetail.getImageCount() != null ? usageDetail.getImageCount() : 0;
        String modelCode = usageDetail.getModel() != null ? usageDetail.getModel() : null;
        BigDecimal unitCredits = BigDecimal.valueOf(getImageGenerationPrice(modelCode));
        
        record.setUsageAmount(new BigDecimal(imageCount));
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(new BigDecimal(imageCount).multiply(unitCredits));
    }

    /**
     * 构建视频生成记录
     */
    private void buildVideoGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.VIDEO_DURATION);
        record.setResourceName(usageDetail.getModel() != null ? usageDetail.getModel() : "DEFAULT_VIDEO_MODEL");
        record.setResourceSubtype("VIDEO_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("model", usageDetail.getModel() != null ? usageDetail.getModel() : null);
        usageData.put("videoDuration", usageDetail.getVideoDuration());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 积分计算 - 根据模型获取积分消耗配置
        record.setBillingUnit("SECONDS");
        int videoDuration = usageDetail.getVideoDuration() != null ? usageDetail.getVideoDuration() : 0;
        String modelCode = usageDetail.getModel() != null ? usageDetail.getModel() : null;
        BigDecimal unitCredits = BigDecimal.valueOf(getVideoGenerationPrice(modelCode));
        
        record.setUsageAmount(new BigDecimal(videoDuration));
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(new BigDecimal(videoDuration).multiply(unitCredits));
    }

    /**
     * 构建PPT生成记录
     */
    private void buildPptGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.PPT_PAGES);
        record.setResourceName(usageDetail.getModel() != null ? usageDetail.getModel() : "PPT_GENERATION");
        record.setResourceSubtype("PPT_PAGES");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("model", usageDetail.getModel() != null ? usageDetail.getModel() : null);
        usageData.put("pptPages", usageDetail.getPptPages());
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 积分计算 - 根据模型获取积分消耗配置
        record.setBillingUnit("PAGES");
        int pptPages = usageDetail.getPptPages() != null ? usageDetail.getPptPages() : 0;
        String modelCode = usageDetail.getModel() != null ? usageDetail.getModel() : null;
        BigDecimal unitCredits = getFunctionCredits("ppt_generation", modelCode);
        
        record.setUsageAmount(new BigDecimal(pptPages));
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(new BigDecimal(pptPages).multiply(unitCredits));
    }

    /**
     * 构建功能使用记录
     */
    private void buildFunctionRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail, String taskType) {
        record.setResourceType(ResourceTypeEnum.FUNCTION_TIMES);
        record.setResourceName(usageDetail.getModel() != null ? usageDetail.getModel() : taskType);
        record.setResourceSubtype("FUNCTION_TIMES");
        record.setDescription(usageDetail.getDescription());
        
        // 使用量数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("model", usageDetail.getModel() != null ? usageDetail.getModel() : null);
        usageData.put("functionTimes", usageDetail.getFunctionTimes());
        usageData.put("taskType", taskType);
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 积分计算 - 根据功能类型和模型获取积分消耗
        record.setBillingUnit("TIMES");
        int functionTimes = usageDetail.getFunctionTimes() != null ? usageDetail.getFunctionTimes() : 1;
        String modelCode = usageDetail.getModel() != null ? usageDetail.getModel() : null;
        BigDecimal unitCredits = getFunctionCredits(taskType, modelCode);
        
        record.setUsageAmount(new BigDecimal(functionTimes));
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(new BigDecimal(functionTimes).multiply(unitCredits));
    }
    
    /**
     * 获取输入Token积分价格
     */
    private Double getInputTokenPrice(String modelCode) {
        BillingProperties.ModelConfig modelConfig = resolveModelConfig(modelCode);
        if (modelConfig != null && modelConfig.getTextGeneration() != null && modelConfig.getTextGeneration().getInputToken() != null) {
            return modelConfig.getTextGeneration().getInputToken();
        }
        return billingProperties.getModelDefaults().getTextGeneration().getInputToken();
    }

    /**
     * 获取输出Token积分价格
     */
    private Double getOutputTokenPrice(String modelCode) {
        BillingProperties.ModelConfig modelConfig = resolveModelConfig(modelCode);
        if (modelConfig != null && modelConfig.getTextGeneration() != null && modelConfig.getTextGeneration().getOutputToken() != null) {
            return modelConfig.getTextGeneration().getOutputToken();
        }
        return billingProperties.getModelDefaults().getTextGeneration().getOutputToken();
    }

    /**
     * 获取图片生成积分价格
     */
    private Double getImageGenerationPrice(String modelCode) {
        BillingProperties.ModelConfig modelConfig = resolveModelConfig(modelCode);
        if (modelConfig != null && modelConfig.getImageGeneration() != null) {
            return modelConfig.getImageGeneration();
        }
        return billingProperties.getModelDefaults().getImageGeneration();
    }

    /**
     * 获取视频生成积分价格
     */
    private Double getVideoGenerationPrice(String modelCode) {
        BillingProperties.ModelConfig modelConfig = resolveModelConfig(modelCode);
        if (modelConfig != null && modelConfig.getVideoGeneration() != null) {
            return modelConfig.getVideoGeneration();
        }
        return billingProperties.getModelDefaults().getVideoGeneration();
    }

    /**
     * 解析模型配置（支持继承）
     */
    private BillingProperties.ModelConfig resolveModelConfig(String modelCode) {
        if (modelCode == null) {
            return null;
        }
        
        BillingProperties.ModelConfig config = billingProperties.getModels().get(modelCode);
        if (config == null) {
            return null;
        }
        
        // 如果有继承关系，合并父配置
        if (config.getExtendsModel() != null) {
            BillingProperties.ModelConfig parentConfig = resolveModelConfig(config.getExtendsModel());
            if (parentConfig != null) {
                config = mergeModelConfig(parentConfig, config);
            }
        }
        
        return config;
    }

    /**
     * 合并模型配置（子配置覆盖父配置）
     */
    private BillingProperties.ModelConfig mergeModelConfig(BillingProperties.ModelConfig parent, BillingProperties.ModelConfig child) {
        BillingProperties.ModelConfig merged = new BillingProperties.ModelConfig();
        
        // 文本生成配置
        if (parent.getTextGeneration() != null || child.getTextGeneration() != null) {
            BillingProperties.TextGenerationConfig mergedText = new BillingProperties.TextGenerationConfig();
            if (parent.getTextGeneration() != null) {
                mergedText.setInputToken(parent.getTextGeneration().getInputToken());
                mergedText.setOutputToken(parent.getTextGeneration().getOutputToken());
            }
            if (child.getTextGeneration() != null) {
                if (child.getTextGeneration().getInputToken() != null) {
                    mergedText.setInputToken(child.getTextGeneration().getInputToken());
                }
                if (child.getTextGeneration().getOutputToken() != null) {
                    mergedText.setOutputToken(child.getTextGeneration().getOutputToken());
                }
            }
            merged.setTextGeneration(mergedText);
        }
        
        // 其他配置字段
        merged.setImageGeneration(child.getImageGeneration() != null ? child.getImageGeneration() : parent.getImageGeneration());
        merged.setVideoGeneration(child.getVideoGeneration() != null ? child.getVideoGeneration() : parent.getVideoGeneration());
        
        // 功能配置
        merged.setBrowseruse(child.getBrowseruse() != null ? child.getBrowseruse() : parent.getBrowseruse());
        merged.setDeepsearch(child.getDeepsearch() != null ? child.getDeepsearch() : parent.getDeepsearch());
        merged.setSoftwareOperation(child.getSoftwareOperation() != null ? child.getSoftwareOperation() : parent.getSoftwareOperation());
        merged.setPptGeneration(child.getPptGeneration() != null ? child.getPptGeneration() : parent.getPptGeneration());
        merged.setMeetingMinutes(child.getMeetingMinutes() != null ? child.getMeetingMinutes() : parent.getMeetingMinutes());
        merged.setDocumentWriting(child.getDocumentWriting() != null ? child.getDocumentWriting() : parent.getDocumentWriting());
        merged.setCoding(child.getCoding() != null ? child.getCoding() : parent.getCoding());
        merged.setTranslation(child.getTranslation() != null ? child.getTranslation() : parent.getTranslation());
        merged.setMindMap(child.getMindMap() != null ? child.getMindMap() : parent.getMindMap());
        merged.setDatabaseAnalysis(child.getDatabaseAnalysis() != null ? child.getDatabaseAnalysis() : parent.getDatabaseAnalysis());
        merged.setExcelAnalysis(child.getExcelAnalysis() != null ? child.getExcelAnalysis() : parent.getExcelAnalysis());
        
        return merged;
    }

    /**
     * 根据功能类型获取积分消耗
     */
    private BigDecimal getFunctionCredits(String taskType, String modelCode) {
        // 优先查找模型特定配置（支持继承）
        BillingProperties.ModelConfig modelConfig = resolveModelConfig(modelCode);
        if (modelConfig != null) {
            Double modelSpecificPrice = getModelSpecificFunctionPrice(modelConfig, taskType);
            if (modelSpecificPrice != null) {
                return BigDecimal.valueOf(modelSpecificPrice);
            }
        }
        
        // 使用功能类默认配置
        BillingProperties.FunctionDefaultsConfig functions = billingProperties.getFunctionDefaults();
        
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
            case "ppt_generation":
                return BigDecimal.valueOf(functions.getPptGeneration());
            default:
                return BigDecimal.valueOf(0.1); // 默认积分消耗
        }
    }

    /**
     * 从模型配置中获取特定功能的价格
     */
    private Double getModelSpecificFunctionPrice(BillingProperties.ModelConfig modelConfig, String taskType) {
        switch (taskType.toLowerCase()) {
            case "deepsearch":
                return modelConfig.getDeepsearch();
            case "browseruse":
                return modelConfig.getBrowseruse();
            case "meeting_minutes":
                return modelConfig.getMeetingMinutes();
            case "document_writing":
                return modelConfig.getDocumentWriting();
            case "coding":
                return modelConfig.getCoding();
            case "translation":
                return modelConfig.getTranslation();
            case "mind_map":
                return modelConfig.getMindMap();
            case "database_analysis":
                return modelConfig.getDatabaseAnalysis();
            case "excel_analysis":
                return modelConfig.getExcelAnalysis();
            case "software_operation":
                return modelConfig.getSoftwareOperation();
            case "ppt_generation":
                return modelConfig.getPptGeneration();
            default:
                return null;
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
        
        // 积分计算 - 暂时按0积分，后续计费系统处理
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
