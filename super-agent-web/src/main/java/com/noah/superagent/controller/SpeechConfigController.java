package com.noah.superagent.controller;

import com.noah.superagent.common.config.SpeechRecognitionProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionConfigResponse;
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 语音识别配置控制器
 * 提供语音识别相关的配置信息给前端
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/speech/config")
@RequiredArgsConstructor
@Tag(name = "语音识别配置", description = "语音识别配置相关接口")
public class SpeechConfigController {

    private final SpeechRecognitionProperties speechProperties;

    /**
     * 获取语音识别配置信息
     * 返回前端需要的所有语音识别配置，包括支持的格式、语言等
     */
    @Operation(summary = "获取语音识别配置", description = "获取语音识别支持的格式、语言和默认配置等信息")
    @GetMapping("/recognition")
    public ApiResponse<SpeechRecognitionConfigResponse> getRecognitionConfig() {
        log.debug("获取语音识别配置信息");

        try {
            SpeechRecognitionConfigResponse config = buildConfigResponse();
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取语音识别配置失败", e);
            return ApiResponse.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 构建配置响应对象
     */
    private SpeechRecognitionConfigResponse buildConfigResponse() {
        return SpeechRecognitionConfigResponse.builder()
                .supportedFormats(buildAudioFormats())
                .supportedSourceLanguages(buildSourceLanguages())
                .supportedTranslationLanguages(buildTranslationLanguages())
                .defaults(buildDefaultSettings())
                .limits(buildParameterLimits())
                .build();
    }

    /**
     * 构建音频格式列表
     */
    private java.util.List<SpeechRecognitionConfigResponse.AudioFormat> buildAudioFormats() {
        Map<String, String> formatNames = speechProperties.getFormatNames();
        Map<String, String> formatDescriptions = speechProperties.getFormatDescriptions();

        return speechProperties.getSupportedFormats().stream()
                .map(format -> SpeechRecognitionConfigResponse.AudioFormat.builder()
                        .code(format)
                        .name(formatNames.getOrDefault(format, format.toUpperCase()))
                        .description(formatDescriptions.getOrDefault(format, ""))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建源语言列表
     */
    private java.util.List<SpeechRecognitionConfigResponse.Language> buildSourceLanguages() {
        Map<String, String> languageNames = speechProperties.getLanguageNames();

        return speechProperties.getSupportedSourceLanguages().stream()
                .map(lang -> SpeechRecognitionConfigResponse.Language.builder()
                        .code(lang)
                        .name(languageNames.getOrDefault(lang, lang))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建翻译目标语言列表
     */
    private java.util.List<SpeechRecognitionConfigResponse.Language> buildTranslationLanguages() {
        Map<String, String> languageNames = speechProperties.getLanguageNames();

        return speechProperties.getSupportedTranslationLanguages().stream()
                .map(lang -> SpeechRecognitionConfigResponse.Language.builder()
                        .code(lang)
                        .name(languageNames.getOrDefault(lang, lang))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建默认设置
     */
    private SpeechRecognitionConfigResponse.DefaultSettings buildDefaultSettings() {
        SpeechRecognitionProperties.DefaultConfig defaults = speechProperties.getDefaults();

        return SpeechRecognitionConfigResponse.DefaultSettings.builder()
                .sampleRate(speechProperties.getDefaultSampleRate())
                .sourceLanguage(defaults.getSourceLanguage())
                .transcriptionEnabled(defaults.getTranscriptionEnabled())
                .translationEnabled(defaults.getTranslationEnabled())
                .maxEndSilence(defaults.getMaxEndSilence())
                .build();
    }

    /**
     * 构建参数限制
     */
    private SpeechRecognitionConfigResponse.ParameterLimits buildParameterLimits() {
        SpeechRecognitionProperties.DefaultConfig defaults = speechProperties.getDefaults();
        SpeechRecognitionProperties.LimitsConfig limits = speechProperties.getLimits();

        return SpeechRecognitionConfigResponse.ParameterLimits.builder()
                .minSampleRate(limits.getMinSampleRate())
                .maxSampleRate(limits.getMaxSampleRate())
                .minEndSilence(defaults.getMinEndSilence())
                .maxEndSilence(defaults.getMaxEndSilenceLimit())
                .maxFileSizeMB(limits.getMaxFileSizeMB())
                .maxRecordingDurationSeconds(limits.getMaxRecordingDurationSeconds())
                .build();
    }

}
