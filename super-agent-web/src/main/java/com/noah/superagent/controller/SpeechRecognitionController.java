package com.noah.superagent.controller;

import com.noah.superagent.ai.service.SpeechRecognitionService;
import com.noah.superagent.common.config.SpeechRecognitionProperties;
import com.noah.superagent.common.dto.response.ApiResponse;
import com.noah.superagent.common.dto.response.SpeechRecognitionResponse;
import com.noah.superagent.common.dto.response.SpeechRecognitionConfigResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 语音识别控制器
 * 处理录音文件上传和识别请求，以及语音识别配置管理
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
@Tag(name = "语音识别", description = "语音识别相关接口，包括音频文件识别和配置获取")
public class SpeechRecognitionController {

    private final SpeechRecognitionService speechRecognitionService;
    private final SpeechRecognitionProperties speechProperties;

    /**
     * 上传录音文件进行语音识别
     */
    @Operation(
        summary = "上传音频文件进行语音识别",
        description = "支持上传音频文件进行语音识别，返回识别结果文本。支持多种音频格式，详细配置可通过 /config 接口获取。"
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
                            "  \"message\": \"不支持的音频格式，支持的格式: pcm, wav, mp3, opus, speex, aac, amr\",\n" +
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
                description = "音频文件，支持格式：pcm, wav, mp3, opus, speex, aac, amr。最大文件大小：100MB",
                required = true,
                content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("audioFile") MultipartFile audioFile,
            
            @Parameter(
                description = "语言提示，支持的语言代码：auto(自动检测), zh(中文), en(英文), ja(日语), yue(粤语), ko(韩语), de(德语), fr(法语), ru(俄语), it(意大利语)。默认为自动检测。",
                required = false,
                schema = @Schema(
                    type = "string", 
                    defaultValue = "auto",
                    allowableValues = {"auto", "zh", "en", "ja", "yue", "ko", "de", "fr", "ru", "it"}
                ),
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
     * 获取语音识别配置信息
     */
    @Operation(
        summary = "获取语音识别配置",
        description = "获取语音识别服务支持的音频格式、语言选项、默认配置和参数限制等信息，供前端动态配置使用。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "获取配置成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "配置信息示例",
                    value = "{\n" +
                            "  \"code\": 200,\n" +
                            "  \"message\": \"操作成功\",\n" +
                            "  \"data\": {\n" +
                            "    \"supportedFormats\": [\n" +
                            "      {\n" +
                            "        \"code\": \"wav\",\n" +
                            "        \"name\": \"WAV\",\n" +
                            "        \"description\": \"WAV格式，必须为PCM编码\"\n" +
                            "      },\n" +
                            "      {\n" +
                            "        \"code\": \"mp3\",\n" +
                            "        \"name\": \"MP3\",\n" +
                            "        \"description\": \"MP3压缩音频格式\"\n" +
                            "      }\n" +
                            "    ],\n" +
                            "    \"supportedSourceLanguages\": [\n" +
                            "      {\n" +
                            "        \"code\": \"auto\",\n" +
                            "        \"name\": \"自动检测\"\n" +
                            "      },\n" +
                            "      {\n" +
                            "        \"code\": \"zh\",\n" +
                            "        \"name\": \"中文\"\n" +
                            "      }\n" +
                            "    ],\n" +
                            "    \"supportedTranslationLanguages\": [\n" +
                            "      {\n" +
                            "        \"code\": \"zh\",\n" +
                            "        \"name\": \"中文\"\n" +
                            "      },\n" +
                            "      {\n" +
                            "        \"code\": \"en\",\n" +
                            "        \"name\": \"英文\"\n" +
                            "      }\n" +
                            "    ],\n" +
                            "    \"defaults\": {\n" +
                            "      \"sampleRate\": 16000,\n" +
                            "      \"sourceLanguage\": \"auto\",\n" +
                            "      \"transcriptionEnabled\": true,\n" +
                            "      \"translationEnabled\": false,\n" +
                            "      \"maxEndSilence\": 800\n" +
                            "    },\n" +
                            "    \"limits\": {\n" +
                            "      \"minSampleRate\": 16000,\n" +
                            "      \"maxSampleRate\": 48000,\n" +
                            "      \"minEndSilence\": 200,\n" +
                            "      \"maxEndSilence\": 6000,\n" +
                            "      \"maxFileSizeMB\": 100,\n" +
                            "      \"maxRecordingDurationSeconds\": 300\n" +
                            "    }\n" +
                            "  },\n" +
                            "  \"success\": true,\n" +
                            "  \"timestamp\": 1695456789000\n" +
                            "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SpeechRecognitionConfigResponse>> getRecognitionConfig() {
        log.debug("获取语音识别配置信息");

        try {
            SpeechRecognitionConfigResponse config = buildConfigResponse();
            return ResponseEntity.ok(ApiResponse.success("获取配置成功", config));
        } catch (Exception e) {
            log.error("获取语音识别配置失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取配置失败: " + e.getMessage()));
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
