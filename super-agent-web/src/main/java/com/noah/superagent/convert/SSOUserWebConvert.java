package com.noah.superagent.convert;

import com.noah.superagent.common.dto.response.UserResponse;
import com.noah.superagent.model.SSOUserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * SSO用户Web层转换器
 * 负责 SSOUserInfo <-> UserResponse 转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface SSOUserWebConvert {

    /**
     * SSO用户信息转响应DTO
     */
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "status", target = "statusDesc", qualifiedByName = "statusToDesc")
    UserResponse toResponse(SSOUserInfo ssoUserInfo);

    /**
     * 状态码转描述
     */
    @Named("statusToDesc")
    default String statusToDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "正常" : "禁用";
    }
}
