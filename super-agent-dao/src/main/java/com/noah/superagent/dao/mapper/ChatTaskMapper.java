package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 对话任务Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface ChatTaskMapper extends BaseMapper<ChatTaskEntity> {

    /**
     * 根据工作空间ID查询对话任务列表
     */
    default List<ChatTaskEntity> selectByWorkspaceId(Long workspaceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getWorkspaceId).eq(workspaceId));
    }

    /**
     * 分页查询对话任务
     */
    default Page<ChatTaskEntity> selectChatTaskPage(Page<ChatTaskEntity> page, Long workspaceId, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(ChatTaskEntity::getWorkspaceId).eq(workspaceId)
                .and(ChatTaskEntity::getTitle).like(keyword, keyword != null)
                .orderBy(ChatTaskEntity::getCreateTime).desc();

        return paginate(page, query);
    }

    /**
     * 根据会话ID查询对话任务
     */
    default ChatTaskEntity selectBySessionId(String sessionId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getSessionId).eq(sessionId));
    }

    /**
     * 根据状态查询对话任务列表
     */
    default List<ChatTaskEntity> selectByStatus(Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getStatus).eq(status)
                .orderBy(ChatTaskEntity::getCreateTime).desc());
    }

    /**
     * 根据工作空间ID和状态查询对话任务列表
     */
    default List<ChatTaskEntity> selectByWorkspaceIdAndStatus(Long workspaceId, Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getWorkspaceId).eq(workspaceId)
                .and(ChatTaskEntity::getStatus).eq(status)
                .orderBy(ChatTaskEntity::getCreateTime).desc());
    }

    /**
     * 根据收藏状态查询对话任务列表
     */
    default List<ChatTaskEntity> selectByFavoriteStatus(Long workspaceId, Integer isFavorite) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getWorkspaceId).eq(workspaceId)
                .and(ChatTaskEntity::getIsFavorite).eq(isFavorite)
                .orderBy(ChatTaskEntity::getCreateTime).desc());
    }

    /**
     * 更新对话任务状态
     */
    default int updateStatusBySessionId(String sessionId, Integer status) {
        // 创建仅包含状态字段的更新对象
        ChatTaskEntity updateTask = new ChatTaskEntity();
        updateTask.setStatus(status);
        return updateByQuery(updateTask, 
                QueryWrapper.create().where(ChatTaskEntity::getSessionId).eq(sessionId));
    }

    /**
     * 根据时间范围查询对话任务列表
     */
    default List<ChatTaskEntity> selectByTimeRange(Long workspaceId, java.util.Date startTime, java.util.Date endTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getWorkspaceId).eq(workspaceId)
                .and(ChatTaskEntity::getCreateTime).ge(startTime)
                .and(ChatTaskEntity::getCreateTime).le(endTime)
                .orderBy(ChatTaskEntity::getCreateTime).desc());
    }

    /**
     * 查询收藏的对话任务
     */
    default List<ChatTaskEntity> selectFavorites(Long workspaceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTaskEntity::getWorkspaceId).eq(workspaceId)
                .and(ChatTaskEntity::getIsFavorite).eq(1)
                .orderBy(ChatTaskEntity::getCreateTime).desc());
    }

    /**
     * 更新对话任务收藏状态
     */
    default int updateFavoriteStatus(String sessionId, Integer isFavorite) {
        // 创建仅包含收藏状态字段的更新对象
        ChatTaskEntity updateTask = new ChatTaskEntity();
        updateTask.setIsFavorite(isFavorite);
        return updateByQuery(updateTask,
                QueryWrapper.create().where(ChatTaskEntity::getSessionId).eq(sessionId));
    }

    /**
     * 批量更新对话任务状态
     */
    default int batchUpdateStatus(List<String> sessionIds, Integer status) {
        // 创建仅包含状态字段的更新对象
        ChatTaskEntity updateTask = new ChatTaskEntity();
        updateTask.setStatus(status);
        return updateByQuery(updateTask,
                QueryWrapper.create().where(ChatTaskEntity::getSessionId).in(sessionIds));
    }
    
    /**
     * 创建对话任务
     */
    default int create(ChatTaskEntity chatTaskEntity) {
        return insert(chatTaskEntity);
    }
}