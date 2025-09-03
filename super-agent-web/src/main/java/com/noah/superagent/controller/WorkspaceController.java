package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.workspace.WorkspaceCreateRequest;
import com.noah.superagent.common.dto.workspace.WorkspaceUpdateRequest;
import com.noah.superagent.common.dto.workspace.WorkspaceResponse;
import com.noah.superagent.workspace.service.WorkspaceService;
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
 * 工作空间管理控制器
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Validated
@Tag(name = "工作空间管理", description = "工作空间CRUD操作接口")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "创建工作空间", description = "创建新工作空间")
    public ApiResponse<WorkspaceResponse> createWorkspace(@Valid @RequestBody WorkspaceCreateRequest request) {
        log.info("接收创建工作空间请求: {}", request.getName());
        
        WorkspaceResponse response = workspaceService.createWorkspace(request);
        return ApiResponse.success("工作空间创建成功", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询工作空间", description = "根据ID查询工作空间详情")
    public ApiResponse<WorkspaceResponse> getWorkspaceById(
            @Parameter(description = "工作空间ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收查询工作空间请求: {}", id);
        
        WorkspaceResponse response = workspaceService.getWorkspaceById(id);
        return ApiResponse.success("查询成功", response);
    }

    @GetMapping
    @Operation(summary = "分页查询工作空间", description = "分页查询工作空间列表，支持关键词搜索")
    public ApiResponse<PageResponse<WorkspaceResponse>> getWorkspacePage(@Valid PageRequest request) {
        log.info("接收分页查询工作空间请求: {}", request);
        
        PageResponse<WorkspaceResponse> response = workspaceService.getWorkspacePage(request);
        return ApiResponse.success("查询成功", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工作空间", description = "更新工作空间信息")
    public ApiResponse<WorkspaceResponse> updateWorkspace(
            @Parameter(description = "工作空间ID", example = "1234567890123456789") 
            @PathVariable("id") Long id,
            @Valid @RequestBody WorkspaceUpdateRequest request) {
        log.info("接收更新工作空间请求: {}", id);
        
        WorkspaceResponse response = workspaceService.updateWorkspace(id, request);
        return ApiResponse.success("更新成功", response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作空间", description = "根据ID删除工作空间")
    public ApiResponse<Void> deleteWorkspace(
            @Parameter(description = "工作空间ID", example = "1234567890123456789") 
            @PathVariable("id") Long id) {
        log.info("接收删除工作空间请求: {}", id);
        
        workspaceService.deleteWorkspace(id);
        return ApiResponse.success("删除成功");
    }
}