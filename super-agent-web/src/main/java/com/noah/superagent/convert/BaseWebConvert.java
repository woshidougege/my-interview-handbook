package com.noah.superagent.convert;

import java.util.List;

/**
 * Web层转换器基接口
 * 负责 Request <-> DO <-> Response 转换
 * 
 * @param <CreateRequest> 创建请求类型
 * @param <UpdateRequest> 更新请求类型
 * @param <Response> 响应类型
 * @param <DO> 领域对象类型
 *
 * @author System
 * @since 1.0.0
 */
public interface BaseWebConvert<CreateRequest, UpdateRequest, Response, DO> {

    // ==================== Request -> DO ====================
    
    /**
     * 创建请求转领域对象
     */
    DO fromCreateRequest(CreateRequest createRequest);

    /**
     * 更新请求转领域对象
     */
    DO fromUpdateRequest(UpdateRequest updateRequest);

    // ==================== DO -> Response ====================
    
    /**
     * 领域对象转响应
     */
    Response toResponse(DO domainObject);

    /**
     * 领域对象列表转响应列表
     */
    List<Response> toResponseList(List<DO> domainObjects);
}
