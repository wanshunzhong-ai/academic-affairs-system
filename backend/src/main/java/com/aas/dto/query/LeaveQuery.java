package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "请假查询条件")
public class LeaveQuery extends PageQuery {

    private String keyword;

    private Long classId;

    private Long studentId;

    @Schema(description = "0待审批 1已通过 2已驳回 3已撤销")
    private Integer status;

    @Schema(description = "请假类型")
    private String leaveType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
