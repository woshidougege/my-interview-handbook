package com.noah.superagent.service.impl;

import java.util.Optional;
import java.util.Set;
import java.util.Map;

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
import com.noah.superagent.service.CreditDeductionTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
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
    private final CreditDeductionTaskService creditDeductionTaskService;

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

            // 4. 根据计费金额决定是否调度积分扣减任务
            if (record.getBillingAmount().compareTo(BigDecimal.ZERO) > 0) {
                // 只有实际产生费用时才调度积分扣减任务
                try {
                    String taskId = creditDeductionTaskService.scheduleImmediateCreditDeduction(
                        request.getUserId(),
                        record.getBillingAmount(),
                        String.format("资源使用扣费 - %s", request.getTaskType()),
                        null,
                        record.getId()
                    );
                    log.info("积分扣减任务调度成功 - userId: {}, taskId: {}, 扣费积分: {}", 
                             request.getUserId(), taskId, record.getBillingAmount());
                } catch (Exception e) {
                    log.error("调度积分扣减任务失败 - userId: {}, 扣费积分: {}, 错误: {}", 
                              request.getUserId(), record.getBillingAmount(), e.getMessage(), e);
                    // 注意：这里不抛出异常，记录已保存，可以通过清理任务重新处理
                }
            } else {
                log.info("资源使用无需计费 - userId: {}, 消耗积分: {}, 跳过积分扣减任务", 
                         request.getUserId(), record.getBillingAmount());
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
     * JDK11优化：Function类型任务集合（减少重复switch判断）
     */
    private static final Set<TaskTypeEnum> FUNCTION_TASK_TYPES = Set.of(
        TaskTypeEnum.MEETING_MINUTES, TaskTypeEnum.DOCUMENT_WRITING, TaskTypeEnum.CODING,
        TaskTypeEnum.TRANSLATION, TaskTypeEnum.MIND_MAP, TaskTypeEnum.DATABASE_ANALYSIS,
        TaskTypeEnum.EXCEL_ANALYSIS, TaskTypeEnum.BROWSERUSE, TaskTypeEnum.DEEPSEARCH,
        TaskTypeEnum.SOFTWARE_OPERATION
    );

    /**
     * 构建资源使用记录 - JDK11优化版本
     */
    private ResourceUsageRecordEntity buildResourceUsageRecord(ResourceUsageRequest request, String reportId) {
        var record = new ResourceUsageRecordEntity();
        var usageDetail = request.getUsageDetail();
        var taskType = request.getTaskType();
        
        // 基础信息设置
        setBaseFields(record, request, reportId);
        
        // 使用JDK11兼容的switch语句 + 现代化Set查找优化
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
            default:
                if (FUNCTION_TASK_TYPES.contains(taskType)) {
                    buildFunctionRecord(record, usageDetail, taskType.getCode());
                } else {
                    buildDefaultRecord(record, usageDetail);
                }
                break;
        }
        
        return record;
    }

    // JDK11优化：提取常量避免魔数
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private static final String DEFAULT_TEXT_MODEL = "DEFAULT_TEXT_MODEL";
    
    /**
     * 构建文本生成记录 - JDK11优化版本
     */
    private void buildTextGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.TOKEN);
        record.setResourceName(Optional.ofNullable(usageDetail.getModel()).orElse(DEFAULT_TEXT_MODEL));
        record.setResourceSubtype("TEXT_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用JDK11 Optional优化token计算，明确类型避免类型推断问题
        Long inputTokens = Optional.ofNullable(usageDetail.getInputTokens()).orElse(0L);
        Long outputTokens = Optional.ofNullable(usageDetail.getOutputTokens()).orElse(0L);
        long totalTokens = inputTokens + outputTokens;
        
        // 使用JDK11 Map.of()构建不可变数据
        Map<String, Object> usageData = Map.of(
            "model", Optional.ofNullable(usageDetail.getModel()).orElse(""),
            "inputTokens", inputTokens,
            "outputTokens", outputTokens,
            "totalTokens", totalTokens
        );
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 函数式编程优化积分计算
        record.setBillingUnit("TOKEN");
        String modelCode = Optional.ofNullable(usageDetail.getModel()).orElse("");
        
        // 函数式计算Token费用
        BigDecimal inputCredits = calculateTokenCredits(inputTokens, getInputTokenPrice(modelCode));
        BigDecimal outputCredits = calculateTokenCredits(outputTokens, getOutputTokenPrice(modelCode));
        BigDecimal totalCredits = inputCredits.add(outputCredits);
        
        BigDecimal totalUsageAmount = BigDecimal.valueOf(totalTokens);
        record.setUsageAmount(totalUsageAmount);
        record.setUnitPrice(calculateSafeUnitPrice(totalCredits, totalTokens));
        record.setBillingAmount(totalCredits);
    }
    
    /**
     * 安全计算单价，避免除零异常
     */
    private BigDecimal calculateSafeUnitPrice(BigDecimal totalCredits, long totalTokens) {
        return totalTokens > 0 
            ? totalCredits.divide(BigDecimal.valueOf(totalTokens), 6, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }
    
    /**
     * 计算Token费用的工具方法
     */
    private BigDecimal calculateTokenCredits(long tokens, Double price) {
        return BigDecimal.valueOf(tokens)
                .multiply(BigDecimal.valueOf(price))
                .divide(THOUSAND, 2, RoundingMode.HALF_UP);
    }

    /**
     * 构建图片生成记录 - JDK11优化版本  
     */
    private void buildImageGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.IMAGE_COUNT);
        record.setResourceName(Optional.ofNullable(usageDetail.getModel()).orElse("DEFAULT_IMAGE_MODEL"));
        record.setResourceSubtype("IMAGE_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // 使用JDK11优化：简化null处理和Map构建
        Integer imageCount = Optional.ofNullable(usageDetail.getImageCount()).orElse(0);
        String modelCode = Optional.ofNullable(usageDetail.getModel()).orElse("");
        
        Map<String, Object> usageData = Map.of(
            "model", modelCode,
            "imageCount", imageCount
        );
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 优雅计算积分
        record.setBillingUnit("COUNT");
        BigDecimal unitCredits = BigDecimal.valueOf(getImageGenerationPrice(modelCode));
        BigDecimal usageAmount = BigDecimal.valueOf(imageCount);
        
        record.setUsageAmount(usageAmount);
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(usageAmount.multiply(unitCredits));
    }

    /**
     * 构建视频生成记录 - JDK11优化版本
     */
    private void buildVideoGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.VIDEO_DURATION);
        record.setResourceName(Optional.ofNullable(usageDetail.getModel()).orElse("DEFAULT_VIDEO_MODEL"));
        record.setResourceSubtype("VIDEO_GENERATION");
        record.setDescription(usageDetail.getDescription());
        
        // JDK11优化：简洁的数据处理
        Integer videoDuration = Optional.ofNullable(usageDetail.getVideoDuration()).orElse(0);
        String modelCode = Optional.ofNullable(usageDetail.getModel()).orElse("");
        
        Map<String, Object> usageData = Map.of(
            "model", modelCode,
            "videoDuration", videoDuration
        );
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 函数式积分计算
        record.setBillingUnit("SECONDS");
        BigDecimal unitCredits = BigDecimal.valueOf(getVideoGenerationPrice(modelCode));
        BigDecimal usageAmount = BigDecimal.valueOf(videoDuration);
        
        record.setUsageAmount(usageAmount);
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(usageAmount.multiply(unitCredits));
    }

    /**
     * 构建PPT生成记录 - JDK11优化版本
     */
    private void buildPptGenerationRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail) {
        record.setResourceType(ResourceTypeEnum.PPT_PAGES);
        record.setResourceName(Optional.ofNullable(usageDetail.getModel()).orElse("PPT_GENERATION"));
        record.setResourceSubtype("PPT_PAGES");
        record.setDescription(usageDetail.getDescription());
        
        // JDK11优化：清晰的数据处理
        Integer pptPages = Optional.ofNullable(usageDetail.getPptPages()).orElse(0);
        String modelCode = Optional.ofNullable(usageDetail.getModel()).orElse("");
        
        Map<String, Object> usageData = Map.of(
            "model", modelCode,
            "pptPages", pptPages
        );
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 简洁的积分计算
        record.setBillingUnit("PAGES");
        BigDecimal unitCredits = getFunctionCredits("ppt_generation", modelCode);
        BigDecimal usageAmount = BigDecimal.valueOf(pptPages);
        
        record.setUsageAmount(usageAmount);
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(usageAmount.multiply(unitCredits));
    }

    /**
     * 构建功能使用记录 - JDK11优化版本
     */
    private void buildFunctionRecord(ResourceUsageRecordEntity record, ResourceUsageRequest.ResourceUsageDetail usageDetail, String taskType) {
        record.setResourceType(ResourceTypeEnum.FUNCTION_TIMES);
        record.setResourceName(Optional.ofNullable(usageDetail.getModel()).orElse(taskType));
        record.setResourceSubtype("FUNCTION_TIMES");
        record.setDescription(usageDetail.getDescription());
        
        // 使用JDK11 Map.of()和Optional构建使用量数据
        Integer functionTimes = Optional.ofNullable(usageDetail.getFunctionTimes()).orElse(0);
        String modelCode = Optional.ofNullable(usageDetail.getModel()).orElse("");
        
        Map<String, Object> usageData = Map.of(
            "model", modelCode,
            "functionTimes", functionTimes,
            "taskType", taskType
        );
        record.setUsageData(JSONUtil.toJsonStr(usageData));
        
        // 使用JDK11 Optional优化积分计算
        record.setBillingUnit("TIMES");
        BigDecimal unitCredits = getFunctionCredits(taskType, modelCode);
        
        BigDecimal usageAmount = BigDecimal.valueOf(functionTimes);
        record.setUsageAmount(usageAmount);
        record.setUnitPrice(unitCredits);
        record.setBillingAmount(usageAmount.multiply(unitCredits));
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
            BillingProperties.TextGenerationConfig mergedText = getTextGenerationConfig(parent, child);
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

    private static BillingProperties.TextGenerationConfig getTextGenerationConfig(BillingProperties.ModelConfig parent, BillingProperties.ModelConfig child) {
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
        return mergedText;
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
