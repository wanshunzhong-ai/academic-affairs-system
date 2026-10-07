package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通用关键字查询条件")
public class CommonQuery extends PageQuery {

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "院系ID")
    private Long deptId;

    @Schema(description = "专业ID")
    private Long majorId;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "课程性质")
    private String courseType;

    @Schema(description = "教室类型")
    private String roomType;
}
