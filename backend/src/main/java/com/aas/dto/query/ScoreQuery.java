package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "成绩查询条件")
public class ScoreQuery extends PageQuery {

    private String keyword;

    private Long semesterId;

    private Long offeringId;

    private Long classId;

    private Long courseId;

    private Long studentId;

    /** 0未录入 1已录入 2已发布 */
    private Integer status;

    @Schema(description = "是否只看不及格")
    private Boolean onlyFail;
}
