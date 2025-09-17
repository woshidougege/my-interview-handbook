package com.noah.superagent.service;

import com.noah.superagent.model.ScheduledChatTaskExecutionLogDTO;
import com.noah.superagent.common.dto.response.PageResponse;

import java.util.List;

/**
 * 定时对话任务执行日志服务接口
 *
 * @author System
 * @since 1.0.0
 */
public interface ScheduledChatTaskExecutionLogService {

    /**
     * 根据任务ID获取执行日志列表
     *
     * @param taskId 定时任务ID
     * @return 执行日志列表
     */
    List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByTaskId(Long taskId);

    /**
     * 根据对话任务ID获取执行日志列表
     *
     * @param chatTaskId 对话任务ID
     * @return 执行日志列表
     */
    List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByChatTaskId(Long chatTaskId);

    /**
     * 根据任务名称获取执行日志列表
     *
     * @param taskName 任务名称
     * @return 执行日志列表
     */
    List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByTaskName(String taskName);

    /**
     * 分页查询执行日志
     *
     * @param taskName 任务名称
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResponse<ScheduledChatTaskExecutionLogDTO> getExecutionLogsPage(String taskName, Integer pageNum, Integer pageSize);
}