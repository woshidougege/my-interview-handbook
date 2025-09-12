package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 对话标题生成请求
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "对话标题生成请求")
public class ChatTitleGenerateRequest extends BaseRequest {

    @Schema(description = "用户问题", example = "如何学习Java编程？")
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题长度不能超过1000个字符")
    private String question;

    @Schema(description = "是否异步生成", example = "false")
    private Boolean async = false;
}
