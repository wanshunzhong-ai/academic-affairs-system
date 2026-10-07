package com.aas.controller;

import com.aas.common.Result;
import com.aas.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 工作台与数据统计
 */
@Tag(name = "20-工作台与统计")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "工作台概览(按当前角色返回不同数据)")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(dashboardService.overview());
    }

    @Operation(summary = "院系学生分布")
    @GetMapping("/dept-distribution")
    public Result<List<Map<String, Object>>> deptDistribution() {
        return Result.success(dashboardService.deptStudentDistribution());
    }

    @Operation(summary = "专业学生分布")
    @GetMapping("/major-distribution")
    public Result<List<Map<String, Object>>> majorDistribution() {
        return Result.success(dashboardService.majorStudentDistribution());
    }

    @Operation(summary = "班级人数分布")
    @GetMapping("/class-distribution")
    public Result<List<Map<String, Object>>> classDistribution() {
        return Result.success(dashboardService.classDistribution());
    }
}
