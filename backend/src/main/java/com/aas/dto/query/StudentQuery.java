package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "学生查询条件")
public class StudentQuery extends PageQuery {

    @Schema(description = "关键字: 学号/姓名/手机号")
    private String keyword;

    private Long deptId;

    private Long majorId;

    private Long classId;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "性别 1男 2女")
    private Integer gender;

    @Schema(description = "学籍状态 1在读 2休学 3退学 4毕业")
    private Integer status;
}
