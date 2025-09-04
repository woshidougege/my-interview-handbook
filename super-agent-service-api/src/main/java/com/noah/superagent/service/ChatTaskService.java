package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.model.ChatTaskDTO;

/**
 * 对话任务服务接口
 * 业务层操作DO对象，与前端DTO解耦
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface ChatTaskService {

    /**
     * 创建对话任务
     *
     * @param chatTaskDO 对话任务领域对象
     * @return 创建后的对话任务信息
     */
    ChatTaskDTO createChatTask(ChatTaskDTO chatTaskDO);

    /**
     * 根据ID查询对话任务
     *
     * @param id 对话任务ID
     * @return 对话任务信息
     */
    ChatTaskDTO getChatTaskById(Long id);

    /**
     * 分页查询对话任务
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param keyword 关键词
     * @return 分页结果
     */
    PageResponse<ChatTaskDTO> getChatTaskPage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 更新对话任务信息
     *
     * @param chatTaskDO 对话任务领域对象
     * @return 更新后的对话任务信息
     */
    ChatTaskDTO updateChatTask(ChatTaskDTO chatTaskDO);

    /**
     * 删除对话任务
     *
     * @param id 对话任务ID
     */
    void deleteChatTask(Long id);
}