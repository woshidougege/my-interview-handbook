package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.WorkspaceStatusEnum;
import com.noah.superagent.dao.entity.WorkspaceEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工作空间Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface WorkspaceMapper extends BaseMapper<WorkspaceEntity> {

    /**
     * 根据用户ID查询工作空间列表
     */
    default List<WorkspaceEntity> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(WorkspaceEntity::getUserId).eq(userId));
    }

    /**
     * 根据用户ID查询默认工作空间列表
     */
    default List<WorkspaceEntity> selectDefaultByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(WorkspaceEntity::getUserId).eq(userId)
                .and(WorkspaceEntity::getIsDefault).eq(1));
    }

    /**
     * 根据工作空间名称查询工作空间列表
     */
    default List<WorkspaceEntity> selectByName(Long userId, String name) {
        return selectListByQuery(QueryWrapper.create()
                .where(WorkspaceEntity::getUserId).eq(userId)
                .and(WorkspaceEntity::getName).like(name));
    }

    /**
     * 根据状态查询工作空间列表
     */
    default List<WorkspaceEntity> selectByStatus(Long userId, Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(WorkspaceEntity::getUserId).eq(userId)
                .and(WorkspaceEntity::getStatus).eq(status));
    }

    /**
     * 更新工作空间状态
     */
    default int updateStatusById(Long id, WorkspaceStatusEnum status) {
        // 创建仅包含状态字段的更新对象
        WorkspaceEntity updateWorkspaceEntity = new WorkspaceEntity();
        updateWorkspaceEntity.setStatus(status);
        return updateByQuery(updateWorkspaceEntity,
                QueryWrapper.create().where(WorkspaceEntity::getId).eq(id));
    }

    /**
     * 查询用户默认工作空间以外的其他工作空间
     */
    default List<WorkspaceEntity> selectNonDefaultWorkspaces(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(WorkspaceEntity::getUserId).eq(userId)
                .and(WorkspaceEntity::getIsDefault).eq(0)
                .orderBy(WorkspaceEntity::getCreateTime).desc());
    }
    
    /**
     * 分页查询工作空间
     * @param page 分页对象
     * @param keyword 关键词
     * @return 分页结果
     */
    default Page<WorkspaceEntity> selectWorkspacePage(Page<WorkspaceEntity> page, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(WorkspaceEntity::getName).like(keyword, keyword != null)
                .or(WorkspaceEntity::getDescription).like(keyword, keyword != null)
                .orderBy(WorkspaceEntity::getCreateTime).desc();
        
        return paginate(page, query);
    }
}