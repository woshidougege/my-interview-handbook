package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.TaskTypeEnum;
import com.noah.superagent.dao.entity.ResourceUsageRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.noah.superagent.dao.entity.table.ResourceUsageRecordEntityTableDef.RESOURCE_USAGE_RECORD_ENTITY;

/**
 * 资源使用记录Mapper
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface ResourceUsageRecordMapper extends BaseMapper<ResourceUsageRecordEntity> {

    /**
     * 根据请求ID查询记录（用于幂等性检查）
     */
    default List<ResourceUsageRecordEntity> findByRequestId(String requestId) {
        return selectListByQuery(QueryWrapper.create()
                .select()
                .where(RESOURCE_USAGE_RECORD_ENTITY.REQUEST_ID.eq(requestId))
        );
    }

    /**
     * 根据用户ID和时间范围查询使用记录
     */
    default List<ResourceUsageRecordEntity> findByUserIdAndTimeRange(Long userId, 
            String startTime, String endTime) {
        return selectListByQuery(QueryWrapper.create()
                .select()
                .where(RESOURCE_USAGE_RECORD_ENTITY.USER_ID.eq(userId))
                .and(RESOURCE_USAGE_RECORD_ENTITY.CREATE_TIME.between(startTime, endTime))
                .orderBy(RESOURCE_USAGE_RECORD_ENTITY.CREATE_TIME.desc())
        );
    }

    /**
     * 根据任务类型统计使用量
     */
    default List<ResourceUsageRecordEntity> findByTaskType(TaskTypeEnum taskType) {
        return selectListByQuery(QueryWrapper.create()
                .select()
                .where(RESOURCE_USAGE_RECORD_ENTITY.TASK_TYPE.eq(taskType))
                .orderBy(RESOURCE_USAGE_RECORD_ENTITY.CREATE_TIME.desc())
        );
    }
}
