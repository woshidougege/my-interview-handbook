package com.noah.superagent.ai.service;

import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import java.util.function.Consumer;

/**
 * 语音识别服务接口
 * 基于阿里云百炼Paraformer实时语音识别
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public interface SpeechRecognitionService {

    /**
     * 开始语音识别会话
     * 
     * @param sessionId 会话ID
     * @param resultCallback 实时识别结果回调
     * @return 是否成功开始会话
     */
    boolean startRecognitionSession(String sessionId, Consumer<SpeechRecognitionResponse> resultCallback);

    /**
     * 发送音频数据进行识别
     * 
     * @param sessionId 会话ID
     * @param audioData 音频数据（二进制）
     * @return 是否成功发送
     */
    boolean sendAudioData(String sessionId, byte[] audioData);

    /**
     * 结束语音识别会话
     * 
     * @param sessionId 会话ID
     */
    void endRecognitionSession(String sessionId);
}
