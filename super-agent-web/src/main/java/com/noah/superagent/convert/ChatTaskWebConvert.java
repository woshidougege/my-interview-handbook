package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;
import com.noah.superagent.model.ChatTaskDO;
import org.mapstruct.Mapper;

/**
 * 对话任务Web层转换器
 * 负责 Request <-> DO <-> Response 转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ChatTaskWebConvert extends BaseWebConvert<
        ChatTaskCreateRequest,    // 创建请求类型
        ChatTaskUpdateRequest,    // 更新请求类型
        ChatTaskResponse,         // 响应类型
        ChatTaskDO               // 领域对象类型
> {
}
