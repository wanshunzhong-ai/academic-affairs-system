package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "考勤查询条件")
public class AttendanceQuery extends PageQuery {

    private String keyword;

    private Long classId;

    private Long offeringId;

    private Long studentId;

    @Schema(description = "1出勤 2迟到 3早退 4缺勤 5请假")
    private Integer attendType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
