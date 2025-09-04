package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.WorkspaceCreateRequest;
import com.noah.superagent.common.dto.request.WorkspaceUpdateRequest;
import com.noah.superagent.common.dto.response.WorkspaceResponse;
import com.noah.superagent.model.WorkspaceDO;
import org.mapstruct.Mapper;

/**
 * 工作空间Web层转换器
 * 负责 Request <-> DO <-> Response 转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface WorkspaceWebConvert extends BaseWebConvert<
        WorkspaceCreateRequest,   // 创建请求类型
        WorkspaceUpdateRequest,   // 更新请求类型
        WorkspaceResponse,        // 响应类型
        WorkspaceDO              // 领域对象类型
> {
}
