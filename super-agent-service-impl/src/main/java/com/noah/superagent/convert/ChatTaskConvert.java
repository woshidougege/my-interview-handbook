package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.ChatTaskCreateRequest;
import com.noah.superagent.common.dto.response.ChatTaskResponse;
import com.noah.superagent.dao.entity.ChatTask;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 对话任务转换器
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ChatTaskConvert {

    /**
     * 创建请求转实体
     */
    ChatTask toEntity(ChatTaskCreateRequest request);

    /**
     * 实体转响应
     */
    ChatTaskResponse toResponse(ChatTask chatTask);

    /**
     * 实体列表转响应列表
     */
    List<ChatTaskResponse> toResponseList(List<ChatTask> chatTasks);
}