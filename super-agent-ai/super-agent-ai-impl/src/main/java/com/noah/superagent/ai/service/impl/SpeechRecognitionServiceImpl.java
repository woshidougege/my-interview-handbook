package com.noah.superagent.ai.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.AiProperties;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 语音识别服务实现类
 * 基于阿里云百炼Paraformer实时语音识别
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionServiceImpl implements SpeechRecognitionService {

    private final AiProperties aiProperties;
    
    /**
     * 存储活跃的识别会话
     */
    private final Map<String, RecognitionSession> activeSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
        
        // 检查API Key配置
        if (!StringUtils.hasText(dashscopeConfig.getApiKey())) {
            log.warn("阿里云百炼API Key未配置，语音识别服务将无法正常工作");
            return;
        }

        // 初始化DashScope
        try {
            System.setProperty("dashscope.api.key", dashscopeConfig.getApiKey());
            log.info("语音识别服务初始化完成 - 模型: {}", 
                    dashscopeConfig.getSpeechRecognition().getModel());
        } catch (Exception e) {
            log.error("语音识别服务初始化失败", e);
        }
    }

    @Override
    public boolean startRecognitionSession(String sessionId, Consumer<SpeechRecognitionResponse> resultCallback) {
        log.info("开始语音识别会话: {}", sessionId);
        
        try {
            AiProperties.AlibabaDashscopeConfig dashscopeConfig = aiProperties.getAlibabaDashscope();
            AiProperties.SpeechRecognitionConfig speechConfig = dashscopeConfig.getSpeechRecognition();
            
            // 构建识别参数
            RecognitionParam param = RecognitionParam.builder()
                    .model(speechConfig.getModel())
                    .format(speechConfig.getAudioFormat())
                    .sampleRate(speechConfig.getSampleRate())
                    .callback(new ResultCallback<RecognitionResult>() {
                        @Override
                        public void onEvent(RecognitionResult result) {
                            handleRecognitionResult(sessionId, result, resultCallback);
                        }

                        @Override
                        public void onError(Exception e) {
                            handleRecognitionError(sessionId, e, resultCallback);
                        }

                        @Override
                        public void onComplete() {
                            handleRecognitionComplete(sessionId, resultCallback);
                        }
                    })
                    .build();

            // 创建识别实例
            Recognition recognition = new Recognition();
            Semaphore semaphore = new Semaphore(0);
            
            // 创建会话记录
            RecognitionSession session = new RecognitionSession(recognition, param, semaphore, resultCallback);
            activeSessions.put(sessionId, session);
            
            // 启动识别
            recognition.streamCall(param);
            
            // 发送会话开始状态
            SpeechRecognitionResponse startResponse = SpeechRecognitionResponse.builder()
                    .sessionId(sessionId)
                    .status(SpeechRecognitionResponse.RecognitionStatus.SESSION_STARTED)
                    .text("")
                    .isFinal(false)
                    .build();
            
            resultCallback.accept(startResponse);
            
            log.info("语音识别会话启动成功: {}", sessionId);
            return true;
            
        } catch (NoApiKeyException e) {
            log.error("API Key未配置，无法启动语音识别会话: {}", sessionId, e);
            return false;
        } catch (Exception e) {
            log.error("启动语音识别会话失败: {}", sessionId, e);
            return false;
        }
    }

    @Override
    public boolean sendAudioData(String sessionId, byte[] audioData) {
        RecognitionSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在，无法发送音频数据: {}", sessionId);
            return false;
        }

        try {
            // 发送音频数据到识别服务
            session.getRecognition().sendAudioFrame(audioData);
            log.debug("发送音频数据成功: {} bytes, 会话: {}", audioData.length, sessionId);
            return true;
        } catch (Exception e) {
            log.error("发送音频数据失败，会话: {}", sessionId, e);
            return false;
        }
    }

    @Override
    public void endRecognitionSession(String sessionId) {
        log.info("结束语音识别会话: {}", sessionId);
        
        RecognitionSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: {}", sessionId);
            return;
        }

        try {
            // 停止识别
            session.getRecognition().stop();
            
            // 发送会话结束状态
            SpeechRecognitionResponse endResponse = SpeechRecognitionResponse.builder()
                    .sessionId(sessionId)
                    .status(SpeechRecognitionResponse.RecognitionStatus.SESSION_ENDED)
                    .text("")
                    .isFinal(true)
                    .build();
            
            session.getResultCallback().accept(endResponse);
            
            // 清理会话
            activeSessions.remove(sessionId);
            
            log.info("语音识别会话结束成功: {}", sessionId);
        } catch (Exception e) {
            log.error("结束语音识别会话失败: {}", sessionId, e);
        }
    }


    /**
     * 处理识别结果
     */
    private void handleRecognitionResult(String sessionId, RecognitionResult result, 
                                       Consumer<SpeechRecognitionResponse> callback) {
        try {
            SpeechRecognitionResponse response = convertToResponse(sessionId, result);
            callback.accept(response);
            
            if (log.isDebugEnabled()) {
                log.debug("识别结果 - 会话: {}, 文本: {}, 最终结果: {}", 
                        sessionId, response.getText(), response.getIsFinal());
            }
        } catch (Exception e) {
            log.error("处理识别结果失败，会话: {}", sessionId, e);
        }
    }

    /**
     * 处理识别错误
     */
    private void handleRecognitionError(String sessionId, Exception error, 
                                      Consumer<SpeechRecognitionResponse> callback) {
        log.error("语音识别错误，会话: {}", sessionId, error);
        
        SpeechRecognitionResponse errorResponse = SpeechRecognitionResponse.builder()
                .sessionId(sessionId)
                .status(SpeechRecognitionResponse.RecognitionStatus.FAILED)
                .errorMessage(error.getMessage())
                .isFinal(true)
                .build();
        
        callback.accept(errorResponse);
        
        // 清理失败的会话
        activeSessions.remove(sessionId);
    }

    /**
     * 处理识别完成
     */
    private void handleRecognitionComplete(String sessionId, Consumer<SpeechRecognitionResponse> callback) {
        log.info("语音识别完成，会话: {}", sessionId);
        
        SpeechRecognitionResponse completeResponse = SpeechRecognitionResponse.builder()
                .sessionId(sessionId)
                .status(SpeechRecognitionResponse.RecognitionStatus.COMPLETED)
                .text("")
                .isFinal(true)
                .build();
        
        callback.accept(completeResponse);
        
        // 清理完成的会话
        activeSessions.remove(sessionId);
    }

    /**
     * 转换DashScope结果为统一响应格式
     */
    private SpeechRecognitionResponse convertToResponse(String sessionId, RecognitionResult result) {
        // 构建句子信息
        var sentences = result.getSentences().stream()
                .map(sentence -> SpeechRecognitionResponse.SentenceInfo.builder()
                        .beginTime(sentence.getBeginTime())
                        .endTime(sentence.getEndTime())
                        .text(sentence.getText())
                        .isSentenceEnd(result.isSentenceEnd())
                        .build())
                .collect(Collectors.toList());

        return SpeechRecognitionResponse.builder()
                .sessionId(sessionId)
                .status(result.isSentenceEnd() ? 
                        SpeechRecognitionResponse.RecognitionStatus.COMPLETED :
                        SpeechRecognitionResponse.RecognitionStatus.RECOGNIZING)
                .text(result.getText())
                .isFinal(result.isSentenceEnd())
                .sentences(sentences)
                .requestId(result.getRequestId())
                .build();
    }

    /**
     * 识别会话信息
     */
    private static class RecognitionSession {
        private final Recognition recognition;
        private final RecognitionParam param;
        private final Semaphore semaphore;
        private final Consumer<SpeechRecognitionResponse> resultCallback;

        public RecognitionSession(Recognition recognition, RecognitionParam param, 
                                Semaphore semaphore, Consumer<SpeechRecognitionResponse> resultCallback) {
            this.recognition = recognition;
            this.param = param;
            this.semaphore = semaphore;
            this.resultCallback = resultCallback;
        }

        public Recognition getRecognition() { return recognition; }
        public RecognitionParam getParam() { return param; }
        public Semaphore getSemaphore() { return semaphore; }
        public Consumer<SpeechRecognitionResponse> getResultCallback() { return resultCallback; }
    }
}
