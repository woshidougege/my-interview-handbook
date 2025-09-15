package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;

import java.util.List;

/**
 * 定时对话任务服务接口
 *
 * @author System
 * @since 1.0.0
 */
public interface ScheduledChatTaskService {

    /**
     * 创建定时对话任务
     * @param task 定时对话任务实体
     * @return 创建后的任务实体
     */
    ScheduledChatTaskEntity createScheduledChatTask(ScheduledChatTaskEntity task);

    /**
     * 根据ID获取定时对话任务
     * @param id 任务ID
     * @return 定时对话任务实体
     */
    ScheduledChatTaskEntity getScheduledChatTaskById(Long id);

    /**
     * 更新定时对话任务
     * @param id 任务ID
     * @param task 更新后的任务实体
     * @return 更新后的任务实体
     */
    ScheduledChatTaskEntity updateScheduledChatTask(Long id, ScheduledChatTaskEntity task);

    /**
     * 删除定时对话任务
     * @param id 任务ID
     */
    void deleteScheduledChatTask(Long id);

    /**
     * 根据用户ID获取定时对话任务列表
     * @param userId 用户ID
     * @return 定时对话任务列表
     */
    List<ScheduledChatTaskEntity> getScheduledChatTasksByUserId(Long userId);

    /**
     * 根据工作空间ID获取定时对话任务列表
     * @param workspaceId 工作空间ID
     * @return 定时对话任务列表
     */
    List<ScheduledChatTaskEntity> getScheduledChatTasksByWorkspaceId(Long workspaceId);

    /**
     * 分页查询定时对话任务
     * @param workspaceId 工作空间ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @return 分页结果
     */
    PageResponse<ScheduledChatTaskEntity> getScheduledChatTasksPage(Long workspaceId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 启用定时对话任务
     * @param id 任务ID
     */
    void enableScheduledChatTask(Long id);

    /**
     * 禁用定时对话任务
     * @param id 任务ID
     */
    void disableScheduledChatTask(Long id);
}