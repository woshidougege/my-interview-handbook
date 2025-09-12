package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.dao.entity.WorkspaceEntity;
import com.noah.superagent.convert.WorkspacePersistenceConvert;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.dao.mapper.WorkspaceMapper;
import com.noah.superagent.model.WorkspaceDTO;
import com.noah.superagent.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工作空间服务实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspacePersistenceConvert workspacePersistenceConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDTO createWorkspace(WorkspaceDTO workspaceDO) {
        log.info("开始创建工作空间，名称: {}", workspaceDO.getName());
        
        // DTO -> Entity
        WorkspaceEntity workspaceEntity = workspacePersistenceConvert.toEntity(workspaceDO);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // 保存工作空间
        int result = workspaceMapper.insertSelective(workspaceEntity);
        if (result <= 0) {
            throw new RuntimeException("工作空间创建失败");
        }
        
        log.info("工作空间创建成功，ID: {}", workspaceEntity.getId());
        // Entity -> DTO
        return workspacePersistenceConvert.fromEntity(workspaceEntity);
    }

    @Override
    public WorkspaceDTO getWorkspaceById(Long id) {
        log.info("查询工作空间信息，ID: {}", id);
        
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOneById(id);
        if (workspaceEntity == null) {
            throw new RuntimeException("工作空间不存在: " + id);
        }
        
        // Entity -> DTO
        return workspacePersistenceConvert.fromEntity(workspaceEntity);
    }

    @Override
    public PageResponse<WorkspaceDTO> getWorkspacePage(Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询工作空间，页码: {}, 每页数量: {}, 关键词: {}", 
                pageNum, pageSize, keyword);
        
        // 创建分页对象
        Page<WorkspaceEntity> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        Page<WorkspaceEntity> workspacePage = workspaceMapper.selectWorkspacePage(page, keyword);
        
        // 转换结果
        return new PageResponse<>(
                workspacePersistenceConvert.fromEntityList(workspacePage.getRecords()),
                workspacePage.getTotalRow(),
                pageNum,
                pageSize
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDTO updateWorkspace(WorkspaceDTO workspaceDO) {
        log.info("更新工作空间信息，ID: {}", workspaceDO.getId());
        
        // 查询工作空间是否存在
        WorkspaceEntity existingWorkspaceEntity = workspaceMapper.selectOneById(workspaceDO.getId());
        if (existingWorkspaceEntity == null) {
            throw new RuntimeException("工作空间不存在: " + workspaceDO.getId());
        }
        
        // 更新字段
        if (StringUtils.hasText(workspaceDO.getName())) {
            existingWorkspaceEntity.setName(workspaceDO.getName());
        }
        
        if (StringUtils.hasText(workspaceDO.getDescription())) {
            existingWorkspaceEntity.setDescription(workspaceDO.getDescription());
        }
        
        if (workspaceDO.getStatus() != null) {
            existingWorkspaceEntity.setStatus(workspaceDO.getStatus());
        }
        
        // 执行更新
        int result = workspaceMapper.update(existingWorkspaceEntity);
        if (result <= 0) {
            throw new RuntimeException("工作空间更新失败");
        }
        
        log.info("工作空间更新成功，ID: {}", workspaceDO.getId());
        // Entity -> DTO
        return workspacePersistenceConvert.fromEntity(existingWorkspaceEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long id) {
        log.info("删除工作空间，ID: {}", id);
        
        // 检查工作空间是否存在
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOneById(id);
        if (workspaceEntity == null) {
            throw new RuntimeException("工作空间不存在: " + id);
        }
        
        // 执行删除
        int result = workspaceMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("工作空间删除失败");
        }
        
        log.info("工作空间删除成功，ID: {}", id);
    }

    @Override
    public List<WorkspaceDTO> getWorkspacesByUserId(Long userId) {
        log.info("根据用户ID查询工作空间列表，用户ID: {}", userId);
        
        // 根据用户ID查询工作空间列表
        List<WorkspaceEntity> workspaceEntities = workspaceMapper.selectByUserId(userId);
        
        // 转换结果
        return workspacePersistenceConvert.fromEntityList(workspaceEntities);
    }
}