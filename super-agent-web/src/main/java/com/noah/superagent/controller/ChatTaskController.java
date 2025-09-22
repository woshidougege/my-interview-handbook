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
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @PostMapping
    @Operation(summary = "创建对话任务", description = "创建新对话任务")
    public ApiResponse<ChatTaskResponse> createChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Parameter(description = "对话内容")
            @RequestBody(required = false) Map<String, String> requestBody) {
        log.info("接收创建对话任务请求，工作空间ID: {}", workspaceId);

        // 获取对话内容，如果未提供则为空字符串
        String content = requestBody != null && requestBody.containsKey("content") ? requestBody.get("content") : "";
        
        // 创建默认的对话任务请求对象
        ChatTaskCreateRequest request = new ChatTaskCreateRequest();
        request.setWorkspaceId(workspaceId);

        
        // 设置标题为内容的前10个字符，如果内容为空则设置默认标题
        String title = "新对话";
        if (content != null && !content.isEmpty()) {
            title = content.length() > 10 ? content.substring(0, 10) : content;
        }
        request.setTitle(title);
        
        // 设置内容
        request.setContent(content);
        
        // 设置默认收藏状态为未收藏
        request.setIsFavorite(com.noah.superagent.common.enums.FavoriteEnum.NOT_FAVORITE);
        
        // 设置默认状态为进行中
        request.setStatus(com.noah.superagent.common.enums.ChatTaskStatusEnum.IN_PROGRESS);

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
        
        // 获取对话任务ID列表
        List<Long> chatTaskIds = doPageResponse.getRecords().stream()
                .map(ChatTaskDTO::getId)
                .collect(Collectors.toList());
        
        // 批量查询定时任务状态
        Map<Long, Long> chatTaskScheduledMap = chatTaskService.getChatTaskScheduledStatus(chatTaskIds);
        
        // 转换为响应列表并填充定时任务状态
        List<ChatTaskResponse> responseList = doPageResponse.getRecords().stream()
                .map(chatTaskWebConvert::toResponse)
                .peek(response -> {
                    Long scheduledTaskId = chatTaskScheduledMap.get(response.getId());
                    response.setHasScheduledTask(scheduledTaskId != null);
                    response.setScheduledTaskId(scheduledTaskId);
                })
                .collect(Collectors.toList());
        
        PageResponse<ChatTaskResponse> response = new PageResponse<>(
                responseList,
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
    public ApiResponse<ChatTaskResponse> favoriteChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Parameter(description = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收收藏对话任务请求: {}", id);
        
        chatTaskService.favoriteChatTask(id);
        
        // 重新查询任务以获取更新后的信息
        ChatTaskDTO chatTaskDO = chatTaskService.getChatTaskById(id);
        
        // 获取定时任务状态
        Map<Long, Long> chatTaskScheduledMap = chatTaskService.getChatTaskScheduledStatus(List.of(id));
        ChatTaskResponse response = chatTaskWebConvert.toResponse(chatTaskDO);
        Long scheduledTaskId = chatTaskScheduledMap.get(response.getId());
        response.setHasScheduledTask(scheduledTaskId != null);
        response.setScheduledTaskId(scheduledTaskId);
        
        return ApiResponse.success("收藏成功", response);
    }
    
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏对话任务", description = "取消对话任务的收藏标记")
    public ApiResponse<ChatTaskResponse> unfavoriteChatTask(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Parameter(description = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id) {
        log.info("接收取消收藏对话任务请求: {}", id);
        
        chatTaskService.unfavoriteChatTask(id);
        
        // 重新查询任务以获取更新后的信息
        ChatTaskDTO chatTaskDO = chatTaskService.getChatTaskById(id);
        
        // 获取定时任务状态
        Map<Long, Long> chatTaskScheduledMap = chatTaskService.getChatTaskScheduledStatus(List.of(id));
        ChatTaskResponse response = chatTaskWebConvert.toResponse(chatTaskDO);
        Long scheduledTaskId = chatTaskScheduledMap.get(response.getId());
        response.setHasScheduledTask(scheduledTaskId != null);
        response.setScheduledTaskId(scheduledTaskId);
        
        return ApiResponse.success("取消收藏成功", response);
    }
    
    @PostMapping("/generate-title")
    @Operation(summary = "生成对话标题", description = "根据用户问题生成对话标题")
    public ApiResponse<ChatTitleGenerateResponse> generateTitle(
            @Valid @RequestBody ChatTitleGenerateRequest request) {
        log.info("接收生成对话标题请求: {}", request.getQuestion());
        
        // TODO: 调用ChatTaskService生成标题
        String generatedTitle = chatTaskService.generateChatTitle(request.getQuestion());
        
        ChatTitleGenerateResponse response = new ChatTitleGenerateResponse();
        response.setTitle(generatedTitle);
        response.setAsync(request.getAsync() != null ? request.getAsync() : false);
        
        return ApiResponse.success("标题生成成功", response);
    }
    
    @PutMapping("/{id}/title")
    @Operation(summary = "修改对话任务标题", description = "修改指定对话任务的标题")
    public ApiResponse<ChatTaskResponse> updateChatTaskTitle(
            @Parameter(description = "工作空间ID", example = "1234567890123456789")
            @PathVariable("workspaceId") Long workspaceId,
            @Parameter(description = "对话任务ID", example = "1234567890123456789")
            @PathVariable("id") Long id,
            @Parameter(description = "新标题")
            @RequestParam("title") String title) {
        log.info("接收修改对话任务标题请求: ID={}, 新标题={}", id, title);
        
        // 创建更新对象
        ChatTaskDTO updateDO = new ChatTaskDTO();
        updateDO.setId(id);
        updateDO.setTitle(title);
        
        // 更新标题
        ChatTaskDTO resultDO = chatTaskService.updateChatTask(updateDO);
        ChatTaskResponse response = chatTaskWebConvert.toResponse(resultDO);
        
        return ApiResponse.success("标题修改成功", response);
    }
}