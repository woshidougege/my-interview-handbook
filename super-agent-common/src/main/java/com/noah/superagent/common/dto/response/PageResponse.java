package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页响应DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    public PageResponse() {}

    public PageResponse(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }
}
