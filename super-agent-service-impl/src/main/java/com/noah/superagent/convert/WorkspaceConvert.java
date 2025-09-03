package com.noah.superagent.convert;

import com.noah.superagent.common.dto.workspace.WorkspaceCreateRequest;
import com.noah.superagent.common.dto.workspace.WorkspaceResponse;
import com.noah.superagent.dao.entity.Workspace;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 工作空间转换器
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface WorkspaceConvert {

    /**
     * 创建请求转实体
     */
    Workspace toEntity(WorkspaceCreateRequest request);

    /**
     * 实体转响应
     */
    WorkspaceResponse toResponse(Workspace workspace);

    /**
     * 实体列表转响应列表
     */
    List<WorkspaceResponse> toResponseList(List<Workspace> workspaces);
}