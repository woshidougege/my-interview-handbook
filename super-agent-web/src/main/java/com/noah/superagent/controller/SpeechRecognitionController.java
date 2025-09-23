package com.noah.superagent.controller;

import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 语音识别控制器
 * 处理录音文件上传和识别请求
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SpeechRecognitionController {

    private final SpeechRecognitionService speechRecognitionService;

    /**
     * 上传录音文件进行识别
     *
     * @param audioFile 录音文件
     * @param language 语言提示（可选）
     * @return 识别结果
     */
    @PostMapping("/recognition/upload")
    public ResponseEntity<ApiResponse<SpeechRecognitionResponse>> uploadAudioRecognition(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "language", required = false, defaultValue = "zh") String language) {
        
        log.info("接收到语音识别请求: 文件名={}, 大小={}KB, 语言={}", 
                audioFile.getOriginalFilename(), 
                audioFile.getSize() / 1024, 
                language);
        
        try {
            // 验证文件
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("上传的文件为空"));
            }
            
            if (audioFile.getSize() > 100 * 1024 * 1024) { // 100MB限制
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件大小不能超过100MB"));
            }
            
            // 调用识别服务
            SpeechRecognitionResponse result = speechRecognitionService.recognizeAudioStream(
                    audioFile.getInputStream(),
                    audioFile.getOriginalFilename(),
                    language
            );
            
            if (result.getStatus() == SpeechRecognitionResponse.RecognitionStatus.COMPLETED) {
                log.info("语音识别成功: {}", result.getText());
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                log.error("语音识别失败: {}", result.getErrorMessage());
                return ResponseEntity.ok(ApiResponse.error(result.getErrorMessage()));
            }
            
        } catch (IOException e) {
            log.error("处理上传文件失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("处理上传文件失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("语音识别服务异常", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("语音识别服务异常: " + e.getMessage()));
        }
    }

    /**
     * 通过URL进行语音识别
     *
     * @param audioUrl 音频文件URL
     * @param language 语言提示（可选）
     * @return 识别结果
     */
    @PostMapping("/recognition/url")
    public ResponseEntity<ApiResponse<SpeechRecognitionResponse>> urlAudioRecognition(
            @RequestParam("audioUrl") String audioUrl,
            @RequestParam(value = "language", required = false, defaultValue = "zh") String language) {
        
        log.info("接收到URL语音识别请求: URL={}, 语言={}", audioUrl, language);
        
        try {
            SpeechRecognitionResponse result = speechRecognitionService.recognizeAudioUrl(audioUrl, language);
            
            if (result.getStatus() == SpeechRecognitionResponse.RecognitionStatus.COMPLETED) {
                log.info("语音识别成功: {}", result.getText());
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                log.error("语音识别失败: {}", result.getErrorMessage());
                return ResponseEntity.ok(ApiResponse.error(result.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("语音识别服务异常", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("语音识别服务异常: " + e.getMessage()));
        }
    }

    /**
     * 获取支持的音频格式列表
     *
     * @return 支持的格式
     */
    @GetMapping("/recognition/formats")
    public ResponseEntity<ApiResponse<String[]>> getSupportedFormats() {
        String[] formats = {
                "aac", "amr", "avi", "flac", "flv", "m4a", "mkv", "mov", 
                "mp3", "mp4", "mpeg", "ogg", "opus", "wav", "webm", "wma", "wmv"
        };
        
        return ResponseEntity.ok(ApiResponse.success(formats));
    }
}
