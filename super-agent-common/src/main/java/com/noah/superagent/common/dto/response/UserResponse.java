package com.noah.superagent.common.dto.response;

import com.noah.superagent.common.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户响应DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户信息响应")
public class UserResponse extends BaseResponse {

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户状态 ACTIVE-正常 DISABLED-禁用", example = "ACTIVE")
    private UserStatusEnum status;

    @Schema(description = "用户状态描述", example = "正常")
    private String statusDesc;

}
