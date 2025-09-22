package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.request.ScheduledChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ScheduledChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.ScheduledChatTaskResponse;
import com.noah.superagent.convert.ScheduledChatTaskWebConvert;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.ScheduledChatTaskService;
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
 * 定时对话任务管理控制器
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/scheduled-tasks")
@RequiredArgsConstructor
@Validated
@Tag(name = "定时对话任务管理", description = "定时对话任务CRUD操作接口")
public class ScheduledChatTaskController {

    private final ScheduledChatTaskService scheduledChatTaskService;
    private final ScheduledChatTaskWebConvert scheduledChatTaskWebConvert;

    @PostMapping
    @Operation(summary = "创建定时对话任务", description = "创建新的定时对话任务，支持通过scheduleConfig参数配置定时规则，系统将自动生成cron表达式")
    public ApiResponse<ScheduledChatTaskResponse> createScheduledChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid @RequestBody ScheduledChatTaskCreateRequest request) {
        log.info("接收创建定时对话任务请求: {}", request.getTaskName());

        // Request -> DTO -> Service -> DTO -> Response
        ScheduledChatTaskDTO taskDTO = scheduledChatTaskWebConvert.fromCreateRequest(request);
        // 设置工作空间ID和用户ID
        taskDTO.setWorkspaceId(workspaceId);
        // 在实际应用中，用户ID应该从安全上下文中获取
        taskDTO.setUserId(request.getUserId());
        ScheduledChatTaskDTO resultDTO = scheduledChatTaskService.createScheduledChatTask(taskDTO);
        ScheduledChatTaskResponse response = scheduledChatTaskWebConvert.toResponse(resultDTO);

        return ApiResponse.success("定时对话任务创建成功", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询定时对话任务", description = "根据ID查询定时对话任务详情")
    public ApiResponse<ScheduledChatTaskResponse> getScheduledChatTaskById(
            @Parameter(description = "定时对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收查询定时对话任务请求: {}", id);

        // Service -> DTO -> Response
        ScheduledChatTaskDTO taskDTO = scheduledChatTaskService.getScheduledChatTaskById(id);
        ScheduledChatTaskResponse response = scheduledChatTaskWebConvert.toResponse(taskDTO);

        return ApiResponse.success("查询成功", response);
    }

    @GetMapping
    @Operation(summary = "分页查询定时对话任务", description = "分页查询定时对话任务列表")
    public ApiResponse<PageResponse<ScheduledChatTaskResponse>> getScheduledChatTaskPage(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid PageRequest request) {
        log.info("接收分页查询定时对话任务请求: {}", request);

        // Service -> PageResponse<DTO> -> PageResponse<Response>
        PageResponse<ScheduledChatTaskDTO> dtoPageResponse = scheduledChatTaskService.getScheduledChatTasksPage(
                workspaceId, request.getPageNum(), request.getPageSize(), request.getKeyword());

        PageResponse<ScheduledChatTaskResponse> response = new PageResponse<>(
                scheduledChatTaskWebConvert.toResponseList(dtoPageResponse.getRecords()),
                dtoPageResponse.getTotal(),
                dtoPageResponse.getPageNum(),
                dtoPageResponse.getPageSize()
        );

        return ApiResponse.success("查询成功", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新定时对话任务", description = "更新定时对话任务信息，支持通过scheduleConfig参数更新定时规则")
    public ApiResponse<ScheduledChatTaskResponse> updateScheduledChatTask(
            @Parameter(description = "定时对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id,
            @Valid @RequestBody ScheduledChatTaskUpdateRequest request) {
        log.info("接收更新定时对话任务请求: {}", id);

        // UpdateRequest -> DTO -> Service -> DTO -> Response
        ScheduledChatTaskDTO updateDTO = scheduledChatTaskWebConvert.fromUpdateRequest(request);
        ScheduledChatTaskDTO resultDTO = scheduledChatTaskService.updateScheduledChatTask(id, updateDTO);
        ScheduledChatTaskResponse response = scheduledChatTaskWebConvert.toResponse(resultDTO);

        return ApiResponse.success("更新成功", response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除定时对话任务", description = "根据ID删除定时对话任务")
    public ApiResponse<Void> deleteScheduledChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Parameter(description = "定时对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收删除定时对话任务请求: {}", id);

        scheduledChatTaskService.deleteScheduledChatTask(id);

        return ApiResponse.success("删除成功");
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用定时对话任务", description = "启用指定的定时对话任务")
    public ApiResponse<Void> enableScheduledChatTask(
            @Parameter(description = "定时对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收启用定时对话任务请求: {}", id);

        scheduledChatTaskService.enableScheduledChatTask(id);

        return ApiResponse.success("启用成功");
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "禁用定时对话任务", description = "禁用指定的定时对话任务")
    public ApiResponse<Void> disableScheduledChatTask(
            @Parameter(description = "定时对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收禁用定时对话任务请求: {}", id);

        scheduledChatTaskService.disableScheduledChatTask(id);

        return ApiResponse.success("禁用成功");
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询定时对话任务", description = "根据用户ID查询定时对话任务列表")
    public ApiResponse<List<ScheduledChatTaskResponse>> getScheduledChatTasksByUserId(
            @Parameter(description = "用户ID", example = "1234567890123456789")
            @PathVariable("userId") Long userId) {
        log.info("接收根据用户ID查询定时对话任务请求: {}", userId);

        List<ScheduledChatTaskDTO> scheduledChatTaskDTOs = scheduledChatTaskService.getScheduledChatTasksByUserId(userId);
        List<ScheduledChatTaskResponse> response = scheduledChatTaskWebConvert.toResponseList(scheduledChatTaskDTOs);

        return ApiResponse.success("查询成功", response);
    }

    @GetMapping("/workspace/{workspaceId}")
    @Operation(summary = "根据工作空间ID查询定时对话任务", description = "根据工作空间ID查询定时对话任务列表")
    public ApiResponse<List<ScheduledChatTaskResponse>> getScheduledChatTasksByWorkspaceId(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId) {
        log.info("接收根据工作空间ID查询定时对话任务请求: {}", workspaceId);

        List<ScheduledChatTaskDTO> scheduledChatTaskDTOs = scheduledChatTaskService.getScheduledChatTasksByWorkspaceId(workspaceId);
        List<ScheduledChatTaskResponse> response = scheduledChatTaskWebConvert.toResponseList(scheduledChatTaskDTOs);

        return ApiResponse.success("查询成功", response);
    }
}