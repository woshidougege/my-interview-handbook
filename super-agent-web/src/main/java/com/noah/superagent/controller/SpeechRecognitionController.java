package com.noah.superagent.controller;

import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 语音识别控制器 - 极简版
 * 处理录音文件上传和识别请求
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
@Tag(name = "语音识别", description = "基于FunASR的语音识别接口")
public class SpeechRecognitionController {

    private final SpeechRecognitionService speechRecognitionService;

    /**
     * 上传录音文件进行语音识别
     */
    @Operation(
        summary = "上传音频文件进行语音识别",
        description = "基于FunASR本地服务的语音识别，支持多种音频格式。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "识别成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "成功示例",
                    value = "{\n" +
                            "  \"code\": 200,\n" +
                            "  \"message\": \"操作成功\",\n" +
                            "  \"data\": {\n" +
                            "    \"status\": \"COMPLETED\",\n" +
                            "    \"text\": \"你好，这是一段测试语音\",\n" +
                            "    \"isFinal\": true,\n" +
                            "    \"confidence\": null,\n" +
                            "    \"sessionId\": null,\n" +
                            "    \"sentences\": null,\n" +
                            "    \"errorMessage\": null,\n" +
                            "    \"requestId\": \"req-12345\"\n" +
                            "  },\n" +
                            "  \"success\": true,\n" +
                            "  \"timestamp\": 1695456789000\n" +
                            "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "参数错误示例", 
                    value = "{\n" +
                            "  \"code\": 400,\n" +
                            "  \"message\": \"文件大小不能超过100MB\",\n" +
                            "  \"data\": null,\n" +
                            "  \"success\": false,\n" +
                            "  \"timestamp\": 1695456789000\n" +
                            "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/recognition/upload")
    public ResponseEntity<ApiResponse<SpeechRecognitionResponse>> uploadAudioRecognition(
            @Parameter(
                description = "音频文件，最大100MB",
                required = true,
                content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("audioFile") MultipartFile audioFile,
            
            @Parameter(
                description = "语言代码，默认auto自动检测",
                required = false,
                example = "auto"
            )
            @RequestParam(value = "language", required = false, defaultValue = "auto") String language) {
        
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
            
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            log.info("语音识别开始 - 使用FunASR本地服务, 开始时间: {}", startTime);
            
            // 调用识别服务
            SpeechRecognitionResponse result = speechRecognitionService.recognizeAudioStream(
                    audioFile.getInputStream(),
                    audioFile.getOriginalFilename(),
                    language,
                    null  // FunASR不需要model参数
            );
            
            // 记录结束时间和耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("语音识别完成 - 使用FunASR本地服务, 耗时: {}ms", duration);
            
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


}
