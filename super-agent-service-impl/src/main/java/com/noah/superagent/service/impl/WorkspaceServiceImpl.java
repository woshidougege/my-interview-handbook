package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.dao.entity.WorkspaceEntity;
import com.noah.superagent.convert.WorkspaceConvert;
import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.WorkspaceCreateRequest;
import com.noah.superagent.common.dto.request.WorkspaceUpdateRequest;
import com.noah.superagent.common.dto.response.WorkspaceResponse;

import com.noah.superagent.dao.mapper.WorkspaceMapper;
import com.noah.superagent.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工作空间服务实现
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceConvert workspaceConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
        log.info("开始创建工作空间，名称: {}", request.getName());
        
        // 转换为实体
        WorkspaceEntity workspaceEntity = workspaceConvert.toEntity(request);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // 保存工作空间
        int result = workspaceMapper.insertSelective(workspaceEntity);
        if (result <= 0) {
            throw new RuntimeException("工作空间创建失败");
        }
        
        log.info("工作空间创建成功，ID: {}", workspaceEntity.getId());
        return workspaceConvert.toResponse(workspaceEntity);
    }

    @Override
    public WorkspaceResponse getWorkspaceById(Long id) {
        log.info("查询工作空间信息，ID: {}", id);
        
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOneById(id);
        if (workspaceEntity == null) {
            throw new RuntimeException("工作空间不存在: " + id);
        }
        
        return workspaceConvert.toResponse(workspaceEntity);
    }

    @Override
    public PageResponse<WorkspaceResponse> getWorkspacePage(PageRequest request) {
        log.info("分页查询工作空间，页码: {}, 每页数量: {}, 关键词: {}", 
                request.getPageNum(), request.getPageSize(), request.getKeyword());
        
        // 创建分页对象
        Page<WorkspaceEntity> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        // 执行分页查询
        // TODO: 实现具体的分页查询逻辑
        Page<WorkspaceEntity> workspacePage = workspaceMapper.selectPlanPage(page, request.getKeyword());
        
        // 转换结果
        return new PageResponse<>(
                workspaceConvert.toResponseList(workspacePage.getRecords()),
                workspacePage.getTotalRow(),
                request.getPageNum(),
                request.getPageSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse updateWorkspace(Long id, WorkspaceUpdateRequest request) {
        log.info("更新工作空间信息，ID: {}", id);
        
        // 查询工作空间是否存在
        WorkspaceEntity existingWorkspaceEntity = workspaceMapper.selectOneById(id);
        if (existingWorkspaceEntity == null) {
            throw new RuntimeException("工作空间不存在: " + id);
        }
        
        // 更新字段
        if (StringUtils.hasText(request.getName())) {
            existingWorkspaceEntity.setName(request.getName());
        }
        
        if (StringUtils.hasText(request.getDescription())) {
            existingWorkspaceEntity.setDescription(request.getDescription());
        }
        
        if (request.getStatus() != null) {
            existingWorkspaceEntity.setStatus(request.getStatus());
        }
        
        // 执行更新
        int result = workspaceMapper.update(existingWorkspaceEntity);
        if (result <= 0) {
            throw new RuntimeException("工作空间更新失败");
        }
        
        log.info("工作空间更新成功，ID: {}", id);
        return workspaceConvert.toResponse(existingWorkspaceEntity);
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
}