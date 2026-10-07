package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "教师查询条件")
public class TeacherQuery extends PageQuery {

    @Schema(description = "关键字: 工号/姓名/手机号")
    private String keyword;

    private Long deptId;

    @Schema(description = "是否班主任 0否 1是")
    private Integer isHeadTeacher;

    @Schema(description = "职称")
    private String title;

    private Integer status;
}
