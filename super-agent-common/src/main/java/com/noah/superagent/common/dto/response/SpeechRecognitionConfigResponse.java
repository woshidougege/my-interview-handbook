package com.noah.superagent.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 语音识别配置响应
 * 提供给前端的语音识别可用配置信息
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionConfigResponse {

    /**
     * 支持的音频格式
     */
    private List<AudioFormat> supportedFormats;

    /**
     * 支持的源语言（识别语言）
     */
    private List<Language> supportedSourceLanguages;

    /**
     * 支持的翻译目标语言
     */
    private List<Language> supportedTranslationLanguages;

    /**
     * 默认配置
     */
    private DefaultSettings defaults;

    /**
     * 参数限制
     */
    private ParameterLimits limits;

    /**
     * 音频格式信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioFormat {
        /**
         * 格式代码
         */
        private String code;

        /**
         * 格式显示名称
         */
        private String name;

        /**
         * 格式说明
         */
        private String description;
    }

    /**
     * 语言信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Language {
        /**
         * 语言代码
         */
        private String code;

        /**
         * 语言显示名称
         */
        private String name;
    }

    /**
     * 默认设置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultSettings {
        /**
         * 默认采样率
         */
        private Integer sampleRate;

        /**
         * 默认源语言
         */
        private String sourceLanguage;

        /**
         * 是否启用转录功能
         */
        private Boolean transcriptionEnabled;

        /**
         * 是否启用翻译功能
         */
        private Boolean translationEnabled;

        /**
         * VAD断句静音时长阈值（毫秒）
         */
        private Integer maxEndSilence;
    }

    /**
     * 参数限制
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterLimits {
        /**
         * 最小采样率
         */
        private Integer minSampleRate;

        /**
         * 最大采样率
         */
        private Integer maxSampleRate;

        /**
         * 最小静音时长（毫秒）
         */
        private Integer minEndSilence;

        /**
         * 最大静音时长（毫秒）
         */
        private Integer maxEndSilence;

        /**
         * 最大文件大小（MB）
         */
        private Integer maxFileSizeMB;

        /**
         * 最大录音时长（秒）
         */
        private Integer maxRecordingDurationSeconds;
    }
}
