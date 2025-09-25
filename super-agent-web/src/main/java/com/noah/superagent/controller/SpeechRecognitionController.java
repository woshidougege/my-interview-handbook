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
        
        log.info("===== 开始语音识别请求 =====");
        log.info("请求信息:");
        log.info("  ├─ 文件名: {}", audioFile.getOriginalFilename());
        log.info("  ├─ 文件大小: {}KB ({} bytes)", audioFile.getSize() / 1024, audioFile.getSize());
        log.info("  ├─ 语言设置: {}", language);
        log.info("  └─ 文件类型: {}", audioFile.getContentType());
        
        // 记录开始时间（移到try块外）
        long startTime = System.currentTimeMillis();
        
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
                    language,
                    null  // FunASR不需要model参数
            );
            
            // 记录结束时间和耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            if (result.getStatus() == SpeechRecognitionResponse.RecognitionStatus.COMPLETED) {
                log.info("===== 语音识别请求完成 =====");
                log.info("响应结果:");
                log.info("  ├─ 文件名: {}", audioFile.getOriginalFilename());
                log.info("  ├─ 识别文本长度: {} 字符", result.getText().length());
                log.info("  ├─ 处理耗时: {}", formatDurationSimple(duration));
                log.info("  └─ 状态: 成功");
                log.info("最终识别文本结果: {}", result.getText());
                log.info("===============================");
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                log.error("===== 语音识别请求失败 =====");
                log.error("错误信息:");
                log.error("  ├─ 文件名: {}", audioFile.getOriginalFilename());
                log.error("  ├─ 错误原因: {}", result.getErrorMessage());
                log.error("  └─ 耗时: {}", formatDurationSimple(duration));
                log.error("===============================");
                return ResponseEntity.ok(ApiResponse.error(result.getErrorMessage()));
            }
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("===== 文件处理异常 =====");
            log.error("异常信息:");
            log.error("  ├─ 文件名: {}", audioFile.getOriginalFilename());
            log.error("  ├─ 异常类型: IO异常");
            log.error("  ├─ 错误信息: {}", e.getMessage());
            log.error("  └─ 耗时: {}", formatDurationSimple(duration));
            log.error("===========================");
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("处理上传文件失败: " + e.getMessage()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("===== 语音识别服务异常 =====");
            log.error("异常信息:");
            log.error("  ├─ 文件名: {}", audioFile.getOriginalFilename());
            log.error("  ├─ 异常类型: 服务异常");
            log.error("  ├─ 错误信息: {}", e.getMessage());
            log.error("  └─ 耗时: {}", formatDurationSimple(duration));
            log.error("===============================");
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("语音识别服务异常: " + e.getMessage()));
        }
    }

    /**
     * 简单的时间格式化方法，用于Controller层日志
     */
    private String formatDurationSimple(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "毫秒";
        } else if (milliseconds < 60000) {
            return String.format("%.1f秒", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return minutes + "分" + seconds + "秒";
        }
    }


}
