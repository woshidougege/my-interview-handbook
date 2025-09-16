package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 订阅套餐功能特性响应DTO
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "PlanFeatureResponse",
    title = "套餐功能特性", 
    description = "套餐功能特性详情，包含功能描述、是否高亮、是否包含等信息"
)
public class PlanFeatureResponse {

    /**
     * 功能描述文本
     */
    @Schema(description = "功能描述文本", example = "新用户赠送1000积分（90天有效）")
    private String text;

    /**
     * 是否高亮显示
     */
    @Schema(description = "是否高亮显示", example = "true")
    private Boolean highlight;

    /**
     * 是否包含此功能（用于前端显示）
     */
    @Schema(description = "是否包含此功能", example = "true")
    private Boolean included;

    /**
     * 构造函数 - 仅文本和高亮
     */
    public PlanFeatureResponse(String text, Boolean highlight) {
        this.text = text;
        this.highlight = highlight;
        this.included = true;
    }
}
