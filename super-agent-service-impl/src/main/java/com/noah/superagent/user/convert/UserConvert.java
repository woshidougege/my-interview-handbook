package com.noah.superagent.user.convert;

import com.noah.superagent.common.dto.user.UserCreateRequest;
import com.noah.superagent.common.dto.user.UserResponse;
import com.noah.superagent.common.enums.UserStatusEnum;
import com.noah.superagent.dao.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 用户转换器
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserConvert {

    /**
     * 创建请求转实体
     */
    User toEntity(UserCreateRequest request);

    /**
     * 实体转响应
     */
    @Mapping(source = "status", target = "statusDesc", qualifiedByName = "statusToDesc")
    UserResponse toResponse(User user);

    /**
     * 实体列表转响应列表
     */
    List<UserResponse> toResponseList(List<User> users);

    /**
     * 状态码转描述
     */
    @Named("statusToDesc")
    default String statusToDesc(Integer status) {
        UserStatusEnum statusEnum = UserStatusEnum.getByCode(status);
        return statusEnum != null ? statusEnum.getDesc() : "未知";
    }
}
