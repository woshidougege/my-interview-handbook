package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;
import com.noah.superagent.convert.ChatTaskWebConvert;
import com.noah.superagent.model.ChatTaskDTO;
import com.noah.superagent.service.ChatTaskService;
import com.noah.superagent.response.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 对话任务管理控制器
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/chat-tasks")
@RequiredArgsConstructor
@Validated
@Api(tags = "对话任务管理", description = "对话任务CRUD操作接口")
public class ChatTaskController {

    private final ChatTaskService chatTaskService;
    
    private final ChatTaskWebConvert chatTaskWebConvert;

    @PostMapping
    @ApiOperation(value = "创建对话任务", notes = "创建新对话任务")
    public ApiResponse<ChatTaskResponse> createChatTask(
            @ApiParam(value = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid @RequestBody ChatTaskCreateRequest request) {
        log.info("接收创建对话任务请求: {}", request.getTitle());
        
        // Request -> DTO -> Service -> DTO -> Response
        ChatTaskDTO chatTaskDO = chatTaskWebConvert.fromCreateRequest(request);
        // 设置工作空间ID
        chatTaskDO.setWorkspaceId(workspaceId);
        ChatTaskDTO resultDO = chatTaskService.createChatTask(chatTaskDO);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(resultDO);
        
        return ApiResponse.success("对话任务创建成功", response);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "查询对话任务", notes = "根据ID查询对话任务详情")
    public ApiResponse<ChatTaskResponse> getChatTaskById(
            @ApiParam(value = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收查询对话任务请求: {}", id);
        
        // Service -> DTO -> Response
        ChatTaskDTO chatTaskDO = chatTaskService.getChatTaskById(id);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(chatTaskDO);
        
        return ApiResponse.success("查询成功", response);
    }

    @GetMapping
    @ApiOperation(value = "分页查询对话任务", notes = "分页查询对话任务列表")
    public ApiResponse<PageResponse<ChatTaskResponse>> getChatTaskPage(
            @ApiParam(value = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid PageRequest request) {
        log.info("接收分页查询对话任务请求: {}", request);
        
        // Service -> PageResponse<DTO> -> PageResponse<Response>
        PageResponse<ChatTaskDTO> doPageResponse = chatTaskService.getChatTaskPageByWorkspaceId(
                workspaceId, request.getPageNum(), request.getPageSize(), request.getKeyword());
        
        PageResponse<ChatTaskResponse> response = new PageResponse<>(
                chatTaskWebConvert.toResponseList(doPageResponse.getRecords()),
                doPageResponse.getTotal(),
                doPageResponse.getPageNum(),
                doPageResponse.getPageSize()
        );
        
        return ApiResponse.success("查询成功", response);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新对话任务", notes = "更新对话任务信息")
    public ApiResponse<ChatTaskResponse> updateChatTask(
            @ApiParam(value = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id,
            @Valid @RequestBody ChatTaskUpdateRequest request) {
        log.info("接收更新对话任务请求: {}", id);
        
        // UpdateRequest -> DTO -> Service -> DTO -> Response
        ChatTaskDTO updateDO = chatTaskWebConvert.fromUpdateRequest(request);
        updateDO.setId(id); // 设置要更新的ID
        ChatTaskDTO resultDO = chatTaskService.updateChatTask(updateDO);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(resultDO);
        
        return ApiResponse.success("更新成功", response);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除对话任务", notes = "根据ID删除对话任务")
    public ApiResponse<Void> deleteChatTask(
            @ApiParam(value = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @ApiParam(value = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收删除对话任务请求: {}", id);
        
        chatTaskService.deleteChatTask(id);
        return ApiResponse.success("删除成功");
    }
    
    @PostMapping("/{id}/favorite")
    @ApiOperation(value = "收藏对话任务", notes = "将指定对话任务标记为收藏")
    public ApiResponse<Void> favoriteChatTask(
            @ApiParam(value = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收收藏对话任务请求: {}", id);
        
        chatTaskService.favoriteChatTask(id);
        
        return ApiResponse.success("收藏成功");
    }
    
    @DeleteMapping("/{id}/favorite")
    @ApiOperation(value = "取消收藏对话任务", notes = "取消对话任务的收藏标记")
    public ApiResponse<Void> unfavoriteChatTask(
            @ApiParam(value = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收取消收藏对话任务请求: {}", id);
        
        chatTaskService.unfavoriteChatTask(id);
        
        return ApiResponse.success("取消收藏成功");
    }
    
    @GetMapping("/favorites")
    @ApiOperation(value = "查询收藏的对话任务列表", notes = "分页查询收藏的对话任务列表")
    public ApiResponse<PageResponse<ChatTaskResponse>> getFavoriteChatTasks(
            @ApiParam(value = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid PageRequest request) {
        log.info("接收查询收藏对话任务列表请求，工作空间ID: {}", workspaceId);
        
        // Service -> PageResponse<DTO> -> PageResponse<Response>
        PageResponse<ChatTaskDTO> doPageResponse = chatTaskService.getFavoriteChatTasks(
                workspaceId, request.getPageNum(), request.getPageSize());
        
        PageResponse<ChatTaskResponse> response = new PageResponse<>(
                chatTaskWebConvert.toResponseList(doPageResponse.getRecords()),
                doPageResponse.getTotal(),
                doPageResponse.getPageNum(),
                doPageResponse.getPageSize()
        );
        
        return ApiResponse.success("查询成功", response);
    }
}