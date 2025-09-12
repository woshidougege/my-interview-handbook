package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.request.ChatTitleGenerateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;
import com.noah.superagent.common.dto.response.ChatTitleGenerateResponse;
import com.noah.superagent.convert.ChatTaskWebConvert;
import com.noah.superagent.model.ChatTaskDTO;
import com.noah.superagent.service.ChatTaskService;
import com.noah.superagent.service.AiService;
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
    
    private final AiService aiService;
    
    private final ChatTaskWebConvert chatTaskWebConvert;

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
    
    @PostMapping("/generate-title")
    @Operation(summary = "生成对话标题", description = "根据用户问题生成对话标题")
    public ApiResponse<ChatTitleGenerateResponse> generateChatTitle(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Valid @RequestBody ChatTitleGenerateRequest request) {
        log.info("接收生成对话标题请求，工作空间ID: {}，问题: {}", workspaceId, request.getQuestion());
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (Boolean.TRUE.equals(request.getAsync())) {
                // 异步处理
                aiService.generateChatTitleAsync(request.getQuestion(), new AiService.TitleGenerationCallback() {
                    @Override
                    public void onSuccess(String title) {
                        log.info("异步标题生成成功: {}", title);
                        // TODO: 可以通过WebSocket或消息队列通知前端
                    }

                    @Override
                    public void onError(String error) {
                        log.error("异步标题生成失败: {}", error);
                        // TODO: 可以通过WebSocket或消息队列通知前端
                    }
                });
                
                ChatTitleGenerateResponse response = ChatTitleGenerateResponse.createAsyncResponse();
                return ApiResponse.success("标题生成任务已提交", response);
            } else {
                // 同步处理
                String title = aiService.generateChatTitle(request.getQuestion());
                long duration = System.currentTimeMillis() - startTime;
                
                ChatTitleGenerateResponse response = ChatTitleGenerateResponse.createSyncResponse(title, duration);
                return ApiResponse.success("标题生成成功", response);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("生成对话标题失败，耗时: {}ms，错误: {}", duration, e.getMessage(), e);
            
            // 返回默认标题而不是抛出异常
            ChatTitleGenerateResponse response = ChatTitleGenerateResponse.createSyncResponse("新对话", duration);
            return ApiResponse.success("已使用默认标题", response);
        }
    }
}