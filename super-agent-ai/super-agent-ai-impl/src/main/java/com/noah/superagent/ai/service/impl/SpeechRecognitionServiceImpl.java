package com.noah.superagent.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.common.config.SpeechRecognitionProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;

/**
 * 语音识别服务实现 - 基于本地FunASR服务
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionServiceImpl implements SpeechRecognitionService {

    private final SpeechRecognitionProperties speechProperties;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebClient webClient;

    @PostConstruct
    public void init() {
        // 初始化WebClient
        this.webClient = WebClient.builder()
                .baseUrl(speechProperties.getFunasr().getServerUrl())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024)) // 100MB
                .build();

        log.info("FunASR服务初始化完成，服务地址: {}", speechProperties.getFunasr().getServerUrl());
    }

    @Override
    public SpeechRecognitionResponse recognizeAudioStream(InputStream audioStream, String filename, String language, String model) {
        log.debug("开始识别录音文件: {}, 语言: {}", filename, language);
        
        long speechRecognitionStartTime = System.currentTimeMillis();

        try {
            // 直接流式调用FunASR服务进行识别
            String originalText = transcribeByStream(audioStream, filename);
            long speechRecognitionEndTime = System.currentTimeMillis();
            long speechRecognitionDuration = speechRecognitionEndTime - speechRecognitionStartTime;
            
            // 打印识别结果摘要
            log.info("===== 语音识别结果 =====");
            log.info("文本统计:");
            log.info("  ├─ 字符总数: {} 字符", originalText.length());
            log.info("  ├─ 预估行数: {} 行", Math.max(1, originalText.length() / 65));
            log.info("  └─ 文本预览: {}", org.apache.commons.lang3.StringUtils.abbreviate(org.apache.commons.lang3.StringUtils.normalizeSpace(originalText), 100));
            log.info("==========================");
            
            // 计算音频处理时长（估算）
            long estimatedAudioDuration = estimateAudioDuration(originalText, speechRecognitionDuration);
            
            // 生成文件ID
            String fileId = IdUtil.getSnowflakeNextIdStr();
            
            // 判断是否需要文本纠错
            String finalText = originalText;
            long textCorrectionDuration = 0;
            boolean correctionApplied = false;
            
            if (shouldCorrectText(estimatedAudioDuration, originalText.length())) {
                String modelName = speechProperties.getTextCorrection().getModel();
                log.info("触发文本纠错条件，开始使用{}模型纠正文本", modelName);
                
                long correctionStartTime = System.currentTimeMillis();
                String correctedText = correctTextWithAiModel(originalText);
                long correctionEndTime = System.currentTimeMillis();
                textCorrectionDuration = correctionEndTime - correctionStartTime;
                
                if (StringUtils.hasText(correctedText) && !correctedText.equals(originalText)) {
                    finalText = correctedText;
                    correctionApplied = true;
                    
                    // 详细的纠错对比日志
                    log.info("===== 文本纠错对比结果 =====");
                    log.info("纠错统计:");
                    log.info("  ├─ 原始文本长度: {} 字符", originalText.length());
                    log.info("  ├─ 纠错后长度: {} 字符", correctedText.length());
                    log.info("  ├─ 长度变化: {}{} 字符", 
                            correctedText.length() > originalText.length() ? "+" : "",
                            correctedText.length() - originalText.length());
                    log.info("  └─ 纠错效果: {}",
                            correctedText.length() > originalText.length() ? "增加了标点和格式" : "压缩了冗余内容");
                    log.info("");
                    log.info("文本对比预览:");
                    log.info("  ┌─ 原始文本: {}", org.apache.commons.lang3.StringUtils.abbreviate(org.apache.commons.lang3.StringUtils.normalizeSpace(originalText), 150));
                    log.info("  └─ 纠错文本: {}", org.apache.commons.lang3.StringUtils.abbreviate(org.apache.commons.lang3.StringUtils.normalizeSpace(correctedText), 150));
                    log.info("==============================");
                } else {
                    log.info("文本纠错未产生变化或失败，使用原始识别结果");
                }
            }
            
            // 计算总耗时
            long totalDuration = System.currentTimeMillis() - speechRecognitionStartTime;
            
            // 格式化打印性能统计
            printPerformanceStatistics(filename, fileId, originalText.length(), estimatedAudioDuration,
                    speechRecognitionDuration, textCorrectionDuration, totalDuration, correctionApplied);
            
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                    .text(finalText)
                    .isFinal(true)
                    .fileId(fileId)
                    .build();
            
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - speechRecognitionStartTime;
            log.error("语音识别失败 - 文件: {}, 总耗时: {}, 错误: {}", 
                    filename, DurationFormatUtils.formatDurationHMS(totalDuration), e.getMessage());
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("语音识别失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        }
    }

    /**
     * 通过文件流进行识别
     */
    private String transcribeByStream(InputStream audioStream, String filename) {
        try {
            log.debug("开始转录音频文件: {}", filename);
            
            // 将InputStream转换为byte数组，避免多次读取流的问题
            byte[] audioBytes = readAllBytes(audioStream);
            log.debug("音频文件读取完成，大小: {} bytes", audioBytes.length);
            
            // 构建multipart/form-data请求
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename; // 保持原文件名
                }
            });
            
            // 获取超时配置，如果为0则表示无限制
            int timeoutSeconds = speechProperties.getFunasr().getTimeout();
            
            String response;
            if (timeoutSeconds <= 0) {
                // 无限制超时
                response = webClient.post()
                        .uri("/transcribe")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(parts))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                log.debug("使用无限制超时进行语音识别");
            } else {
                // 有限制超时
                response = webClient.post()
                        .uri("/transcribe")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(parts))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .block();
                log.debug("使用{}秒超时进行语音识别", timeoutSeconds);
            }

            return parseTranscriptionResult(response);
            
        } catch (WebClientResponseException e) {
            log.error("FunASR服务调用失败: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("语音识别服务调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("语音识别过程中发生错误", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage());
        }
    }

    /**
     * 读取InputStream的所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[8192];
        int bytesRead;
        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }


    /**
     * 解析FunASR返回结果
     */
    private String parseTranscriptionResult(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 处理Python FunASR接口返回格式: {"success": true, "text": "..."}
            if (jsonNode.has("success") && jsonNode.get("success").asBoolean()) {
                if (jsonNode.has("text")) {
                    String result = jsonNode.get("text").asText();
                    log.debug("成功解析FunASR识别结果，长度: {}字符", result.length());
                    return result;
                }
                
                // 兼容其他success格式: {"success": true, "result": [{"text": "..."}]}
                JsonNode resultArray = jsonNode.get("result");
                if (resultArray != null && resultArray.isArray() && !resultArray.isEmpty()) {
                    JsonNode firstResult = resultArray.get(0);
                    return firstResult.get("text").asText();
                }
            }
            
            // 处理简单文本格式: {"text": "..."}
            if (jsonNode.has("text")) {
                return jsonNode.get("text").asText();
            }
            
            // 处理数组格式: [{"text": "..."}]
            if (jsonNode.isArray() && !jsonNode.isEmpty()) {
                JsonNode firstItem = jsonNode.get(0);
                if (firstItem.has("text")) {
                    return firstItem.get("text").asText();
                }
            }
            
            log.warn("无法解析识别结果: {}", response);
            return "识别结果解析失败";
            
        } catch (Exception e) {
            log.error("解析识别结果时发生错误", e);
            return "结果解析错误: " + e.getMessage();
        }
    }

    /**
     * 判断是否需要文本纠错
     */
    private boolean shouldCorrectText(long audioDurationSeconds, int textLength) {
        SpeechRecognitionProperties.TextCorrectionConfig config = speechProperties.getTextCorrection();
        
        // 检查是否启用纠错
        if (!config.getEnabled()) {
            log.debug("文本纠错功能未启用");
            return false;
        }
        
        // 检查音频时长条件
        if (audioDurationSeconds < config.getMinAudioDuration()) {
            log.debug("音频时长{}秒，未达到纠错最小时长{}秒", audioDurationSeconds, config.getMinAudioDuration());
            return false;
        }
        
        // 检查文本长度条件
        if (textLength < config.getMinTextLength()) {
            log.debug("文本长度{}字符，未达到纠错最小长度{}字符", textLength, config.getMinTextLength());
            return false;
        }
        
        log.info("满足文本纠错条件 - 音频时长: {}秒, 文本长度: {}字符", audioDurationSeconds, textLength);
        return true;
    }
    
    /**
     * 使用AI大模型纠正文本
     */
    private String correctTextWithAiModel(String originalText) {
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        SpeechRecognitionProperties.TextCorrectionConfig correctionConfig = speechProperties.getTextCorrection();
        
        // 检查AI服务是否可用
        if (!dashscopeConfig.getEnabled() || !StringUtils.hasText(dashscopeConfig.getApiKey())) {
            log.warn("AI服务未启用或API Key未配置，跳过文本纠错");
            return originalText;
        }
        
        try {
            // 构建消息
            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(correctionConfig.getSystemPrompt())
                    .build();
                    
            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content("请纠正以下语音识别文本：\n\n" + originalText)
                    .build();
            
            // 动态计算合适的maxTokens
            int dynamicMaxTokens = calculateOptimalMaxTokens(originalText, correctionConfig.getMaxTokens());
            
            // 构建生成参数
            GenerationParam param = GenerationParam.builder()
                    .apiKey(dashscopeConfig.getApiKey())
                    .model(correctionConfig.getModel())
                    .messages(Arrays.asList(systemMessage, userMessage))
                    .maxTokens(dynamicMaxTokens)
                    .temperature(correctionConfig.getTemperature().floatValue())
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
                    
            log.debug("动态设置maxTokens: {} (配置值: {}, 文本长度: {})", 
                    dynamicMaxTokens, correctionConfig.getMaxTokens(), originalText.length());
            
            log.debug("调用AI大模型纠正文本，模型: {}", correctionConfig.getModel());
            
            // 调用SDK
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            if (result.getOutput() == null || result.getOutput().getChoices() == null || result.getOutput().getChoices().isEmpty()) {
                log.error("AI大模型响应异常 - 模型: {}, result={}", correctionConfig.getModel(), result);
                return originalText;
            }
            
            String correctedText = result.getOutput().getChoices().get(0).getMessage().getContent().trim();
            
            log.info("AI文本纠错完成 - 模型: {}, 原始长度: {}, 纠正后长度: {}", 
                    correctionConfig.getModel(), originalText.length(), correctedText.length());
            
            return correctedText;
            
        } catch (Exception e) {
            log.error("调用AI大模型纠错失败 - 模型: {}, 错误: {}", correctionConfig.getModel(), e.getMessage());
            return originalText; // 失败时返回原始文本
        }
    }
    
    /**
     * 估算音频时长（基于文本长度和处理时间的经验公式）
     */
    private long estimateAudioDuration(String text, long processingTimeMs) {
        // 经验估算：每分钟约产生120-200个中文字符的文本
        // 这里使用保守估计150字符/分钟
        int charCount = text.length();
        long estimatedByText = (charCount * 60L) / 150; // 秒
        
        // 另一种估算：FunASR处理时间通常是音频时长的1-3倍
        // 使用2倍作为中位数估算
        long estimatedByProcessing = processingTimeMs / 1000 / 2;
        
        // 取两种估算的平均值，但不少于30秒
        long estimated = Math.max(30, (estimatedByText + estimatedByProcessing) / 2);
        
        log.debug("音频时长估算 - 基于文本: {}秒, 基于处理时间: {}秒, 最终估算: {}秒", 
                estimatedByText, estimatedByProcessing, estimated);
        
        return estimated;
    }
    
    /**
     * 根据输入文本长度动态计算最优的maxTokens
     */
    private int calculateOptimalMaxTokens(String inputText, int configuredMaxTokens) {
        // 计算输入文本的token数（估算）
        int inputTokens = estimateTokenCount(inputText);
        
        // 为系统提示词和用户提示预留token（约200个）
        int systemPromptTokens = 200;
        
        // 输出token = 输入token * 1.2 (纠错通常会增加20%的内容：标点、格式等)
        int estimatedOutputTokens = (int) (inputTokens * 1.2);
        
        // 总需求 = 输入 + 系统提示 + 输出
        int totalNeeded = inputTokens + systemPromptTokens + estimatedOutputTokens;
        
        // 添加20%的安全边距
        int safeMaxTokens = (int) (totalNeeded * 1.2);
        
        // 确保不小于配置值，但也不超过模型限制（qwen-plus最大32k）
        int finalMaxTokens = Math.max(
            Math.min(safeMaxTokens, 32000), // 不超过32k
            Math.max(configuredMaxTokens, 4000) // 不小于配置值或4k
        );
        
        log.debug("Token计算 - 输入: {}, 估算输出: {}, 最终设置: {} (配置: {})", 
                inputTokens, estimatedOutputTokens, finalMaxTokens, configuredMaxTokens);
        
        return finalMaxTokens;
    }
    
    /**
     * 估算文本的token数量
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 中文：1个字符 ≈ 1.5个token
        // 英文：1个字符 ≈ 0.25个token  
        // 混合文本采用保守估计：1个字符 ≈ 1个token
        return text.length();
    }
    
    /**
     * 格式化打印性能统计信息
     */
    private void printPerformanceStatistics(String filename, String fileId, int textLength, 
            long estimatedAudioDuration, long speechRecognitionDuration, long textCorrectionDuration, 
            long totalDuration, boolean correctionApplied) {
        
        log.info("===== 语音识别性能统计 =====");
        log.info("文件信息:");
        log.info("  ├─ 文件名: {}", filename);
        log.info("  ├─ 文件ID: {}", fileId);
        log.info("  ├─ 文本长度: {} 字符", textLength);
        log.info("  └─ 预估音频时长: {}", DurationFormatUtils.formatDurationHMS(estimatedAudioDuration * 1000));
        log.info("");
        log.info("耗时统计:");
        log.info("  ├─ 语音识别耗时: {}", DurationFormatUtils.formatDurationHMS(speechRecognitionDuration));
        if (correctionApplied) {
            log.info("  ├─ 文本纠错耗时: {}", DurationFormatUtils.formatDurationHMS(textCorrectionDuration));
            log.info("  └─ 总耗时: {}", DurationFormatUtils.formatDurationHMS(totalDuration));
        } else {
            log.info("  ├─ 文本纠错: 未触发或跳过");
            log.info("  └─ 总耗时: {}", DurationFormatUtils.formatDurationHMS(totalDuration));
        }
        log.info("");
        log.info("处理效率分析:");
        double recognitionRatio = (double) estimatedAudioDuration * 1000 / speechRecognitionDuration;
        log.info("  ├─ 识别效率: {}x ({})", String.format("%.1f", recognitionRatio), getEfficiencyDescription(recognitionRatio));
        if (correctionApplied) {
            double totalRatio = (double) estimatedAudioDuration * 1000 / totalDuration;
            log.info("  └─ 整体效率: {}x (含纠错, {})", String.format("%.1f", totalRatio), getEfficiencyDescription(totalRatio));
        } else {
            log.info("  └─ 整体效率: {}x (仅识别, {})", String.format("%.1f", recognitionRatio), getEfficiencyDescription(recognitionRatio));
        }
        log.info("=============================");
    }
    
    
    /**
     * 获取处理效率描述
     */
    private String getEfficiencyDescription(double ratio) {
        if (ratio >= 3.0) {
            return "很快，比实时快" + String.format("%.1f", ratio) + "倍";
        } else if (ratio >= 2.0) {
            return "较快，比实时快" + String.format("%.1f", ratio) + "倍";
        } else if (ratio >= 1.0) {
            return "正常，比实时快" + String.format("%.1f", ratio) + "倍";
        } else if (ratio >= 0.5) {
            return "较慢，比实时慢" + String.format("%.1f", 1.0/ratio) + "倍";
        } else {
            return "很慢，比实时慢" + String.format("%.1f", 1.0/ratio) + "倍";
        }
    }


}