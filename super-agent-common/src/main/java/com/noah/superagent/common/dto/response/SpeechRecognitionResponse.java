package com.noah.superagent.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 语音识别响应
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionResponse {

    /**
     * 识别状态
     */
    private RecognitionStatus status;

    /**
     * 识别的文本内容
     */
    private String text;

    /**
     * 是否为最终结果
     */
    private Boolean isFinal;

    /**
     * 置信度 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 句子信息（包含时间戳等详细信息）
     */
    private List<SentenceInfo> sentences;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 识别状态枚举
     */
    public enum RecognitionStatus {
        /**
         * 识别中（中间结果）
         */
        RECOGNIZING,
        
        /**
         * 识别完成（最终结果）
         */
        COMPLETED,
        
        /**
         * 识别失败
         */
        FAILED,
        
        /**
         * 会话开始
         */
        SESSION_STARTED,
        
        /**
         * 会话结束
         */
        SESSION_ENDED
    }

    /**
     * 句子信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentenceInfo {
        /**
         * 句子开始时间（毫秒）
         */
        private Long beginTime;

        /**
         * 句子结束时间（毫秒）
         */
        private Long endTime;

        /**
         * 句子文本
         */
        private String text;

        /**
         * 是否为句子结尾
         */
        private Boolean isSentenceEnd;

        /**
         * 情感标签（仅8k-v2模型支持）
         */
        private String emotionTag;

        /**
         * 情感置信度（仅8k-v2模型支持）
         */
        private Double emotionConfidence;

        /**
         * 字级别时间戳信息
         */
        private List<WordInfo> words;
    }

    /**
     * 字级别信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordInfo {
        /**
         * 字开始时间（毫秒）
         */
        private Long beginTime;

        /**
         * 字结束时间（毫秒）
         */
        private Long endTime;

        /**
         * 字符
         */
        private String text;

        /**
         * 标点符号
         */
        private String punctuation;
    }
}
