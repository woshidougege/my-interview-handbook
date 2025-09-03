package com.noah.superagent.common.dto.killbill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 创建Kill Bill账户请求
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "创建Kill Bill账户请求")
public class CreateAccountRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "张三")
    private String name;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @NotBlank(message = "外部键不能为空")
    @Schema(description = "外部键", example = "user_zhangsan_001")
    private String externalKey;

    @Schema(description = "货币", example = "USD")
    private String currency = "USD";

    @Schema(description = "初始积分金额", example = "1000.00")
    private BigDecimal initialCredit;
}
