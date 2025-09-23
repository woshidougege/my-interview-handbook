package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.model.ScheduledChatTaskDTO;

import java.util.List;

/**
 * 定时对话任务服务接口
 * 对应数据库表：t_scheduled_chat_task
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface ScheduledChatTaskService {

    /**
     * 创建定时对话任务
     * @param taskDTO 定时对话任务DTO
     * @return 创建后的任务DTO
     */
    ScheduledChatTaskDTO createScheduledChatTask(ScheduledChatTaskDTO taskDTO);

    /**
     * 根据ID获取定时对话任务
     * @param id 任务ID
     * @return 定时对话任务DTO
     */
    ScheduledChatTaskDTO getScheduledChatTaskById(Long id);

    /**
     * 更新定时对话任务
     * @param id 任务ID
     * @param taskDTO 更新后的任务DTO
     * @return 更新后的任务DTO
     */
    ScheduledChatTaskDTO updateScheduledChatTask(Long id, ScheduledChatTaskDTO taskDTO);

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
    List<ScheduledChatTaskDTO> getScheduledChatTasksByUserId(Long userId);

    /**
     * 根据工作空间ID获取定时对话任务列表
     * @param workspaceId 工作空间ID
     * @return 定时对话任务列表
     */
    List<ScheduledChatTaskDTO> getScheduledChatTasksByWorkspaceId(Long workspaceId);

    /**
     * 分页查询定时对话任务
     * @param workspaceId 工作空间ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @return 分页结果
     */
    PageResponse<ScheduledChatTaskDTO> getScheduledChatTasksPage(Long workspaceId, Integer pageNum, Integer pageSize, String keyword);

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