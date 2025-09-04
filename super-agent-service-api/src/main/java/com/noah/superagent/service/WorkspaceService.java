package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.model.WorkspaceDO;

/**
 * 工作空间服务接口
 * 业务层操作DO对象，与前端DTO解耦
 *
 * @author System
 * @since 1.0.0
 */
public interface WorkspaceService {

    /**
     * 创建工作空间
     *
     * @param workspaceDO 工作空间领域对象
     * @return 创建后的工作空间信息
     */
    WorkspaceDO createWorkspace(WorkspaceDO workspaceDO);

    /**
     * 根据ID查询工作空间
     *
     * @param id 工作空间ID
     * @return 工作空间信息
     */
    WorkspaceDO getWorkspaceById(Long id);

    /**
     * 分页查询工作空间
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param keyword 关键词
     * @return 分页结果
     */
    PageResponse<WorkspaceDO> getWorkspacePage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 更新工作空间信息
     *
     * @param workspaceDO 工作空间领域对象
     * @return 更新后的工作空间信息
     */
    WorkspaceDO updateWorkspace(WorkspaceDO workspaceDO);

    /**
     * 删除工作空间
     *
     * @param id 工作空间ID
     */
    void deleteWorkspace(Long id);
}