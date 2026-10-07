package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "选课查询条件")
public class SelectionQuery extends PageQuery {

    private String keyword;

    private Long semesterId;

    private Long offeringId;

    private Long classId;

    private Long courseId;

    private Long studentId;

    private Integer status;
}
