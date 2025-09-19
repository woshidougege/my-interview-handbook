package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.model.ChatTaskDTO;

import java.util.List;
import java.util.Map;

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
    
    /**
     * 收藏对话任务
     *
     * @param id 对话任务ID
     */
    void favoriteChatTask(Long id);
    
    /**
     * 取消收藏对话任务
     *
     * @param id 对话任务ID
     */
    void unfavoriteChatTask(Long id);
    
    /**
     * 查询收藏的对话任务列表
     *
     * @param workspaceId 工作空间ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 收藏的对话任务分页列表
     */
    PageResponse<ChatTaskDTO> getFavoriteChatTasks(Long workspaceId, Integer pageNum, Integer pageSize);
    
    /**
     * 查询工作空间下的所有对话任务
     *
     * @param workspaceId 工作空间ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param keyword 关键词
     * @return 对话任务分页列表
     */
    PageResponse<ChatTaskDTO> getChatTaskPageByWorkspaceId(Long workspaceId, Integer pageNum, Integer pageSize, String keyword);
    
    /**
     * 批量查询对话任务的定时任务状态
     * 
     * @param chatTaskIds 对话任务ID列表
     * @return 对话任务ID到定时任务ID的映射
     */
    Map<Long, Long> getChatTaskScheduledStatus(List<Long> chatTaskIds);
    
    /**
     * 基于算法生成对话标题
     * 
     * @param question 用户问题
     * @return 生成的标题
     */
    String generateChatTitle(String question);
}