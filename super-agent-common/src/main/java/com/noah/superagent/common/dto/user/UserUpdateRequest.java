package com.noah.superagent.common.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 用户更新请求DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@Schema(description = "用户更新请求")
public class UserUpdateRequest {

    @Size(min = 2, max = 20, message = "昵称长度必须在2-20个字符之间")
    @Schema(description = "昵称", example = "李四")
    private String nickname;

    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Schema(description = "密码", example = "newpassword")
    private String password;

    @Schema(description = "用户状态 1-正常 0-禁用", example = "1")
    private Integer status;
}
