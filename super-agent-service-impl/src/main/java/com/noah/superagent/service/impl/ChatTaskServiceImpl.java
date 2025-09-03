package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.convert.ChatTaskConvert;
import com.noah.superagent.dao.entity.ChatTask;
import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;

import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.service.ChatTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 对话任务服务实现
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTaskServiceImpl implements ChatTaskService {

    private final ChatTaskMapper chatTaskMapper;
    private final ChatTaskConvert chatTaskConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskResponse createChatTask(ChatTaskCreateRequest request) {
        log.info("开始创建对话任务，标题: {}", request.getTitle());
        
        // 转换为实体
        ChatTask chatTask = chatTaskConvert.toEntity(request);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // 保存对话任务
        int result = chatTaskMapper.insertSelective(chatTask);
        if (result <= 0) {
            throw new RuntimeException("对话任务创建失败");
        }
        
        log.info("对话任务创建成功，ID: {}", chatTask.getId());
        return chatTaskConvert.toResponse(chatTask);
    }

    @Override
    public ChatTaskResponse getChatTaskById(Long id) {
        log.info("查询对话任务信息，ID: {}", id);
        
        ChatTask chatTask = chatTaskMapper.selectOneById(id);
        if (chatTask == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        return chatTaskConvert.toResponse(chatTask);
    }

    @Override
    public PageResponse<ChatTaskResponse> getChatTaskPage(PageRequest request) {
        log.info("分页查询对话任务，页码: {}, 每页数量: {}, 关键词: {}", 
                request.getPageNum(), request.getPageSize(), request.getKeyword());
        
        // 创建分页对象
        Page<ChatTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        // 执行分页查询
        // TODO: 实现具体的分页查询逻辑
        Page<ChatTask> chatTaskPage = chatTaskMapper.selectChatTaskPage(page, null, request.getKeyword());
        
        // 转换结果
        return new PageResponse<>(
                chatTaskConvert.toResponseList(chatTaskPage.getRecords()),
                chatTaskPage.getTotalRow(),
                request.getPageNum(),
                request.getPageSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskResponse updateChatTask(Long id, ChatTaskUpdateRequest request) {
        log.info("更新对话任务信息，ID: {}", id);
        
        // 查询对话任务是否存在
        ChatTask existingChatTask = chatTaskMapper.selectOneById(id);
        if (existingChatTask == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // 更新字段
        if (StringUtils.hasText(request.getTitle())) {
            existingChatTask.setTitle(request.getTitle());
        }
        
        if (StringUtils.hasText(request.getContent())) {
            existingChatTask.setContent(request.getContent());
        }
        
        if (request.getStatus() != null) {
            existingChatTask.setStatus(request.getStatus());
        }
        
        // 执行更新
        int result = chatTaskMapper.update(existingChatTask);
        if (result <= 0) {
            throw new RuntimeException("对话任务更新失败");
        }
        
        log.info("对话任务更新成功，ID: {}", id);
        return chatTaskConvert.toResponse(existingChatTask);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatTask(Long id) {
        log.info("删除对话任务，ID: {}", id);
        
        // 检查对话任务是否存在
        ChatTask chatTask = chatTaskMapper.selectOneById(id);
        if (chatTask == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // 执行删除
        int result = chatTaskMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("对话任务删除失败");
        }
        
        log.info("对话任务删除成功，ID: {}", id);
    }
}