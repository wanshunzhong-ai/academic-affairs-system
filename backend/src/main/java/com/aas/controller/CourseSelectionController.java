package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.DataScope;
import com.aas.dto.query.SelectionQuery;
import com.aas.entity.CourseSelection;
import com.aas.security.SecurityUtils;
import com.aas.service.CourseSelectionService;
import com.aas.util.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选课管理
 */
@Tag(name = "15-选课管理")
@RestController
@RequestMapping("/api/selection")
@RequiredArgsConstructor
public class CourseSelectionController {

    private final CourseSelectionService selectionService;

    @Operation(summary = "分页查询选课记录(自动应用数据权限)")
    @PreAuthorize("@ss.hasPerm('selection:list')")
    @GetMapping("/page")
    public Result<PageResult<CourseSelection>> page(SelectionQuery query) {
        return Result.success(selectionService.page(query, DataScope.of(SecurityUtils.getLoginUser())));
    }

    @Operation(summary = "我的选课列表")
    @PreAuthorize("@ss.hasPerm('selection:select')")
    @GetMapping("/my")
    public Result<List<CourseSelection>> my(@RequestParam(required = false) Long semesterId) {
        Long studentId = SecurityUtils.getStudentId();
        if (studentId == null) {
            return Result.success(List.of());
        }
        return Result.success(selectionService.mySelections(studentId, semesterId));
    }

    @Operation(summary = "我已选学分")
    @PreAuthorize("@ss.hasPerm('selection:select')")
    @GetMapping("/my/credit")
    public Result<Map<String, Object>> myCredit(@RequestParam(required = false) Long semesterId) {
        Long studentId = SecurityUtils.getStudentId();
        Map<String, Object> map = new HashMap<>();
        if (studentId == null) {
            map.put("credit", BigDecimal.ZERO);
            return Result.success(map);
        }
        BigDecimal credit = selectionService.myCredit(studentId, semesterId);
        map.put("credit", credit);
        map.put("maxCredit", 30);
        return Result.success(map);
    }

    @Operation(summary = "学生选课")
    @OperLog(module = "选课管理", operation = "学生选课")
    @PreAuthorize("@ss.hasPerm('selection:select')")
    @PostMapping("/select")
    public Result<Void> select(@RequestParam Long offeringId) {
        selectionService.select(SecurityUtils.getStudentId(), offeringId);
        return Result.success("选课成功", null);
    }

    @Operation(summary = "学生批量选课")
    @OperLog(module = "选课管理", operation = "批量选课")
    @PreAuthorize("@ss.hasPerm('selection:select')")
    @PostMapping("/select/batch")
    public Result<Void> batchSelect(@RequestBody List<Long> offeringIds) {
        selectionService.batchSelect(SecurityUtils.getStudentId(), offeringIds);
        return Result.success("选课成功", null);
    }

    @Operation(summary = "学生退课")
    @OperLog(module = "选课管理", operation = "学生退课")
    @PreAuthorize("@ss.hasPerm('selection:select')")
    @PostMapping("/drop")
    public Result<Void> drop(@RequestParam Long offeringId) {
        selectionService.drop(SecurityUtils.getStudentId(), offeringId);
        return Result.success("退课成功", null);
    }

    @Operation(summary = "某开课的选课学生名单")
    @PreAuthorize("@ss.hasAnyPerm('selection:list','score:input')")
    @GetMapping("/offering/{offeringId}")
    public Result<List<CourseSelection>> offeringStudents(@PathVariable Long offeringId) {
        return Result.success(selectionService.offeringStudents(offeringId));
    }

    @Operation(summary = "教务代选课")
    @OperLog(module = "选课管理", operation = "教务代选课")
    @PreAuthorize("@ss.hasPerm('selection:list')")
    @PostMapping("/assign")
    public Result<Void> assign(@RequestParam Long offeringId, @RequestBody List<Long> studentIds) {
        selectionService.assign(offeringId, studentIds);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "移除选课记录")
    @OperLog(module = "选课管理", operation = "移除选课记录")
    @PreAuthorize("@ss.hasPerm('selection:list')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        selectionService.remove(id);
        return Result.success("已移除", null);
    }

    @Operation(summary = "导出选课名单")
    @OperLog(module = "选课管理", operation = "导出选课名单")
    @PreAuthorize("@ss.hasPerm('selection:list')")
    @GetMapping("/export")
    public void export(SelectionQuery query, HttpServletResponse response) {
        query.setPageNum(1);
        query.setPageSize(500);
        var page = selectionService.page(query, DataScope.of(SecurityUtils.getLoginUser()));
        ExcelUtils.export(response, "选课名单", "选课记录",
                new String[]{"学号", "姓名", "班级", "课程编号", "课程名称", "课程性质", "学分", "授课教师", "学期", "选课方式", "状态", "选课时间"},
                page.getRecords(),
                s -> new Object[]{
                        s.getStudentNo(), s.getStudentName(), s.getClassName(), s.getCourseCode(),
                        s.getCourseName(), s.getCourseType(), s.getCredit(), s.getTeacherName(),
                        s.getSemesterName(), s.getSelectType() != null && s.getSelectType() == 2 ? "系统分配" : "学生自选",
                        s.getStatus() != null && s.getStatus() == 1 ? "已选" : "已退选", s.getSelectTime()
                });
    }
}
