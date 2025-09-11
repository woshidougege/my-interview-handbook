package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Schema(description = "手机号码", required = true, example = "13800138000")
    private String phone;

    @Schema(description = "短信验证码", required = true, example = "123456")
    private String phoneCode;

    @Schema(description = "SM2加密后的新密码", required = true, example = "304802...")
    private String newPassword;

    @Schema(description = "服务代码", required = true, example = "super_agent")
    private String servicecode;
}