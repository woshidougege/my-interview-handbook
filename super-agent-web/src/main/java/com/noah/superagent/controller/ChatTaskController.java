package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;
import com.noah.superagent.convert.ChatTaskWebConvert;
import com.noah.superagent.model.ChatTaskDO;
import com.noah.superagent.service.ChatTaskService;
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 对话任务管理控制器
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat-tasks")
@RequiredArgsConstructor
@Validated
@Tag(name = "对话任务管理", description = "对话任务CRUD操作接口")
public class ChatTaskController {

    private final ChatTaskService chatTaskService;
    private final ChatTaskWebConvert chatTaskWebConvert;

    @PostMapping
    @Operation(summary = "创建对话任务", description = "创建新对话任务")
    public ApiResponse<ChatTaskResponse> createChatTask(@Valid @RequestBody ChatTaskCreateRequest request) {
        log.info("接收创建对话任务请求: {}", request.getTitle());
        
        // Request -> DO -> Service -> DO -> Response
        ChatTaskDO chatTaskDO = chatTaskWebConvert.fromCreateRequest(request);
        ChatTaskDO resultDO = chatTaskService.createChatTask(chatTaskDO);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(resultDO);
        
        return ApiResponse.success("对话任务创建成功", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询对话任务", description = "根据ID查询对话任务详情")
    public ApiResponse<ChatTaskResponse> getChatTaskById(
            @Parameter(description = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收查询对话任务请求: {}", id);
        
        // Service -> DO -> Response
        ChatTaskDO chatTaskDO = chatTaskService.getChatTaskById(id);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(chatTaskDO);
        
        return ApiResponse.success("查询成功", response);
    }

    @GetMapping
    @Operation(summary = "分页查询对话任务", description = "分页查询对话任务列表，支持关键词搜索")
    public ApiResponse<PageResponse<ChatTaskResponse>> getChatTaskPage(@Valid PageRequest request) {
        log.info("接收分页查询对话任务请求: {}", request);
        
        // Service -> PageResponse<DO> -> PageResponse<Response>
        PageResponse<ChatTaskDO> doPageResponse = chatTaskService.getChatTaskPage(
                request.getPageNum(), request.getPageSize(), request.getKeyword());
        
        PageResponse<ChatTaskResponse> response = new PageResponse<>(
                chatTaskWebConvert.toResponseList(doPageResponse.getRecords()),
                doPageResponse.getTotal(),
                doPageResponse.getPageNum(),
                doPageResponse.getPageSize()
        );
        
        return ApiResponse.success("查询成功", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新对话任务", description = "更新对话任务信息")
    public ApiResponse<ChatTaskResponse> updateChatTask(
            @Parameter(description = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id,
            @Valid @RequestBody ChatTaskUpdateRequest request) {
        log.info("接收更新对话任务请求: {}", id);
        
        // UpdateRequest -> DO -> Service -> DO -> Response
        ChatTaskDO updateDO = chatTaskWebConvert.fromUpdateRequest(request);
        updateDO.setId(id); // 设置要更新的ID
        ChatTaskDO resultDO = chatTaskService.updateChatTask(updateDO);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(resultDO);
        
        return ApiResponse.success("更新成功", response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除对话任务", description = "根据ID删除对话任务")
    public ApiResponse<Void> deleteChatTask(
            @Parameter(description = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收删除对话任务请求: {}", id);
        
        // 删除方法无需转换
        chatTaskService.deleteChatTask(id);
        return ApiResponse.success("删除成功");
    }
}