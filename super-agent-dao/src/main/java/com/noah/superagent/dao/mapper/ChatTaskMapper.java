package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.ChatTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 对话任务Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface ChatTaskMapper extends BaseMapper<ChatTask> {

    /**
     * 根据工作空间ID查询对话任务列表
     */
    default List<ChatTask> selectByWorkspaceId(Long workspaceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTask::getWorkspaceId).eq(workspaceId));
    }

    /**
     * 分页查询对话任务
     */
    default Page<ChatTask> selectChatTaskPage(Page<ChatTask> page, Long workspaceId, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(ChatTask::getWorkspaceId).eq(workspaceId)
                .and(ChatTask::getTitle).like(keyword, keyword != null)
                .orderBy(ChatTask::getCreateTime).desc();

        return paginate(page, query);
    }

    /**
     * 根据会话ID查询对话任务
     */
    default ChatTask selectBySessionId(String sessionId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(ChatTask::getSessionId).eq(sessionId));
    }

    /**
     * 根据状态查询对话任务列表
     */
    default List<ChatTask> selectByStatus(Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTask::getStatus).eq(status)
                .orderBy(ChatTask::getCreateTime).desc());
    }

    /**
     * 根据工作空间ID和状态查询对话任务列表
     */
    default List<ChatTask> selectByWorkspaceIdAndStatus(Long workspaceId, Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTask::getWorkspaceId).eq(workspaceId)
                .and(ChatTask::getStatus).eq(status)
                .orderBy(ChatTask::getCreateTime).desc());
    }

    /**
     * 根据收藏状态查询对话任务列表
     */
    default List<ChatTask> selectByFavoriteStatus(Long workspaceId, Integer isFavorite) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTask::getWorkspaceId).eq(workspaceId)
                .and(ChatTask::getIsFavorite).eq(isFavorite)
                .orderBy(ChatTask::getCreateTime).desc());
    }

    /**
     * 更新对话任务状态
     */
    default int updateStatusBySessionId(String sessionId, Integer status) {
        // 创建仅包含状态字段的更新对象
        ChatTask updateTask = new ChatTask();
        updateTask.setStatus(status);
        return updateByQuery(updateTask, 
                QueryWrapper.create().where(ChatTask::getSessionId).eq(sessionId));
    }

    /**
     * 根据时间范围查询对话任务列表
     */
    default List<ChatTask> selectByTimeRange(Long workspaceId, java.util.Date startTime, java.util.Date endTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTask::getWorkspaceId).eq(workspaceId)
                .and(ChatTask::getCreateTime).ge(startTime)
                .and(ChatTask::getCreateTime).le(endTime)
                .orderBy(ChatTask::getCreateTime).desc());
    }

    /**
     * 查询收藏的对话任务
     */
    default List<ChatTask> selectFavorites(Long workspaceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ChatTask::getWorkspaceId).eq(workspaceId)
                .and(ChatTask::getIsFavorite).eq(1)
                .orderBy(ChatTask::getCreateTime).desc());
    }

    /**
     * 更新对话任务收藏状态
     */
    default int updateFavoriteStatus(String sessionId, Integer isFavorite) {
        // 创建仅包含收藏状态字段的更新对象
        ChatTask updateTask = new ChatTask();
        updateTask.setIsFavorite(isFavorite);
        return updateByQuery(updateTask,
                QueryWrapper.create().where(ChatTask::getSessionId).eq(sessionId));
    }

    /**
     * 批量更新对话任务状态
     */
    default int batchUpdateStatus(List<String> sessionIds, Integer status) {
        // 创建仅包含状态字段的更新对象
        ChatTask updateTask = new ChatTask();
        updateTask.setStatus(status);
        return updateByQuery(updateTask,
                QueryWrapper.create().where(ChatTask::getSessionId).in(sessionIds));
    }
    
    /**
     * 创建对话任务
     */
    default int create(ChatTask chatTask) {
        return insert(chatTask);
    }
}