package com.noah.superagent.service;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.WorkspaceCreateRequest;
import com.noah.superagent.common.dto.request.WorkspaceUpdateRequest;
import com.noah.superagent.common.dto.response.WorkspaceResponse;

/**
 * 工作空间服务接口
 *
 * @author System
 * @since 1.0.0
 */
public interface WorkspaceService {

    /**
     * 创建工作空间
     *
     * @param request 创建请求
     * @return 工作空间信息
     */
    WorkspaceResponse createWorkspace(WorkspaceCreateRequest request);

    /**
     * 根据ID查询工作空间
     *
     * @param id 工作空间ID
     * @return 工作空间信息
     */
    WorkspaceResponse getWorkspaceById(Long id);

    /**
     * 分页查询工作空间
     *
     * @param request 分页请求
     * @return 分页结果
     */
    PageResponse<WorkspaceResponse> getWorkspacePage(PageRequest request);

    /**
     * 更新工作空间信息
     *
     * @param id 工作空间ID
     * @param request 更新请求
     * @return 更新后的工作空间信息
     */
    WorkspaceResponse updateWorkspace(Long id, WorkspaceUpdateRequest request);

    /**
     * 删除工作空间
     *
     * @param id 工作空间ID
     */
    void deleteWorkspace(Long id);
}