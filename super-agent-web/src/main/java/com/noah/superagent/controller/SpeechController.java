package com.noah.superagent.controller;

import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 语音服务控制器
 * 提供语音识别基础配置接口
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Tag(name = "语音服务", description = "语音识别配置接口")
@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
@Slf4j
public class SpeechController {

    @Operation(summary = "获取语音识别配置", description = "获取当前语音识别服务的配置信息")
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        
        Map<String, Object> config = new HashMap<>();
        config.put("model", "paraformer-realtime-v2");
        config.put("language", "zh");
        config.put("realtime", true);
        config.put("punctuation", true);
        config.put("websocketEndpoint", "/api/v1/ws/speech-recognition");
        config.put("sampleRate", 16000);
        config.put("audioFormat", "webm");
        
        return ResponseEntity.ok(ApiResponse.success(config));
    }
}