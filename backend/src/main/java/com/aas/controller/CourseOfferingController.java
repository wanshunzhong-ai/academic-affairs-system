package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.PageQuery;
import com.aas.dto.query.OfferingQuery;
import com.aas.entity.CourseOffering;
import com.aas.entity.CourseSchedule;
import com.aas.security.SecurityUtils;
import com.aas.service.CourseOfferingService;
import com.aas.vo.OfferingForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 开课与排课管理
 */
@Tag(name = "14-开课与排课管理")
@RestController
@RequestMapping("/api/offering")
@RequiredArgsConstructor
public class CourseOfferingController {

    private final CourseOfferingService offeringService;

    @Operation(summary = "分页查询开课安排")
    @PreAuthorize("@ss.hasAnyPerm('offering:list','schedule:view')")
    @GetMapping("/page")
    public Result<PageResult<CourseOffering>> page(OfferingQuery query) {
        return Result.success(offeringService.page(query));
    }

    @Operation(summary = "开课详情(含排课)")
    @GetMapping("/{id}")
    public Result<CourseOffering> detail(@PathVariable Long id) {
        return Result.success(offeringService.detail(id));
    }

    @Operation(summary = "开课排课时间段")
    @GetMapping("/{id}/schedules")
    public Result<List<CourseSchedule>> schedules(@PathVariable Long id) {
        return Result.success(offeringService.schedules(id));
    }

    @Operation(summary = "新增开课(含排课，自动校验冲突)")
    @OperLog(module = "开课管理", operation = "新增开课")
    @PreAuthorize("@ss.hasPerm('offering:list')")
    @PostMapping
    public Result<Long> create(@RequestBody OfferingForm form) {
        return Result.success("开课成功", offeringService.create(form.getOffering(), form.getSchedules()));
    }

    @Operation(summary = "修改开课")
    @OperLog(module = "开课管理", operation = "修改开课")
    @PreAuthorize("@ss.hasPerm('offering:list')")
    @PutMapping
    public Result<Void> update(@RequestBody OfferingForm form) {
        offeringService.update(form.getOffering(), form.getSchedules());
        return Result.success("修改成功", null);
    }

    @Operation(summary = "单独保存排课")
    @OperLog(module = "开课管理", operation = "排课")
    @PreAuthorize("@ss.hasPerm('offering:list')")
    @PutMapping("/{id}/schedules")
    public Result<Void> saveSchedules(@PathVariable Long id, @RequestBody List<CourseSchedule> schedules) {
        offeringService.saveSchedules(id, schedules);
        return Result.success("排课成功", null);
    }

    @Operation(summary = "删除开课")
    @OperLog(module = "开课管理", operation = "删除开课")
    @PreAuthorize("@ss.hasPerm('offering:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        offeringService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除开课")
    @OperLog(module = "开课管理", operation = "批量删除开课")
    @PreAuthorize("@ss.hasPerm('offering:list')")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        offeringService.deleteBatch(ids);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "发布/结课")
    @OperLog(module = "开课管理", operation = "修改开课状态")
    @PreAuthorize("@ss.hasPerm('offering:list')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        offeringService.changeStatus(id, status);
        return Result.success("操作成功", null);
    }

    // ==================== 课表查询 ====================

    @Operation(summary = "我的课表(学生/教师自动识别)")
    @GetMapping("/timetable")
    public Result<List<CourseOffering>> myTimetable(@RequestParam(required = false) Long semesterId) {
        var user = SecurityUtils.getLoginUser();
        if (user.getStudentId() != null) {
            return Result.success(offeringService.studentTimetable(user.getStudentId(), semesterId));
        }
        if (user.getTeacherId() != null) {
            return Result.success(offeringService.teacherTimetable(user.getTeacherId(), semesterId));
        }
        return Result.success(List.of());
    }

    @Operation(summary = "学生课表")
    @PreAuthorize("@ss.hasPerm('schedule:view')")
    @GetMapping("/timetable/student/{studentId}")
    public Result<List<CourseOffering>> studentTimetable(@PathVariable Long studentId,
                                                         @RequestParam(required = false) Long semesterId) {
        return Result.success(offeringService.studentTimetable(studentId, semesterId));
    }

    @Operation(summary = "教师课表")
    @GetMapping("/timetable/teacher/{teacherId}")
    public Result<List<CourseOffering>> teacherTimetable(@PathVariable Long teacherId,
                                                         @RequestParam(required = false) Long semesterId) {
        return Result.success(offeringService.teacherTimetable(teacherId, semesterId));
    }

    @Operation(summary = "班级课表")
    @GetMapping("/timetable/class/{classId}")
    public Result<List<CourseOffering>> classTimetable(@PathVariable Long classId,
                                                       @RequestParam(required = false) Long semesterId) {
        return Result.success(offeringService.classTimetable(classId, semesterId));
    }

    @Operation(summary = "公共选修课列表")
    @GetMapping("/public")
    public Result<List<CourseOffering>> publicOfferings(@RequestParam(required = false) Long semesterId) {
        return Result.success(offeringService.listPublicOfferings(semesterId));
    }

    @Operation(summary = "选课统计概览")
    @GetMapping("/{id}/stat")
    public Result<java.util.Map<String, Object>> stat(@PathVariable Long id) {
        CourseOffering offering = offeringService.detail(id);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("capacity", offering.getCapacity());
        map.put("selectedCount", offering.getSelectedCount());
        map.put("rate", offering.getCapacity() == null || offering.getCapacity() == 0 ? 0
                : Math.round(offering.getSelectedCount() * 1000.0 / offering.getCapacity()) / 10.0);
        return Result.success(map);
    }

    @Operation(summary = "可选开课分页(学生选课页)")
    @PreAuthorize("@ss.hasAnyPerm('selection:select','selection:list')")
    @GetMapping("/selectable")
    public Result<PageResult<CourseOffering>> selectable(@RequestParam(required = false) Long semesterId,
                                                         @RequestParam(required = false) String keyword,
                                                         PageQuery pageQuery) {
        var user = SecurityUtils.getLoginUser();
        return Result.success(offeringService.selectableOfferings(
                user.getStudentId(), user.getClassId(), semesterId, keyword, pageQuery));
    }
}
