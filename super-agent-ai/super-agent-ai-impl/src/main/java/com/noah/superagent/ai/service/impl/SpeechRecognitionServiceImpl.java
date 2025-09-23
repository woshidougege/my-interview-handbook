package com.noah.superagent.ai.service.impl;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResultPack;
import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.common.config.SpeechRecognitionProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

/**
 * 语音识别服务实现类
 * 基于阿里云百炼Gummy实时语音识别API
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionServiceImpl implements SpeechRecognitionService {

    private final AiProperties aiProperties;
    private final SpeechRecognitionProperties speechProperties;
    
    /**
     * 临时文件目录
     */
    private static final String TEMP_UPLOAD_DIR = "temp/uploads";

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
            log.info("语音识别服务初始化完成 - 模型: {}", speechProperties.getModel());
            
            // 确保临时目录存在
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
                log.info("创建临时目录: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建临时目录失败", e);
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
        if (!isValidAudioFormat(filename)) {
            log.error("不支持的音频格式: {}", filename);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("不支持的音频格式，支持的格式: " + String.join(", ", speechProperties.getSupportedFormats()))
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
            // 确保API Key已设置
            System.setProperty("dashscope.api.key", apiKey);
            
            // 使用文件调用API
            return recognizeAudioFile(tempFilePath, getFileExtension(filename), language);
            
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("识别失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        } finally {
            // 清理临时文件
            cleanupTemporaryFile(tempFilePath);
        }
    }

    /**
     * 使用Gummy实时语音识别API识别音频文件
     */
    private SpeechRecognitionResponse recognizeAudioFile(String filePath, String format, String language) {
        log.info("使用Gummy API识别音频文件: {}, 格式: {}, 语言: {}", filePath, format, language);
        
        try {
            // 构建识别参数
            TranslationRecognizerParam param = TranslationRecognizerParam.builder()
                    .model(speechProperties.getModel())
                    .format(format.toLowerCase())
                    .sampleRate(speechProperties.getDefaultSampleRate())
                    .transcriptionEnabled(speechProperties.getDefaults().getTranscriptionEnabled())
                    .sourceLanguage(speechProperties.getDefaults().getSourceLanguage())
                    .translationEnabled(speechProperties.getDefaults().getTranslationEnabled())
                    .maxEndSilence(speechProperties.getDefaults().getMaxEndSilence())
                    .build();
            
            // 创建识别器
            TranslationRecognizerRealtime translator = new TranslationRecognizerRealtime();
            
            // 使用文件调用API
            log.info("开始语音识别...");
            TranslationRecognizerResultPack result = translator.call(param, new File(filePath));
            
            // 关闭连接
            translator.getDuplexApi().close(1000, "bye");
            
            // 处理识别结果
            return parseTranslationResult(result);
            
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
     * 解析语音识别结果
     */
    private SpeechRecognitionResponse parseTranslationResult(TranslationRecognizerResultPack result) {
        if (result == null) {
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("识别结果为空")
                    .isFinal(true)
                    .build();
        }
        
        // 检查是否有错误
        if (result.getError() != null) {
            log.error("识别出错: {}", result.getError());
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("识别失败: " + result.getError().getMessage())
                    .isFinal(true)
                    .build();
        }
        
        try {
            log.info("识别请求ID: {}", result.getRequestId());
            
            // 从转录结果中获取文本
            StringBuilder textBuilder = new StringBuilder();
            ArrayList<TranscriptionResult> transcriptionResults = result.getTranscriptionResultList();
            
            if (transcriptionResults != null && !transcriptionResults.isEmpty()) {
                for (TranscriptionResult transcriptionResult : transcriptionResults) {
                    if (transcriptionResult.getText() != null) {
                        textBuilder.append(transcriptionResult.getText());
                    }
                }
            }
            
            String text = textBuilder.toString().trim();
            log.info("识别结果: {}", text);
            
            if (text.isEmpty()) {
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                        .text("未识别到语音内容")
                        .isFinal(true)
                        .build();
            }
            
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                    .text(text)
                    .isFinal(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析识别结果失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("解析识别结果失败: " + e.getMessage())
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
        return speechProperties.getSupportedFormats().contains(extension);
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
