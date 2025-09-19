package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.common.enums.FavoriteEnum;
import com.noah.superagent.convert.ChatTaskPersistenceConvert;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.model.ChatTaskDTO;
import com.noah.superagent.service.ChatTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.noah.superagent.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话任务服务实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTaskServiceImpl implements ChatTaskService {

    private final ChatTaskMapper chatTaskMapper;
    private final ScheduledChatTaskMapper scheduledChatTaskMapper;
    private final ChatTaskPersistenceConvert chatTaskPersistenceConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskDTO createChatTask(ChatTaskDTO chatTaskDO) {
        log.info("开始创建对话任务，标题: {}", chatTaskDO.getTitle());
        
        // DTO -> Entity
        ChatTaskEntity chatTaskEntity = chatTaskPersistenceConvert.toEntity(chatTaskDO);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // 自动生成会话ID（contextId），用于与下游平台通信
        if (!StringUtils.hasText(chatTaskEntity.getContextId())) {
            chatTaskEntity.setContextId("ctx_" + System.currentTimeMillis() + "_" + System.nanoTime());
        }
        
        // 保存对话任务
        int result = chatTaskMapper.insertSelective(chatTaskEntity);
        BusinessException.throwIf(result <= 0, "对话任务创建失败");
        
        log.info("对话任务创建成功，ID: {}, ContextId: {}", chatTaskEntity.getId(), chatTaskEntity.getContextId());
        // Entity -> DTO
        return chatTaskPersistenceConvert.fromEntity(chatTaskEntity);
    }

    @Override
    public ChatTaskDTO getChatTaskById(Long id) {
        log.info("查询对话任务信息，ID: {}", id);
        
        ChatTaskEntity chatTaskEntity = chatTaskMapper.selectOneById(id);
        BusinessException.throwIf(chatTaskEntity == null, "对话任务不存在: " + id);
        
        // Entity -> DTO
        return chatTaskPersistenceConvert.fromEntity(chatTaskEntity);
    }

    @Override
    public PageResponse<ChatTaskDTO> getChatTaskPage(Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询对话任务列表，页码: {}, 每页数量: {}, 关键词: {}", pageNum, pageSize, keyword);
        
        Page<ChatTaskEntity> page = new Page<>(pageNum, pageSize);
        page = chatTaskMapper.selectChatTaskPage(page, null, keyword);
        
        // Entity -> DTO
        List<ChatTaskDTO> records = new ArrayList<>();
        for (ChatTaskEntity entity : page.getRecords()) {
            records.add(chatTaskPersistenceConvert.fromEntity(entity));
        }
        
        return new PageResponse<>(
            records,
            page.getTotalRow(),
            (int) page.getPageNumber(),
            (int) page.getPageSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskDTO updateChatTask(ChatTaskDTO chatTaskDO) {
        log.info("更新对话任务信息，ID: {}", chatTaskDO.getId());
        
        // 查询对话任务是否存在
        ChatTaskEntity existingTask = chatTaskMapper.selectOneById(chatTaskDO.getId());
        BusinessException.throwIf(existingTask == null, "对话任务不存在: " + chatTaskDO.getId());
        
        // DTO -> Entity
        ChatTaskEntity updateTask = chatTaskPersistenceConvert.toEntity(chatTaskDO);
        // 执行更新（忽略空值字段）
        int result = chatTaskMapper.update(updateTask);
        BusinessException.throwIf(result <= 0, "对话任务更新失败");
        
        // 查询更新后的数据
        ChatTaskEntity updatedTask = chatTaskMapper.selectOneById(chatTaskDO.getId());
        // Entity -> DTO
        return chatTaskPersistenceConvert.fromEntity(updatedTask);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatTask(Long id) {
        log.info("删除对话任务，ID: {}", id);
        
        ChatTaskEntity existingTask = chatTaskMapper.selectOneById(id);
        if (existingTask == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        int result = chatTaskMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("对话任务删除失败");
        }
        
        log.info("对话任务删除成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteChatTask(Long id) {
        log.info("收藏对话任务，ID: {}", id);
        
        ChatTaskEntity existingTask = chatTaskMapper.selectOneById(id);
        if (existingTask == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // 更新收藏状态为已收藏
        existingTask.setIsFavorite(FavoriteEnum.FAVORITE);
        int result = chatTaskMapper.update(existingTask);
        if (result <= 0) {
            throw new RuntimeException("对话任务收藏失败");
        }
        
        log.info("对话任务收藏成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteChatTask(Long id) {
        log.info("取消收藏对话任务，ID: {}", id);
        
        ChatTaskEntity existingTask = chatTaskMapper.selectOneById(id);
        if (existingTask == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // 更新收藏状态为未收藏
        existingTask.setIsFavorite(FavoriteEnum.NOT_FAVORITE);
        int result = chatTaskMapper.update(existingTask);
        if (result <= 0) {
            throw new RuntimeException("对话任务取消收藏失败");
        }
        
        log.info("对话任务取消收藏成功，ID: {}", id);
    }

    @Override
    public PageResponse<ChatTaskDTO> getFavoriteChatTasks(Long workspaceId, Integer pageNum, Integer pageSize) {
        log.info("分页查询收藏的对话任务列表，工作空间ID: {}, 页码: {}, 每页数量: {}", workspaceId, pageNum, pageSize);
        
        Page<ChatTaskEntity> page = new Page<>(pageNum, pageSize);
        page = chatTaskMapper.selectFavoriteChatTaskPage(page, workspaceId, FavoriteEnum.FAVORITE);
        
        // Entity -> DTO
        List<ChatTaskDTO> records = new ArrayList<>();
        for (ChatTaskEntity entity : page.getRecords()) {
            records.add(chatTaskPersistenceConvert.fromEntity(entity));
        }
        
        return new PageResponse<>(
            records,
            page.getTotalRow(),
            (int) page.getPageNumber(),
            (int) page.getPageSize()
        );
    }

    @Override
    public PageResponse<ChatTaskDTO> getChatTaskPageByWorkspaceId(Long workspaceId, Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询工作空间下的对话任务列表，工作空间ID: {}, 页码: {}, 每页数量: {}, 关键词: {}", workspaceId, pageNum, pageSize, keyword);
        
        Page<ChatTaskEntity> page = new Page<>(pageNum, pageSize);
        page = chatTaskMapper.selectChatTaskPage(page, workspaceId, keyword);
        
        // Entity -> DTO
        List<ChatTaskDTO> records = new ArrayList<>();
        for (ChatTaskEntity entity : page.getRecords()) {
            records.add(chatTaskPersistenceConvert.fromEntity(entity));
        }
        
        return new PageResponse<>(
            records,
            page.getTotalRow(),
            (int) page.getPageNumber(),
            (int) page.getPageSize()
        );
    }
    
    @Override
    public Map<Long, Long> getChatTaskScheduledStatus(List<Long> chatTaskIds) {
        log.info("批量查询对话任务的定时任务状态，对话任务ID列表数量: {}", chatTaskIds != null ? chatTaskIds.size() : 0);
        
        // 查询与这些对话任务关联的定时任务
        List<ScheduledChatTaskEntity> scheduledTasks = scheduledChatTaskMapper.selectByChatTaskIds(chatTaskIds);
        
        // 构建映射关系：对话任务ID -> 定时任务ID
        Map<Long, Long> chatTaskToScheduledTaskMap = new HashMap<>();
        for (ScheduledChatTaskEntity scheduledTask : scheduledTasks) {
            if (scheduledTask.getChatTaskId() != null) {
                chatTaskToScheduledTaskMap.put(scheduledTask.getChatTaskId(), scheduledTask.getId());
            }
        }
        
        return chatTaskToScheduledTaskMap;
    }
}