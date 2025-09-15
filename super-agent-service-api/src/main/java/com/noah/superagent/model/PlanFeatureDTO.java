package com.noah.superagent.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 订阅套餐功能特性DTO
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeatureDTO {

    /**
     * 功能描述文本
     */
    private String text;

    /**
     * 是否高亮显示
     */
    private Boolean highlight;

    /**
     * 是否包含此功能（用于前端显示）
     */
    private Boolean included = true;

    /**
     * 构造函数 - 仅文本和高亮
     */
    public PlanFeatureDTO(String text, Boolean highlight) {
        this.text = text;
        this.highlight = highlight;
        this.included = true;
    }
}
