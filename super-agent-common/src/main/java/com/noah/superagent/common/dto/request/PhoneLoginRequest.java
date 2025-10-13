package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PhoneLoginRequest {
    @Schema(description = "手机号码", required = true, example = "13800138000")
    private String phone;

    @Schema(description = "短信验证码", required = true, example = "123456")
    private String phoneCode;

    @Schema(description = "服务代码", required = true, example = "super_agent")
    private String servicecode;
    
    @Schema(description = "是否首次登录", example = "true")
    private Boolean isFirstLogin;
}