package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.UserCreateRequest;
import com.noah.superagent.common.dto.request.UserUpdateRequest;
import com.noah.superagent.common.dto.response.UserResponse;
import com.noah.superagent.common.enums.UserStatusEnum;
import com.noah.superagent.model.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 用户Web层转换器
 * 负责 Request <-> DTO <-> Response 转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserWebConvert extends BaseWebConvert<
        UserCreateRequest,        // 创建请求类型
        UserUpdateRequest,        // 更新请求类型
        UserResponse,            // 响应类型
        UserDTO                  // 数据传输对象类型
> {

    /**
     * 重写创建请求转DTO方法，忽略ID字段映射
     * 创建操作时ID应该由雪花算法自动生成，不从请求中获取
     */
    @Override
    @Mapping(target = "id", ignore = true)
    UserDTO fromCreateRequest(UserCreateRequest createRequest);

    /**
     * 重写数据传输对象转响应方法，添加状态描述转换
     */
    @Override
    @Mapping(source = "status", target = "statusDesc", qualifiedByName = "statusToDesc")
    UserResponse toResponse(UserDTO userDTO);

    /**
     * 状态码转描述
     */
    @Named("statusToDesc")
    default String statusToDesc(UserStatusEnum status) {
        return status != null ? status.getDesc() : "未知";
    }
}
