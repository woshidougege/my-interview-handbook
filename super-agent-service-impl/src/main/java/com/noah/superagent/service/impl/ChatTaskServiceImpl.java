package com.noah.superagent.service.impl;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
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
import cn.hutool.core.util.IdUtil;

import javax.annotation.PostConstruct;
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

    // HanLP初始化状态
    private volatile boolean hanLPInitialized = false;

    @PostConstruct
    public void initHanLP() {
        // 异步初始化HanLP，避免阻塞启动
        new Thread(() -> {
            try {
                log.info("开始初始化HanLP模型...");
                long startTime = System.currentTimeMillis();
                
                // 触发HanLP模型加载
                HanLP.segment("初始化测试");
                HanLP.extractKeyword("初始化测试", 1);
                
                long endTime = System.currentTimeMillis();
                log.info("HanLP模型初始化完成，耗时: {}ms", endTime - startTime);
                
                hanLPInitialized = true;
            } catch (Exception e) {
                log.error("HanLP模型初始化失败，将使用简单算法: {}", e.getMessage());
                hanLPInitialized = true; // 设置为true，使用兜底算法
            }
        }).start();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatTaskDTO createChatTask(ChatTaskDTO chatTaskDO) {
        log.info("开始创建对话任务，标题: {}", chatTaskDO.getTitle());
        
        // DTO -> Entity
        ChatTaskEntity chatTaskEntity = chatTaskPersistenceConvert.toEntity(chatTaskDO);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // 自动生成会话ID（contextId），用于与下游平台通信
        if (!StringUtils.hasText(chatTaskEntity.getContextId())) {
            chatTaskEntity.setContextId("ctx_" + IdUtil.getSnowflakeNextIdStr());
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
        BusinessException.throwIf(existingTask == null, "对话任务不存在: " + id);
        
        int result = chatTaskMapper.deleteById(id);
        BusinessException.throwIf(result <= 0, "对话任务删除失败");
        
        log.info("对话任务删除成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteChatTask(Long id) {
        log.info("收藏对话任务，ID: {}", id);
        
        ChatTaskEntity existingTask = chatTaskMapper.selectOneById(id);
        BusinessException.throwIf(existingTask == null, "对话任务不存在: " + id);
        
        // 更新收藏状态为已收藏
        existingTask.setIsFavorite(FavoriteEnum.FAVORITE);
        int result = chatTaskMapper.update(existingTask);
        BusinessException.throwIf(result <= 0, "对话任务收藏失败");
        
        log.info("对话任务收藏成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteChatTask(Long id) {
        log.info("取消收藏对话任务，ID: {}", id);
        
        ChatTaskEntity existingTask = chatTaskMapper.selectOneById(id);
        BusinessException.throwIf(existingTask == null, "对话任务不存在: " + id);
        
        // 更新收藏状态为未收藏
        existingTask.setIsFavorite(FavoriteEnum.NOT_FAVORITE);
        int result = chatTaskMapper.update(existingTask);
        BusinessException.throwIf(result <= 0, "对话任务取消收藏失败");
        
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
    
    @Override
    public String generateChatTitle(String question) {
        log.info("接收标题生成请求，问题长度: {}", question != null ? question.length() : 0);
        
        if (!StringUtils.hasText(question)) {
            return "新对话";
        }
        
        try {
            // 如果HanLP可用，使用智能算法
            if (hanLPInitialized) {
                return generateIntelligentTitle(question);
            } else {
                log.warn("HanLP未初始化，使用简单算法");
                return generateSimpleTitle(question);
            }
            
        } catch (Exception e) {
            log.error("智能标题生成失败，使用兜底方案: {}", e.getMessage());
            return generateSimpleTitle(question);
        }
    }

    /**
     * 使用HanLP生成智能标题
     */
    private String generateIntelligentTitle(String text) {
        try {
            // 1. 使用HanLP提取关键词（现成功能）
            List<String> keywords = HanLP.extractKeyword(text, 3);
            
            // 2. 使用HanLP进行分词和词性标注（现成功能）
            List<Term> terms = HanLP.segment(text);
            
            // 3. 智能分析生成标题
            return buildSmartTitle(keywords, terms, text);
            
        } catch (Exception e) {
            log.warn("HanLP处理失败: {}", e.getMessage());
            return generateSimpleTitle(text);
        }
    }

    /**
     * 构建智能标题
     */
    private String buildSmartTitle(List<String> keywords, List<Term> terms, String originalText) {
        // 1. 判断是否为问句
        if (isQuestion(originalText)) {
            return generateQuestionTitle(keywords, originalText);
        }
        
        // 2. 判断是否包含动作词（任务请求）
        if (containsActionWords(terms)) {
            return generateTaskTitle(keywords, terms);
        }
        
        // 3. 使用关键词生成标题
        return generateKeywordTitle(keywords, originalText);
    }

    /**
     * 生成问句标题
     */
    private String generateQuestionTitle(List<String> keywords, String text) {
        String questionType = "关于";
        
        // 使用正则识别问句类型（利用HanLP分词后的结果更准确）
        if (text.matches(".*(如何|怎么|怎样).*")) questionType = "如何";
        else if (text.matches(".*(什么|啥).*")) questionType = "什么是";
        else if (text.matches(".*(为什么|为啥).*")) questionType = "为什么";
        else if (text.matches(".*(哪里|哪儿|在哪).*")) questionType = "在哪";
        
        String topic = keywords.isEmpty() ? "问题" : keywords.get(0);
        return questionType + topic;
    }

    /**
     * 生成任务标题
     */
    private String generateTaskTitle(List<String> keywords, List<Term> terms) {
        // 提取动词作为动作
        String action = terms.stream()
            .filter(term -> term.nature.startsWith("v")) // 动词
            .map(term -> term.word)
            .filter(word -> word.length() > 1)
            .findFirst()
            .orElse("处理");
        
        String target = keywords.isEmpty() ? "任务" : keywords.get(0);
        return action + target;
    }

    /**
     * 生成关键词标题
     */
    private String generateKeywordTitle(List<String> keywords, String originalText) {
        if (keywords.isEmpty()) {
            return generateSimpleTitle(originalText);
        }
        
        if (keywords.size() >= 2) {
            return "关于" + String.join("和", keywords.subList(0, 2));
        } else {
            return "关于" + keywords.get(0);
        }
    }

    /**
     * 判断是否为问句
     */
    private boolean isQuestion(String text) {
        return text.contains("？") || text.contains("?") || 
               text.matches(".*(如何|怎么|什么|为什么|哪里|谁|多少).*");
    }

    /**
     * 判断是否包含动作词
     */
    private boolean containsActionWords(List<Term> terms) {
        return terms.stream()
            .anyMatch(term -> term.nature.startsWith("v") && term.word.length() > 1) ||
               terms.stream().anyMatch(term -> 
                   term.word.matches("(帮|请|写|做|解决|修复|优化|分析|设计|开发|实现|创建|生成).*"));
    }

    /**
     * 简单标题生成（兜底方案）
     */
    private String generateSimpleTitle(String text) {
        String cleanText = text.trim();
        
        // 简单的问句识别
        if (isQuestion(cleanText)) {
            if (cleanText.length() > 15) {
                return cleanText.substring(0, 15) + "...";
            }
            return cleanText;
        }
        
        // 通用处理
        if (cleanText.length() > 12) {
            return "关于 " + cleanText.substring(0, 12) + "...";
        }
        
        return cleanText.isEmpty() ? "新对话" : "关于 " + cleanText;
    }
}