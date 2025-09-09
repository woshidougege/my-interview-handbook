package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 资源类型枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
public enum ResourceTypeEnum implements BaseEnum<String> {

    TOKEN("TOKEN", "Token", "千token"),
    IMAGE_COUNT("IMAGE_COUNT", "图片数量", "张"),
    VIDEO_DURATION("VIDEO_DURATION", "视频时长", "秒"),
    PPT_PAGES("PPT_PAGES", "PPT页数", "页"),
    FUNCTION_TIMES("FUNCTION_TIMES", "功能次数", "次");

    @EnumValue
    @JsonValue
    private final String code;
    private final String name;
    private final String unit;

    ResourceTypeEnum(String code, String name, String unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
    }

    @JsonCreator
    public static ResourceTypeEnum getByCode(String code) {
        return BaseEnum.getByCode(ResourceTypeEnum.class, code);
    }

    public String getDesc() {
        return name;
    }
}