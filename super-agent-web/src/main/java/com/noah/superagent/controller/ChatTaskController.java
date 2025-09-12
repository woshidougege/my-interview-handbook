package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;
import com.noah.superagent.common.dto.request.ChatMessageRequest;
import com.noah.superagent.common.dto.response.ChatMessageResponse;
import com.noah.superagent.convert.ChatTaskWebConvert;
import com.noah.superagent.model.ChatTaskDTO;
import com.noah.superagent.service.ChatTaskService;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.websocket.ChatWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

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
@Tag(name = "对话任务管理", description = "对话任务CRUD操作接口")
public class ChatTaskController {

    private final ChatTaskService chatTaskService;
    
    private final ChatTaskWebConvert chatTaskWebConvert;
    
    private final ChatWebSocketHandler chatWebSocketHandler;

    @PostMapping
    @Operation(summary = "创建对话任务", description = "创建新对话任务")
    public ApiResponse<ChatTaskResponse> createChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
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
    
    @PostMapping("/send-message")
    @Operation(summary = "发送对话消息", description = "发送对话消息，支持新建对话和已有对话")
    public ApiResponse<ChatMessageResponse> sendChatMessage(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid @RequestBody ChatMessageRequest request) {
        log.info("接收发送对话消息请求，工作空间ID: {}, 对话任务ID: {}", workspaceId, request.getChatTaskId());
        
        ChatTaskDTO chatTaskDO;
        boolean isNewChat = false;
        
        // 如果chatTaskId为空，创建新的对话任务
        if (request.getChatTaskId() == null) {
            log.info("创建新的对话任务");
            chatTaskDO = new ChatTaskDTO();
            chatTaskDO.setWorkspaceId(workspaceId);
            chatTaskDO.setContextId(UUID.randomUUID().toString());
            chatTaskDO.setTitle("新对话");
            chatTaskDO.setContent("");
            chatTaskDO.setStatus(com.noah.superagent.common.enums.ChatTaskStatusEnum.IN_PROGRESS);
            chatTaskDO.setIsFavorite(com.noah.superagent.common.enums.FavoriteEnum.NOT_FAVORITE);
            chatTaskDO = chatTaskService.createChatTask(chatTaskDO);
            isNewChat = true;
        } else {
            // 获取现有的对话任务
            chatTaskDO = chatTaskService.getChatTaskById(request.getChatTaskId());
            if (chatTaskDO == null) {
                return ApiResponse.error("对话任务不存在");
            }
            
            // 检查对话任务是否属于指定的工作空间
            if (!workspaceId.equals(chatTaskDO.getWorkspaceId())) {
                return ApiResponse.error("对话任务不属于指定的工作空间");
            }
        }
        
        // 调用下游AI处理消息
        String aiResponse = chatWebSocketHandler.processMessage(request.getMessage(), null);
        
        // 更新对话任务内容
        String updatedContent = chatTaskDO.getContent() == null ? "" : chatTaskDO.getContent();
        updatedContent += "\n用户: " + request.getMessage() + "\nAI: " + aiResponse;
        chatTaskDO.setContent(updatedContent);
        chatTaskService.updateChatTask(chatTaskDO);
        
        // 构造响应
        ChatMessageResponse response = new ChatMessageResponse();
        response.setChatTaskId(chatTaskDO.getId());
        response.setContextId(chatTaskDO.getContextId());
        response.setResponse(aiResponse);
        
        log.info("对话消息发送完成，对话任务ID: {}, 是否新对话: {}", chatTaskDO.getId(), isNewChat);
        return ApiResponse.success(isNewChat ? "新对话创建并发送消息成功" : "消息发送成功", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询对话任务", description = "根据ID查询对话任务详情")
    public ApiResponse<ChatTaskResponse> getChatTaskById(
            @Parameter(description = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收查询对话任务请求: {}", id);
        
        // Service -> DTO -> Response
        ChatTaskDTO chatTaskDO = chatTaskService.getChatTaskById(id);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(chatTaskDO);
        
        return ApiResponse.success("查询成功", response);
    }

    @GetMapping
    @Operation(summary = "分页查询对话任务", description = "分页查询对话任务列表")
    public ApiResponse<PageResponse<ChatTaskResponse>> getChatTaskPage(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
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
    @Operation(summary = "更新对话任务", description = "更新对话任务信息")
    public ApiResponse<ChatTaskResponse> updateChatTask(
            @Parameter(description = "对话任务ID", example = "1234567890123456789") 
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
    @Operation(summary = "删除对话任务", description = "根据ID删除对话任务")
    public ApiResponse<Void> deleteChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Parameter(description = "对话任务ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收删除对话任务请求: {}", id);
        
        chatTaskService.deleteChatTask(id);
        return ApiResponse.success("删除成功");
    }
    
    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏对话任务", description = "将指定对话任务标记为收藏")
    public ApiResponse<Void> favoriteChatTask(
            @Parameter(description = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收收藏对话任务请求: {}", id);
        
        chatTaskService.favoriteChatTask(id);
        
        return ApiResponse.success("收藏成功");
    }
    
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏对话任务", description = "取消对话任务的收藏标记")
    public ApiResponse<Void> unfavoriteChatTask(
            @Parameter(description = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收取消收藏对话任务请求: {}", id);
        
        chatTaskService.unfavoriteChatTask(id);
        
        return ApiResponse.success("取消收藏成功");
    }
    
    @GetMapping("/favorites")
    @Operation(summary = "查询收藏的对话任务列表", description = "分页查询收藏的对话任务列表")
    public ApiResponse<PageResponse<ChatTaskResponse>> getFavoriteChatTasks(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
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