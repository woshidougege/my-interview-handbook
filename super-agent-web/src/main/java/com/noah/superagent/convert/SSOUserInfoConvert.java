package com.noah.superagent.convert;

import com.noah.superagent.model.SSOUserInfo;
import com.norinrd.interfaces.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * SSO用户信息转换器
 * 将SDK的UserDto转换为我们的SSOUserInfo
 * 字段名保持一致，MapStruct会自动映射
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface SSOUserInfoConvert {

    SSOUserInfoConvert INSTANCE = Mappers.getMapper(SSOUserInfoConvert.class);

    /**
     * 将SDK的UserDto转换为SSOUserInfo
     * 由于字段名已经保持一致，MapStruct会自动进行映射
     * 忽略SSOUserInfo中额外的字段（它们不存在于UserDto中）
     *
     * @param userDto SDK返回的用户信息
     * @return 转换后的SSO用户信息
     */
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "tokenExpire", ignore = true)
    SSOUserInfo toSSOUserInfo(UserDto userDto);
}
