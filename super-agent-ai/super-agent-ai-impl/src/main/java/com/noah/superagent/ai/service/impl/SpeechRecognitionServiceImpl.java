package com.noah.superagent.ai.service.impl;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResultPack;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
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
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.apache.tika.Tika;

/**
 * 语音识别服务实现类
 * 支持阿里云百炼Gummy实时语音识别API和Fun-ASR录音文件识别API
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
     * 公网文件发布相关常量
     */
    private static final String PUBLIC_DOMAIN = "https://unparrying-elizbeth-chylophyllous.ngrok-free.app";
    private static final String PUBLIC_DIR = "temp/public";
    private static final String URL_PREFIX = "/files";
    
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

        try {
            // 确保API Key已设置
            System.setProperty("dashscope.api.key", apiKey);
            
            // 检查使用的模型类型
            String model = speechProperties.getModel();
            log.info("使用语音识别模型: {}", model);
            
            if (model.startsWith("fun-asr")) {
                // 使用Fun-ASR模型（需要公网URL）
                return recognizeWithFunAsr(audioStream, filename, language);
            } else {
                // 使用传统的Gummy模型（本地文件）
                return recognizeWithGummy(audioStream, filename, language);
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
     * 使用Fun-ASR模型识别音频文件（通过公网URL）
     */
    private SpeechRecognitionResponse recognizeWithFunAsr(InputStream audioStream, String filename, String language) {
        log.info("使用Fun-ASR模型识别音频文件: {}", filename);
        
        try {
            // 1. 发布文件到公网
            String publicUrl = publishFileToPublic(audioStream, filename);
            log.info("文件已发布到公网: {}", publicUrl);
            
            // 2. 使用Fun-ASR API识别
            TranscriptionParam param = TranscriptionParam.builder()
                    .model(speechProperties.getModel())
                    .fileUrls(java.util.Arrays.asList(publicUrl))
                    .build();
            
            Transcription transcription = new Transcription();
            
            // 异步提交任务
            com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult result = transcription.asyncCall(param);
            log.info("Fun-ASR任务提交成功，任务ID: {}", result.getTaskId());
            
            // 同步等待结果
            com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult finalResult = transcription.wait(
                    TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId())
            );
            log.info("Fun-ASR识别完成，状态: {}", finalResult.getTaskStatus());
            
            // 解析结果
            return parseFunAsrResult(finalResult);
            
        } catch (Exception e) {
            log.error("Fun-ASR识别失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("Fun-ASR识别失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        }
    }
    
    /**
     * 使用Gummy实时语音识别API识别音频文件（本地文件）
     */
    private SpeechRecognitionResponse recognizeWithGummy(InputStream audioStream, String filename, String language) {
        log.info("使用Gummy模型识别音频文件: {}", filename);
        
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
            // 检测真实的音频格式
            String realFormat = detectAudioFormat(tempFilePath);
            String fileExtension = getFileExtension(filename);
            
            if (realFormat == null) {
                realFormat = fileExtension;
                log.warn("无法检测音频文件真实格式，使用文件扩展名: {}", realFormat);
            } else {
                log.info("检测到音频文件真实格式: {} (文件名扩展名: {})", realFormat, fileExtension);
            }
            
            return recognizeGummyFile(tempFilePath, realFormat, language);
            
        } catch (Exception e) {
            log.error("Gummy识别失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("Gummy识别失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        }
    }
    
    /**
     * 使用Gummy实时语音识别API识别音频文件
     */
    private SpeechRecognitionResponse recognizeGummyFile(String filePath, String format, String language) {
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
    
    /**
     * 解析Fun-ASR识别结果
     */
    private SpeechRecognitionResponse parseFunAsrResult(com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult result) {
        if (result == null) {
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("Fun-ASR识别结果为空")
                    .isFinal(true)
                    .build();
        }
        
        try {
            log.info("Fun-ASR任务状态: {}", result.getTaskStatus());
            
            // 检查任务状态
            if (!"SUCCEEDED".equals(result.getTaskStatus())) {
                String errorMsg = "Fun-ASR任务失败，状态: " + result.getTaskStatus();
                log.error(errorMsg);
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                        .errorMessage(errorMsg)
                        .isFinal(true)
                        .build();
            }
            
            // 从输出中提取识别文本
            Object output = result.getOutput();
            if (output == null) {
                log.warn("Fun-ASR输出为空");
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                        .text("未识别到语音内容")
                        .isFinal(true)
                        .build();
            }
            
            // 输出转换为字符串并提取文本
            String outputStr = output.toString();
            log.info("Fun-ASR原始输出: {}", outputStr);
            
            // 这里可以根据实际输出格式进行解析
            // Fun-ASR的输出通常包含识别的文本内容
            String recognizedText = extractTextFromFunAsrOutput(outputStr);
            
            if (recognizedText == null || recognizedText.trim().isEmpty()) {
                return SpeechRecognitionResponse.builder()
                        .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                        .text("未识别到语音内容")
                        .isFinal(true)
                        .build();
            }
            
            log.info("Fun-ASR识别成功: {}", recognizedText);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                    .text(recognizedText.trim())
                    .isFinal(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析Fun-ASR结果失败", e);
            return SpeechRecognitionResponse.builder()
                    .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                    .errorMessage("解析识别结果失败: " + e.getMessage())
                    .isFinal(true)
                    .build();
        }
    }
    
    /**
     * 从Fun-ASR输出中提取文本内容
     */
    private String extractTextFromFunAsrOutput(String output) {
        try {
            // Fun-ASR的输出是JSON格式，需要解析
            // 这里做简单的文本提取，可以根据实际格式优化
            if (output.contains("\"text\"")) {
                // 提取JSON中的text字段
                int textStart = output.indexOf("\"text\"");
                if (textStart >= 0) {
                    int colonIndex = output.indexOf(":", textStart);
                    if (colonIndex >= 0) {
                        int quoteStart = output.indexOf("\"", colonIndex);
                        if (quoteStart >= 0) {
                            int quoteEnd = output.indexOf("\"", quoteStart + 1);
                            if (quoteEnd >= 0) {
                                return output.substring(quoteStart + 1, quoteEnd);
                            }
                        }
                    }
                }
            }
            
            // 如果没有找到text字段，返回处理后的原始输出
            return output.replaceAll("[\\[\\]{}\"\\\\]", "").trim();
            
        } catch (Exception e) {
            log.warn("提取Fun-ASR文本内容失败，返回原始输出: {}", e.getMessage());
            return output;
        }
    }
    
    /**
     * 发布文件到公网目录
     */
    private String publishFileToPublic(InputStream inputStream, String filename) {
        try {
            // 确保公网目录存在
            Path publicDir = Paths.get(PUBLIC_DIR);
            if (!Files.exists(publicDir)) {
                Files.createDirectories(publicDir);
                log.info("创建公网目录: {}", publicDir.toAbsolutePath());
            }
            
            // 生成唯一文件名
            String extension = getFileExtension(filename);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String uniqueFilename = String.format("audio_%s_%s.%s", timestamp, uuid, extension);
            
            // 保存文件
            Path targetPath = publicDir.resolve(uniqueFilename);
            try (InputStream in = inputStream) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 生成公网URL
            String publicUrl = PUBLIC_DOMAIN + URL_PREFIX + "/" + uniqueFilename;
            
            log.info("文件发布成功 - 本地路径: {}, 公网URL: {}", targetPath, publicUrl);
            return publicUrl;
            
        } catch (IOException e) {
            log.error("发布文件到公网失败: {}", filename, e);
            throw new RuntimeException("发布文件失败: " + e.getMessage(), e);
        }
    }
    
}
