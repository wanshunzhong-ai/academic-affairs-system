package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "开课查询条件")
public class OfferingQuery extends PageQuery {

    private String keyword;

    private Long semesterId;

    private Long teacherId;

    private Long classId;

    private Long courseId;

    @Schema(description = "是否公共选修 0否 1是")
    private Integer isPublic;

    /** 0未发布 1已发布 2已结课 */
    private Integer status;
}
