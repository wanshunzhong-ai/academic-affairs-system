package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询条件")
public class UserQuery extends PageQuery {

    private String keyword;

    @Schema(description = "STUDENT/TEACHER/ADMIN")
    private String userType;

    @Schema(description = "角色标识")
    private String roleCode;

    private Integer status;
}
