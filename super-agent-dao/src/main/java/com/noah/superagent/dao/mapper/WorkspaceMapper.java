package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.Workspace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作空间Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface WorkspaceMapper extends BaseMapper<Workspace> {

    /**
     * 根据用户ID查询工作空间列表
     */
    default List<Workspace> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(Workspace::getUserId).eq(userId));
    }

    /**
     * 根据用户ID查询默认工作空间
     */
    default Workspace selectDefaultByUserId(Long userId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(Workspace::getUserId).eq(userId)
                .and(Workspace::getIsDefault).eq(1));
    }

    /**
     * 根据工作空间名称查询工作空间列表
     */
    default List<Workspace> selectByName(Long userId, String name) {
        return selectListByQuery(QueryWrapper.create()
                .where(Workspace::getUserId).eq(userId)
                .and(Workspace::getName).like(name));
    }

    /**
     * 根据状态查询工作空间列表
     */
    default List<Workspace> selectByStatus(Long userId, Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(Workspace::getUserId).eq(userId)
                .and(Workspace::getStatus).eq(status));
    }

    /**
     * 更新工作空间状态
     */
    default int updateStatusById(Long id, Integer status) {
        // 创建仅包含状态字段的更新对象
        Workspace updateWorkspace = new Workspace();
        updateWorkspace.setStatus(status);
        return updateByQuery(updateWorkspace, 
                QueryWrapper.create().where(Workspace::getId).eq(id));
    }

    /**
     * 查询用户默认工作空间以外的其他工作空间
     */
    default List<Workspace> selectNonDefaultWorkspaces(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(Workspace::getUserId).eq(userId)
                .and(Workspace::getIsDefault).eq(0)
                .orderBy(Workspace::getCreateTime).desc());
    }
    
    /**
     * 分页查询工作空间
     * @param page 分页对象
     * @param keyword 关键词
     * @return 分页结果
     */
    Page<Workspace> selectPlanPage(Page<Workspace> page, @Param("keyword") String keyword);
}