package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.DataScope;
import com.aas.dto.query.AttendanceQuery;
import com.aas.entity.AttAttendance;
import com.aas.security.SecurityUtils;
import com.aas.service.AttAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理
 */
@Tag(name = "17-考勤管理")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttAttendanceController {

    private final AttAttendanceService attendanceService;

    @Operation(summary = "分页查询考勤记录(自动应用数据权限)")
    @PreAuthorize("@ss.hasAnyPerm('attendance:list','leave:my')")
    @GetMapping("/page")
    public Result<PageResult<AttAttendance>> page(AttendanceQuery query) {
        return Result.success(attendanceService.page(query, DataScope.of(SecurityUtils.getLoginUser())));
    }

    @Operation(summary = "我的考勤汇总")
    @PreAuthorize("@ss.hasAnyPerm('attendance:list','leave:my')")
    @GetMapping("/my")
    public Result<Map<String, Object>> my() {
        return Result.success(attendanceService.mySummary(SecurityUtils.getStudentId()));
    }

    @Operation(summary = "新增考勤记录")
    @OperLog(module = "考勤管理", operation = "新增考勤记录")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @PostMapping
    public Result<Void> create(@RequestBody AttAttendance record) {
        attendanceService.create(record);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改考勤记录")
    @OperLog(module = "考勤管理", operation = "修改考勤记录")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @PutMapping
    public Result<Void> update(@RequestBody AttAttendance record) {
        attendanceService.update(record);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除考勤记录")
    @OperLog(module = "考勤管理", operation = "删除考勤记录")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除考勤记录")
    @OperLog(module = "考勤管理", operation = "批量删除考勤记录")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        attendanceService.deleteBatch(ids);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "生成点名草稿(某开课的选课学生)")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @GetMapping("/draft/{offeringId}")
    public Result<List<Map<String, Object>>> draft(@PathVariable Long offeringId) {
        return Result.success(attendanceService.draftForOffering(offeringId));
    }

    @Operation(summary = "批量保存考勤(一次点名)")
    @OperLog(module = "考勤管理", operation = "批量记录考勤")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @PostMapping("/batch/{offeringId}")
    public Result<Integer> batchRecord(@PathVariable Long offeringId,
                                       @RequestParam(required = false) String date,
                                       @RequestBody List<AttAttendance> records) {
        LocalDate attendDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        return Result.success("已保存", attendanceService.batchRecord(offeringId, attendDate, records));
    }

    @Operation(summary = "班级考勤统计")
    @PreAuthorize("@ss.hasPerm('attendance:list')")
    @GetMapping("/stat/class/{classId}")
    public Result<List<Map<String, Object>>> classStats(@PathVariable Long classId) {
        return Result.success(attendanceService.classStats(classId));
    }
}
