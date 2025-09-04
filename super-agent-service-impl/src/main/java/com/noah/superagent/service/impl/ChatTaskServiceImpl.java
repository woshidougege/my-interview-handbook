package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.convert.ChatTaskPersistenceConvert;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.model.ChatTaskDO;
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
    private final ChatTaskPersistenceConvert chatTaskPersistenceConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskDO createChatTask(ChatTaskDO chatTaskDO) {
        log.info("开始创建对话任务，标题: {}", chatTaskDO.getTitle());
        
        // DO -> Entity
        ChatTaskEntity chatTaskEntity = chatTaskPersistenceConvert.toEntity(chatTaskDO);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // 保存对话任务
        int result = chatTaskMapper.insertSelective(chatTaskEntity);
        if (result <= 0) {
            throw new RuntimeException("对话任务创建失败");
        }
        
        log.info("对话任务创建成功，ID: {}", chatTaskEntity.getId());
        // Entity -> DO
        return chatTaskPersistenceConvert.fromEntity(chatTaskEntity);
    }

    @Override
    public ChatTaskDO getChatTaskById(Long id) {
        log.info("查询对话任务信息，ID: {}", id);
        
        ChatTaskEntity chatTaskEntity = chatTaskMapper.selectOneById(id);
        if (chatTaskEntity == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // Entity -> DO
        return chatTaskPersistenceConvert.fromEntity(chatTaskEntity);
    }

    @Override
    public PageResponse<ChatTaskDO> getChatTaskPage(Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询对话任务，页码: {}, 每页数量: {}, 关键词: {}", 
                pageNum, pageSize, keyword);
        
        // 创建分页对象
        Page<ChatTaskEntity> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        // TODO: 实现具体的分页查询逻辑
        Page<ChatTaskEntity> chatTaskPage = chatTaskMapper.selectChatTaskPage(page, null, keyword);
        
        // 转换结果
        return new PageResponse<>(
                chatTaskPersistenceConvert.fromEntityList(chatTaskPage.getRecords()),
                chatTaskPage.getTotalRow(),
                pageNum,
                pageSize
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskDO updateChatTask(ChatTaskDO chatTaskDO) {
        log.info("更新对话任务信息，ID: {}", chatTaskDO.getId());
        
        // 查询对话任务是否存在
        ChatTaskEntity existingChatTaskEntity = chatTaskMapper.selectOneById(chatTaskDO.getId());
        if (existingChatTaskEntity == null) {
            throw new RuntimeException("对话任务不存在: " + chatTaskDO.getId());
        }
        
        // 更新字段
        if (StringUtils.hasText(chatTaskDO.getTitle())) {
            existingChatTaskEntity.setTitle(chatTaskDO.getTitle());
        }
        
        if (StringUtils.hasText(chatTaskDO.getContent())) {
            existingChatTaskEntity.setContent(chatTaskDO.getContent());
        }
        
        if (chatTaskDO.getStatus() != null) {
            existingChatTaskEntity.setStatus(chatTaskDO.getStatus());
        }
        
        // 执行更新
        int result = chatTaskMapper.update(existingChatTaskEntity);
        if (result <= 0) {
            throw new RuntimeException("对话任务更新失败");
        }
        
        log.info("对话任务更新成功，ID: {}", chatTaskDO.getId());
        // Entity -> DO
        return chatTaskPersistenceConvert.fromEntity(existingChatTaskEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatTask(Long id) {
        log.info("删除对话任务，ID: {}", id);
        
        // 检查对话任务是否存在
        ChatTaskEntity chatTaskEntity = chatTaskMapper.selectOneById(id);
        if (chatTaskEntity == null) {
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