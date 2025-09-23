package com.noah.superagent.ai.service;

import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;

import java.io.InputStream;

/**
 * 语音识别服务接口
 * 基于阿里云百炼Gummy实时语音识别API
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface SpeechRecognitionService {

    /**
     * 识别录音文件流
     * 
     * @param audioStream 录音文件输入流
     * @param filename 文件名（包含扩展名）
     * @param language 语言提示（可选，如：zh、en等）
     * @return 识别结果
     */
    SpeechRecognitionResponse recognizeAudioStream(InputStream audioStream, String filename, String language);


}
