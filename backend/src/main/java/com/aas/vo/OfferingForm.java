package com.aas.vo;

import com.aas.entity.CourseOffering;
import com.aas.entity.CourseSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 开课表单(开课信息 + 排课时间段)
 */
@Data
@Schema(description = "开课表单")
public class OfferingForm {

    @Schema(description = "开课信息")
    private CourseOffering offering;

    @Schema(description = "排课时间段列表")
    private List<CourseSchedule> schedules = new ArrayList<>();
}
