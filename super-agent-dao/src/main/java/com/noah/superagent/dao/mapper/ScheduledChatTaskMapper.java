package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * 定时对话任务Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface ScheduledChatTaskMapper extends BaseMapper<ScheduledChatTaskEntity> {

    /**
     * 根据用户ID查询定时对话任务列表
     */
    default List<ScheduledChatTaskEntity> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getUserId).eq(userId));
    }

    /**
     * 根据工作空间ID查询定时对话任务列表
     */
    default List<ScheduledChatTaskEntity> selectByWorkspaceId(Long workspaceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getWorkspaceId).eq(workspaceId));
    }

    /**
     * 分页查询定时对话任务
     */
    default Page<ScheduledChatTaskEntity> selectScheduledChatTaskPage(Page<ScheduledChatTaskEntity> page, Long workspaceId, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getWorkspaceId).eq(workspaceId)
                .and(ScheduledChatTaskEntity::getTaskName).like(keyword, keyword != null)
                .orderBy(ScheduledChatTaskEntity::getCreateTime).desc();

        return paginate(page, query);
    }

    /**
     * 根据状态查询定时对话任务列表
     */
    default List<ScheduledChatTaskEntity> selectByStatus(Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getStatus).eq(status)
                .orderBy(ScheduledChatTaskEntity::getCreateTime).desc());
    }

    /**
     * 查询需要执行的定时对话任务列表
     */
    default List<ScheduledChatTaskEntity> selectTasksToExecute(Date currentTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getStatus).eq(1) // 启用状态
                .and(ScheduledChatTaskEntity::getNextExecutionTime).le(currentTime)
                .orderBy(ScheduledChatTaskEntity::getNextExecutionTime).asc());
    }

    /**
     * 根据任务名称查询定时对话任务
     */
    default ScheduledChatTaskEntity selectByTaskName(Long userId, String taskName) {
        return selectOneByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getUserId).eq(userId)
                .and(ScheduledChatTaskEntity::getTaskName).eq(taskName));
    }

    /**
     * 根据对话任务ID列表查询定时任务列表
     */
    default List<ScheduledChatTaskEntity> selectByChatTaskIds(List<Long> chatTaskIds) {
        if (chatTaskIds == null || chatTaskIds.isEmpty()) {
            return List.of();
        }
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskEntity::getChatTaskId).in(chatTaskIds));
    }
}