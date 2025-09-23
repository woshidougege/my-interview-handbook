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
import org.apache.tika.mime.MediaType;
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
import java.util.List;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.apache.tika.Tika;

/**
 * 语音识别服务实现类
 * 基于阿里云百炼Gummy实时语音识别API
 * 
 * 注意：临时文件不会自动删除，需要定期手动清理temp/uploads目录
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
    
    /**
     * Apache Tika实例，用于检测文件真实格式
     */
    private final Tika tika = new Tika();

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
            
            // 检测真实的音频格式
            String realFormat = detectAudioFormat(tempFilePath);
            String fileExtension = getFileExtension(filename);
            
            if (realFormat == null) {
                // 如果无法检测，回退到文件扩展名
                realFormat = fileExtension;
                log.warn("无法检测音频文件真实格式，使用文件扩展名: {}", realFormat);
            } else {
                log.info("检测到音频文件真实格式: {} (文件名扩展名: {})", realFormat, fileExtension);
            }
            
            // 首先尝试使用检测到的格式
            try {
                return recognizeAudioFile(tempFilePath, realFormat, language);
            } catch (Exception e) {
                log.warn("使用检测格式 {} 失败: {}", realFormat, e.getMessage());
                
                // 智能回退策略
                List<String> fallbackFormats = getFallbackFormats(realFormat, fileExtension);
                
                for (String fallbackFormat : fallbackFormats) {
                    log.warn("尝试回退格式: {}", fallbackFormat);
                    try {
                        return recognizeAudioFile(tempFilePath, fallbackFormat, language);
                    } catch (Exception fallbackException) {
                        log.warn("回退格式 {} 也失败: {}", fallbackFormat, fallbackException.getMessage());
                    }
                }
                
                log.error("所有格式都失败，抛出原始异常");
                throw e; // 所有回退都失败，抛出原始异常
            }
            
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
     * 使用Gummy实时语音识别API识别音频文件
     */
    private SpeechRecognitionResponse recognizeAudioFile(String filePath, String format, String language) {
        log.info("使用Gummy API识别音频文件: {}, 格式: {}, 语言: {}", filePath, format, language);
        
        TranslationRecognizerRealtime translator = null;
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
            translator = new TranslationRecognizerRealtime();
            
            // 使用文件调用API
            log.info("开始语音识别...");
            TranslationRecognizerResultPack result = translator.call(param, new File(filePath));
            
            // 处理识别结果
            return parseTranslationResult(result);
            
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("识别失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        } finally {
            // 确保连接被正确关闭，释放文件句柄
            if (translator != null) {
                try {
                    translator.getDuplexApi().close(1000, "bye");
                    log.debug("API连接已关闭，文件句柄已释放");
                } catch (Exception e) {
                    log.warn("关闭API连接失败", e);
                }
            }
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
            
            log.info("临时文件保存成功: {} (注意：临时文件不会自动删除，需要手动清理)", tempFilePath.toAbsolutePath());
            return tempFilePath.toString();
            
        } catch (IOException e) {
            log.error("保存临时文件失败", e);
            return null;
        }
    }
    
    /**
     * 检测音频文件的真实格式
     * 使用Apache Tika基于文件头检测，不依赖文件扩展名
     */
    private String detectAudioFormat(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("文件不存在: {}", filePath);
                return null;
            }
            
            // 记录文件基本信息
            long fileSize = file.length();
            log.info("音频文件信息: 路径={}, 大小={} bytes ({} KB)", 
                    filePath, fileSize, fileSize / 1024);
            
            // 使用Tika检测MIME类型
            MediaType mediaType = MediaType.parse(tika.detect(file));
            log.info("检测到完整MIME类型: {} (主类型:{}, 子类型:{})", 
                    mediaType, mediaType.getType(), mediaType.getSubtype());
            
            // 记录MIME类型的参数信息（如果有的话）
            if (!mediaType.getParameters().isEmpty()) {
                log.info("MIME类型参数: {}", mediaType.getParameters());
            }
            
            // 将MediaType转换为我们支持的格式
            String format = mediaTypeToFormat(mediaType);
            
            // 验证格式是否被支持
            if (format != null && speechProperties.getSupportedFormats().contains(format)) {
                log.info("格式映射成功: {} -> {}", mediaType.getSubtype(), format);
                return format;
            } else {
                log.warn("检测到不支持的音频格式: {} (MIME: {}, 支持的格式: {})", 
                        format, mediaType, speechProperties.getSupportedFormats());
                return null;
            }
            
        } catch (Exception e) {
            log.error("检测音频格式失败: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 将MediaType转换为我们支持的音频格式代码
     * 使用类型安全的MediaType，避免字符串比较
     */
    private String mediaTypeToFormat(MediaType mediaType) {
        if (mediaType == null) {
            return null;
        }
        
        // 获取主类型和子类型进行匹配
        String type = mediaType.getType();
        String subtype = mediaType.getSubtype();
        
        // 只处理音频类型
        if (!"audio".equals(type)) {
            log.debug("非音频类型: {}", mediaType);
            return null;
        }
        
        // 根据子类型映射到我们支持的格式
        switch (subtype.toLowerCase()) {
            case "wav":
            case "wave":
            case "x-wav":
            case "vnd.wave":  // 添加对audio/vnd.wave的支持
                return "wav";
                
            case "mpeg":
            case "mp3":
                return "mp3";
                
            case "mp4":
            case "m4a":
                // MP4/M4A格式的智能处理
                log.info("检测到MP4/M4A容器格式，尝试多种格式兼容");
                return "aac"; // 优先尝试AAC
                
            case "aac":
                return "aac";
                
            case "ogg":
            case "opus":
                return "opus";
                
            case "x-speex":
                return "speex";
                
            case "amr":
                return "amr";
                
            case "basic":
            case "l16":
            case "pcm":
                return "pcm";
                
            default:
                log.debug("未映射的音频子类型: {} (完整类型: {})", subtype, mediaType);
                return null;
        }
    }
    
    /**
     * 获取智能回退格式列表
     * 根据检测到的格式和文件扩展名，提供合理的回退选项
     */
    private List<String> getFallbackFormats(String detectedFormat, String fileExtension) {
        List<String> fallbacks = new ArrayList<>();
        
        // 如果检测格式与文件扩展名不同，优先尝试文件扩展名
        if (!detectedFormat.equals(fileExtension)) {
            fallbacks.add(fileExtension);
        }
        
        // 根据检测到的格式添加特定的回退策略
        switch (detectedFormat) {
            case "aac":
                // AAC失败时，尝试MP3和MP4
                if (!"mp3".equals(fileExtension)) fallbacks.add("mp3");
                if (!"mp4".equals(fileExtension)) fallbacks.add("mp4");
                break;
                
            case "mp3":
                // MP3失败时，尝试AAC（可能是MP4容器）
                if (!"aac".equals(fileExtension)) fallbacks.add("aac");
                break;
                
            case "wav":
                // WAV失败时，尝试PCM
                if (!"pcm".equals(fileExtension)) fallbacks.add("pcm");
                break;
                
            case "opus":
                // Opus失败时，尝试OGG
                if (!"ogg".equals(fileExtension)) fallbacks.add("ogg");
                break;
        }
        
        // 通用回退：如果都失败，尝试最常见的格式
        for (String commonFormat : Arrays.asList("mp3", "wav", "aac")) {
            if (!commonFormat.equals(detectedFormat) && 
                !commonFormat.equals(fileExtension) && 
                !fallbacks.contains(commonFormat)) {
                fallbacks.add(commonFormat);
            }
        }
        
        log.info("为格式 {} (扩展名: {}) 生成回退策略: {}", detectedFormat, fileExtension, fallbacks);
        return fallbacks;
    }
    
}
