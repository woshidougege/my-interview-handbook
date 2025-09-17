package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.ScheduledChatTaskExecutionLogResponse;
import com.noah.superagent.convert.ScheduledChatTaskExecutionLogWebConvert;
import com.noah.superagent.model.ScheduledChatTaskExecutionLogDTO;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.ScheduledChatTaskExecutionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 定时对话任务执行日志管理控制器
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scheduled-chat-task-execution-logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "定时对话任务执行日志管理", description = "定时对话任务执行日志查询接口")
public class ScheduledChatTaskExecutionLogController {

    private final ScheduledChatTaskExecutionLogService scheduledChatTaskExecutionLogService;
    private final ScheduledChatTaskExecutionLogWebConvert scheduledChatTaskExecutionLogWebConvert;

    @GetMapping
    @Operation(summary = "根据任务名称查询执行日志", description = "根据任务名称查询执行日志列表")
    public ApiResponse<List<ScheduledChatTaskExecutionLogResponse>> getExecutionLogsByTaskName(
            @Parameter(description = "任务名称", example = "每日新闻摘要")
            @RequestParam("taskName") String taskName) {
        log.info("接收根据任务名称查询执行日志请求: {}", taskName);

        List<ScheduledChatTaskExecutionLogDTO> executionLogDTOs = scheduledChatTaskExecutionLogService.getExecutionLogsByTaskName(taskName);
        List<ScheduledChatTaskExecutionLogResponse> response = scheduledChatTaskExecutionLogWebConvert.toResponseList(executionLogDTOs);

        return ApiResponse.success("查询成功", response);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询执行日志", description = "分页查询执行日志列表")
    public ApiResponse<PageResponse<ScheduledChatTaskExecutionLogResponse>> getExecutionLogsPage(
            @Parameter(description = "任务名称", example = "每日新闻摘要")
            @RequestParam("taskName") String taskName,
            @Valid PageRequest request) {
        log.info("接收分页查询执行日志请求: {}", request);

        PageResponse<ScheduledChatTaskExecutionLogDTO> dtoPageResponse = scheduledChatTaskExecutionLogService.getExecutionLogsPage(
                taskName, request.getPageNum(), request.getPageSize());

        PageResponse<ScheduledChatTaskExecutionLogResponse> response = new PageResponse<>(
                scheduledChatTaskExecutionLogWebConvert.toResponseList(dtoPageResponse.getRecords()),
                dtoPageResponse.getTotal(),
                dtoPageResponse.getPageNum(),
                dtoPageResponse.getPageSize()
        );

        return ApiResponse.success("查询成功", response);
    }
}