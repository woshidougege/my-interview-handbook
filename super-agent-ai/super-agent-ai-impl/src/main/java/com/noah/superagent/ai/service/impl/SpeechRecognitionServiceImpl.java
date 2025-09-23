package com.noah.superagent.ai.service.impl;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionTaskResult;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 语音识别服务实现类
 * 基于阿里云百炼SenseVoice录音文件识别
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionServiceImpl implements SpeechRecognitionService {

    private final AiProperties aiProperties;
    
    /**
     * 临时录音文件目录
     */
    private static final String TEMP_UPLOAD_DIR = "temp/uploads";
    
    /**
     * 支持的音频格式
     */
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "aac", "amr", "avi", "flac", "flv", "m4a", "mkv", "mov", 
            "mp3", "mp4", "mpeg", "ogg", "opus", "wav", "webm", "wma", "wmv"
    );

    @PostConstruct
    public void init() {
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        
        String apiKey = dashscopeConfig.getApiKey();
        log.info("读取到的API Key值: {}", apiKey != null ? "sk-****" + apiKey.substring(Math.max(0, apiKey.length() - 6)) : "null");
        
        // 检查API Key配置
        if (!StringUtils.hasText(apiKey)) {
            log.warn("阿里云百炼API Key未配置，语音识别服务将无法正常工作");
            return;
        }

        // 初始化DashScope
        try {
            System.setProperty("dashscope.api.key", apiKey);
            log.info("设置系统属性 dashscope.api.key 成功");
            log.info("验证系统属性值: {}", System.getProperty("dashscope.api.key") != null ? "已设置" : "未设置");
            log.info("语音识别服务初始化完成 - 模型: sensevoice-v1");
            
            // 确保上传目录存在
            createUploadDirectory();
        } catch (Exception e) {
            log.error("语音识别服务初始化失败", e);
        }
    }

    /**
     * 创建上传目录
     */
    private void createUploadDirectory() {
        try {
            Path uploadPath = Paths.get(TEMP_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("创建上传目录: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建上传目录失败", e);
        }
    }

    @Override
    public SpeechRecognitionResponse recognizeAudioStream(InputStream audioStream, String filename, String language) {
        log.info("开始识别录音文件: {}, 语言: {}", filename, language);
        
        // 检查API Key配置
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        String apiKey = dashscopeConfig.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.error("阿里云百炼API Key未配置");
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("API Key未配置")
                    .isFinal(true)
                    .build();
        }

        // 验证文件格式
        if (filename == null || !isValidAudioFormat(filename)) {
            log.error("不支持的音频格式: {}", filename);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("不支持的音频格式，支持的格式: " + String.join(", ", SUPPORTED_FORMATS))
                    .isFinal(true)
                    .build();
        }

        // 保存临时文件
        String tempFilePath = saveTemporaryFile(audioStream, filename);
        if (tempFilePath == null) {
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("保存临时文件失败")
                    .isFinal(true)
                    .build();
        }

        try {
            // TODO: 实际项目中需要将文件上传到公网可访问的URL（如OSS）
            // 这里暂时用本地文件路径示例
            String publicUrl = convertToPublicUrl(tempFilePath);
            if (publicUrl == null) {
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                        .errorMessage("无法生成公网访问URL，请配置文件上传服务")
                        .isFinal(true)
                        .build();
            }
            
            // 调用百炼识别服务
            return recognizeAudioUrl(publicUrl, language);
            
        } finally {
            // 清理临时文件
            cleanupTemporaryFile(tempFilePath);
        }
    }

    @Override
    public SpeechRecognitionResponse recognizeAudioUrl(String audioFileUrl, String language) {
        log.info("通过URL识别录音文件: {}, 语言: {}", audioFileUrl, language);
        
        try {
            // 确保API Key已设置
            System.setProperty("dashscope.api.key", aiProperties.getAlibabaDashscope().getApiKey());
            
            // 构建识别参数
            TranscriptionParam param;
            if (StringUtils.hasText(language)) {
                param = TranscriptionParam.builder()
                        .model("sensevoice-v1")
                        .fileUrls(Arrays.asList(audioFileUrl))
                        .parameter("language_hints", new String[]{language})
                        .build();
            } else {
                param = TranscriptionParam.builder()
                        .model("sensevoice-v1")
                        .fileUrls(Arrays.asList(audioFileUrl))
                        .build();
            }
            
            // 创建转录实例
            Transcription transcription = new Transcription();
            
            // 异步提交任务
            log.info("提交语音识别任务...");
            TranscriptionResult result = transcription.asyncCall(param);
            
            if (result == null) {
                throw new RuntimeException("提交识别任务失败");
            }
            
            log.info("识别任务已提交，任务ID: {}", result.getTaskId());
            
            // 同步等待任务完成
            log.info("等待识别任务完成...");
            TranscriptionQueryParam queryParam = TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId());
            result = transcription.wait(queryParam);
            
            // 处理识别结果
            return parseTranscriptionResult(result);
            
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("识别失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        }
    }


    /**
     * 验证音频文件格式
     */
    private boolean isValidAudioFormat(String filename) {
        if (filename == null) return false;
        
        String extension = getFileExtension(filename).toLowerCase();
        return SUPPORTED_FORMATS.contains(extension);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 保存临时文件
     */
    private String saveTemporaryFile(InputStream audioStream, String filename) {
        try {
            if (filename == null) {
                filename = "audio.webm";
            }
            
            // 生成唯一的文件名
            String uuid = UUID.randomUUID().toString();
            String extension = getFileExtension(filename);
            String tempFileName = "audio_" + uuid + "." + extension;
            
            Path tempFilePath = Paths.get(TEMP_UPLOAD_DIR, tempFileName);
            
            // 保存文件
            Files.copy(audioStream, tempFilePath);
            
            log.info("临时文件保存成功: {}", tempFilePath.toAbsolutePath());
            return tempFilePath.toString();
            
        } catch (IOException e) {
            log.error("保存临时文件失败", e);
            return null;
        }
    }
    
    /**
     * 转换为公网可访问的URL
     * TODO: 实际项目中需要上传到OSS等云存储服务
     */
    private String convertToPublicUrl(String localFilePath) {
        // 目前返回null，表示需要实现文件上传服务
        // 在实际项目中，这里应该：
        // 1. 将文件上传到阿里云OSS等云存储
        // 2. 返回公网可访问的URL
        log.warn("需要实现文件上传到公网可访问的URL，当前返回null");
        return null;
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTemporaryFile(String filePath) {
        if (filePath == null) return;
        
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.debug("清理临时文件: {}", filePath);
        } catch (IOException e) {
            log.warn("清理临时文件失败: {}", filePath, e);
        }
    }
    
    /**
     * 解析转录结果
     */
    private SpeechRecognitionResponse parseTranscriptionResult(TranscriptionResult result) {
        try {
            if (result.getResults() == null || result.getResults().isEmpty()) {
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                        .errorMessage("未获取到识别结果")
                        .isFinal(true)
                        .build();
            }
            
            TranscriptionTaskResult taskResult = result.getResults().get(0);
            String transcriptionUrl = taskResult.getTranscriptionUrl();
            
            if (!StringUtils.hasText(transcriptionUrl)) {
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                        .errorMessage("未获取到转录结果URL")
                        .isFinal(true)
                        .build();
            }
            
            // 获取转录结果内容
            String recognizedText = fetchTranscriptionContent(transcriptionUrl);
            
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                    .text(recognizedText)
                    .isFinal(true)
                    .requestId(result.getRequestId())
                    .build();
                    
        } catch (Exception e) {
            log.error("解析转录结果失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("解析结果失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        }
    }
    
    /**
     * 获取转录结果内容
     */
    private String fetchTranscriptionContent(String transcriptionUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(transcriptionUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            
            // 解析JSON结果，提取识别文本
            Gson gson = new Gson();
            JsonObject jsonResult = gson.fromJson(content.toString(), JsonObject.class);
            
            // 根据百炼SenseVoice的返回格式提取文本
            if (jsonResult.has("output") && jsonResult.getAsJsonObject("output").has("text")) {
                return jsonResult.getAsJsonObject("output").get("text").getAsString();
            } else {
                // 如果格式不符合预期，返回原始内容
                log.warn("转录结果格式不符合预期，返回原始内容");
                return content.toString();
            }
        }
    }
}
