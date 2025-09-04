package com.noah.superagent.convert;

import java.util.List;

/**
 * Web层转换器基接口
 * 负责 Request <-> DTO <-> Response 转换
 * 
 * @param <CreateRequest> 创建请求类型
 * @param <UpdateRequest> 更新请求类型
 * @param <Response> 响应类型
 * @param <DTO> 数据传输对象类型
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface BaseWebConvert<CreateRequest, UpdateRequest, Response, DTO> {

    // ==================== Request -> DTO ====================
    
    /**
     * 创建请求转数据传输对象
     */
    DTO fromCreateRequest(CreateRequest createRequest);

    /**
     * 更新请求转数据传输对象
     */
    DTO fromUpdateRequest(UpdateRequest updateRequest);

    // ==================== DTO -> Response ====================
    
    /**
     * 数据传输对象转响应
     */
    Response toResponse(DTO dataTransferObject);

    /**
     * 数据传输对象列表转响应列表
     */
    List<Response> toResponseList(List<DTO> dataTransferObjects);
}
