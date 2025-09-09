package com.noah.superagent.common.dto.request;

import com.noah.superagent.common.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Size;

/**
 * 用户更新请求DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户更新请求")
public class UserUpdateRequest extends BaseRequest {

    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    @Schema(description = "用户名", example = "李四")
    private String username;

    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Schema(description = "密码", example = "newpassword")
    private String password;

    @Schema(description = "用户状态 ACTIVE-正常 DISABLED-禁用", example = "ACTIVE")
    private UserStatusEnum status;
}
