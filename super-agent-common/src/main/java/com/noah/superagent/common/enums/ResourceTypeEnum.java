package com.noah.superagent.common.enums;

import lombok.Getter;

/**
 * 资源类型枚举
 *
 * @author Noah
 * @since 1.0.0
 */
@Getter
public enum ResourceTypeEnum {

    TOKEN("TOKEN", "Token", "千token"),
    IMAGE_COUNT("IMAGE_COUNT", "图片数量", "张"),
    VIDEO_DURATION("VIDEO_DURATION", "视频时长", "秒"),
    PPT_PAGES("PPT_PAGES", "PPT页数", "页"),
    FUNCTION_TIMES("FUNCTION_TIMES", "功能次数", "次");

    private final String code;
    private final String name;
    private final String unit;

    ResourceTypeEnum(String code, String name, String unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
    }

    public static ResourceTypeEnum of(String code) {
        for (ResourceTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}