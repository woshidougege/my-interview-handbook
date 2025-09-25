package com.noah.superagent.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.SpeechRecognitionProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.Duration;

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
        
        try {
            // 直接流式调用FunASR服务进行识别
            String result = transcribeByStream(audioStream, filename);
            
            // 生成文件ID
            String fileId = IdUtil.getSnowflakeNextIdStr();
            
            log.info("语音识别成功 - 文件ID: {}, 文本长度: {}字符", fileId, result.length());
            
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                    .text(result)
                    .isFinal(true)
                    .fileId(fileId)
                    .build();
            
        } catch (Exception e) {
            log.error("语音识别失败", e);
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
                if (resultArray != null && resultArray.isArray() && resultArray.size() > 0) {
                    JsonNode firstResult = resultArray.get(0);
                    return firstResult.get("text").asText();
                }
            }
            
            // 处理简单文本格式: {"text": "..."}
            if (jsonNode.has("text")) {
                return jsonNode.get("text").asText();
            }
            
            // 处理数组格式: [{"text": "..."}]
            if (jsonNode.isArray() && jsonNode.size() > 0) {
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
     * 健康检查
     */
    public boolean checkHealth() {
        try {
            webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("FunASR健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}