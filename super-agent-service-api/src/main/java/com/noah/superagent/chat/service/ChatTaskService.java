package com.noah.superagent.chat.service;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.chat.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.chat.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.chat.ChatTaskResponse;

/**
 * 对话任务服务接口
 *
 * @author System
 * @since 1.0.0
 */
public interface ChatTaskService {

    /**
     * 创建对话任务
     *
     * @param request 创建请求
     * @return 对话任务信息
     */
    ChatTaskResponse createChatTask(ChatTaskCreateRequest request);

    /**
     * 根据ID查询对话任务
     *
     * @param id 对话任务ID
     * @return 对话任务信息
     */
    ChatTaskResponse getChatTaskById(Long id);

    /**
     * 分页查询对话任务
     *
     * @param request 分页请求
     * @return 分页结果
     */
    PageResponse<ChatTaskResponse> getChatTaskPage(PageRequest request);

    /**
     * 更新对话任务信息
     *
     * @param id 对话任务ID
     * @param request 更新请求
     * @return 更新后的对话任务信息
     */
    ChatTaskResponse updateChatTask(Long id, ChatTaskUpdateRequest request);

    /**
     * 删除对话任务
     *
     * @param id 对话任务ID
     */
    void deleteChatTask(Long id);
}