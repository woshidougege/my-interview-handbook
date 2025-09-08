package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 任务类型枚举
 *
 * @author Noah
 * @since 1.0.0
 */
@Getter
public enum TaskTypeEnum implements BaseEnum<String> {

    TEXT_GENERATION("TEXT_GENERATION", "文本生成"),
    IMAGE_GENERATION("IMAGE_GENERATION", "文生图"),
    VIDEO_GENERATION("VIDEO_GENERATION", "文生视频"),
    PPT_GENERATION("PPT_GENERATION", "PPT生成"),
    MEETING_MINUTES("MEETING_MINUTES", "会议纪要"),
    DOCUMENT_WRITING("DOCUMENT_WRITING", "文档编写"),
    CODING("CODING", "编码"),
    TRANSLATION("TRANSLATION", "翻译"),
    MIND_MAP("MIND_MAP", "思维导图"),
    DATABASE_ANALYSIS("DATABASE_ANALYSIS", "数据库分析"),
    EXCEL_ANALYSIS("EXCEL_ANALYSIS", "Excel分析"),
    BROWSERUSE("BROWSERUSE", "浏览器使用"),
    DEEPSEARCH("DEEPSEARCH", "深度搜索"),
    SOFTWARE_OPERATION("SOFTWARE_OPERATION", "软件操作");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    TaskTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static TaskTypeEnum getByCode(String code) {
        return BaseEnum.getByCode(TaskTypeEnum.class, code);
    }

    public String getDesc() {
        return description;
    }
}