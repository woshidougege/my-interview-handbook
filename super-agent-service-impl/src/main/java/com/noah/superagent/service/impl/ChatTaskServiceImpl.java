package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.enums.ChatTaskStatusEnum;
import com.noah.superagent.common.enums.FavoriteEnum;
import com.noah.superagent.convert.ChatTaskPersistenceConvert;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.model.ChatTaskDTO;
import com.noah.superagent.service.ChatTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.noah.superagent.dao.entity.table.ChatTaskEntityTableDef.CHAT_TASK_ENTITY;

/**
 * 对话任务服务实现类
 *
 * @author 任相鹏
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
    public ChatTaskDTO createChatTask(ChatTaskDTO chatTaskDO) {
        log.info("创建对话任务: {}", chatTaskDO.getTitle());
        
        // DTO -> Entity
        ChatTaskEntity chatTaskEntity = chatTaskPersistenceConvert.toEntity(chatTaskDO);
        
        // 设置默认值
        if (chatTaskEntity.getStatus() == null) {
            chatTaskEntity.setStatus(ChatTaskStatusEnum.IN_PROGRESS);
        }
        
        if (chatTaskEntity.getIsFavorite() == null) {
            chatTaskEntity.setIsFavorite(FavoriteEnum.NOT_FAVORITE);
        }
        
        // 执行插入
        int result = chatTaskMapper.insert(chatTaskEntity);
        if (result <= 0) {
            throw new RuntimeException("对话任务创建失败");
        }
        
        log.info("对话任务创建成功，ID: {}", chatTaskEntity.getId());
        // Entity -> DTO
        return chatTaskPersistenceConvert.fromEntity(chatTaskEntity);
    }

    @Override
    public ChatTaskDTO getChatTaskById(Long id) {
        log.info("根据ID查询对话任务: {}", id);
        
        // 查询对话任务
        ChatTaskEntity chatTaskEntity = chatTaskMapper.selectOneById(id);
        if (chatTaskEntity == null) {
            return null;
        }
        
        // Entity -> DTO
        return chatTaskPersistenceConvert.fromEntity(chatTaskEntity);
    }

    @Override
    public PageResponse<ChatTaskDTO> getChatTaskPage(Integer pageNum, Integer pageSize, String keyword) {
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
    public ChatTaskDTO updateChatTask(ChatTaskDTO chatTaskDO) {
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
        // Entity -> DTO
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteChatTask(Long id) {
        log.info("收藏对话任务，ID: {}", id);
        
        // 检查对话任务是否存在
        ChatTaskEntity chatTaskEntity = chatTaskMapper.selectOneById(id);
        if (chatTaskEntity == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // 更新收藏状态为已收藏(1)
        chatTaskEntity.setIsFavorite(FavoriteEnum.FAVORITE);
        int result = chatTaskMapper.update(chatTaskEntity);
        if (result <= 0) {
            throw new RuntimeException("对话任务收藏失败");
        }
        
        log.info("对话任务收藏成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteChatTask(Long id) {
        log.info("取消收藏对话任务，ID: {}", id);
        
        // 检查对话任务是否存在
        ChatTaskEntity chatTaskEntity = chatTaskMapper.selectOneById(id);
        if (chatTaskEntity == null) {
            throw new RuntimeException("对话任务不存在: " + id);
        }
        
        // 更新收藏状态为未收藏(0)
        chatTaskEntity.setIsFavorite(FavoriteEnum.NOT_FAVORITE);
        int result = chatTaskMapper.update(chatTaskEntity);
        if (result <= 0) {
            throw new RuntimeException("对话任务取消收藏失败");
        }
        
        log.info("对话任务取消收藏成功，ID: {}", id);
    }
    
    @Override
    public PageResponse<ChatTaskDTO> getFavoriteChatTasks(Long workspaceId, Integer pageNum, Integer pageSize) {
        log.info("查询收藏的对话任务列表，工作空间ID: {}, 页码: {}, 每页数量: {}", workspaceId, pageNum, pageSize);
        
        // 查询收藏的对话任务
        List<ChatTaskEntity> favoriteTasksList = chatTaskMapper.selectByFavoriteStatus(workspaceId, FavoriteEnum.FAVORITE);
        
        // 手动分页处理
        int totalCount = favoriteTasksList.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);
        
        List<ChatTaskEntity> pagedList = startIndex < totalCount ? 
            favoriteTasksList.subList(startIndex, endIndex) : 
            new ArrayList<>();
        
        // 转换结果
        return new PageResponse<>(
                chatTaskPersistenceConvert.fromEntityList(pagedList),
                (long) totalCount,
                pageNum,
                pageSize
        );
    }
    
    @Override
    public PageResponse<ChatTaskDTO> getChatTaskPageByWorkspaceId(Long workspaceId, Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询工作空间下的对话任务，工作空间ID: {}, 页码: {}, 每页数量: {}, 关键词: {}", 
                workspaceId, pageNum, pageSize, keyword);
        
        // 创建分页对象
        Page<ChatTaskEntity> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        Page<ChatTaskEntity> chatTaskPage = chatTaskMapper.selectChatTaskPage(page, workspaceId, keyword);
        
        // 转换结果
        return new PageResponse<>(
                chatTaskPersistenceConvert.fromEntityList(chatTaskPage.getRecords()),
                chatTaskPage.getTotalRow(),
                pageNum,
                pageSize
        );
    }
}