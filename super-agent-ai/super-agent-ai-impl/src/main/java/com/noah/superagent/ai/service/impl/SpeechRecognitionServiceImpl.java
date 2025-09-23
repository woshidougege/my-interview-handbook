package com.noah.superagent.ai.service.impl;

import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
            // 当前版本暂不支持文件上传识别，需要集成OSS等云存储服务
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("语音识别功能暂时不可用，需要配置文件上传到公网可访问的存储服务")
                    .isFinal(true)
                    .build();
            
        } finally {
            // 清理临时文件
            cleanupTemporaryFile(tempFilePath);
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
}
