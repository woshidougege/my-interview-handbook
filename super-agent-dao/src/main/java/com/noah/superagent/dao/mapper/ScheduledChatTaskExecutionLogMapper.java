package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.ScheduledChatTaskExecutionLogEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 定时对话任务执行日志Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface ScheduledChatTaskExecutionLogMapper extends BaseMapper<ScheduledChatTaskExecutionLogEntity> {

    /**
     * 根据任务ID查询执行日志列表
     *
     * @param taskId 定时任务ID
     * @return 执行日志列表
     */
    default List<ScheduledChatTaskExecutionLogEntity> selectByTaskId(Long taskId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskExecutionLogEntity::getTaskId).eq(taskId)
                .orderBy(ScheduledChatTaskExecutionLogEntity::getStartTime).desc());
    }

    /**
     * 根据对话任务ID查询执行日志列表
     *
     * @param chatTaskId 对话任务ID
     * @return 执行日志列表
     */
    default List<ScheduledChatTaskExecutionLogEntity> selectByChatTaskId(Long chatTaskId) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskExecutionLogEntity::getChatTaskId).eq(chatTaskId)
                .orderBy(ScheduledChatTaskExecutionLogEntity::getStartTime).desc());
    }

    /**
     * 根据任务名称查询执行日志列表
     *
     * @param taskName 任务名称
     * @return 执行日志列表
     */
    default List<ScheduledChatTaskExecutionLogEntity> selectByTaskName(String taskName) {
        return selectListByQuery(QueryWrapper.create()
                .where(ScheduledChatTaskExecutionLogEntity::getTaskName).eq(taskName)
                .orderBy(ScheduledChatTaskExecutionLogEntity::getStartTime).desc());
    }

    /**
     * 分页查询执行日志
     *
     * @param page     分页对象
     * @param taskName 任务名称
     * @return 分页结果
     */
    default Page<ScheduledChatTaskExecutionLogEntity> selectExecutionLogPage(Page<ScheduledChatTaskExecutionLogEntity> page, String taskName) {
        QueryWrapper query = QueryWrapper.create()
                .where(ScheduledChatTaskExecutionLogEntity::getTaskName).eq(taskName)
                .orderBy(ScheduledChatTaskExecutionLogEntity::getStartTime).desc();

        return paginate(page, query);
    }
}