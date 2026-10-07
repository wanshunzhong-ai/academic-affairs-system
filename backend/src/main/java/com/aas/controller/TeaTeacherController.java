package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.TeacherQuery;
import com.aas.entity.TeaTeacher;
import com.aas.security.SecurityUtils;
import com.aas.service.TeaTeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师 / 班主任管理
 */
@Tag(name = "12-教师与班主任管理")
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeaTeacherController {

    private final TeaTeacherService teacherService;

    @Operation(summary = "分页查询教师")
    @PreAuthorize("@ss.hasAnyPerm('student:query','class:list')")
    @GetMapping("/page")
    public Result<PageResult<TeaTeacher>> page(TeacherQuery query) {
        return Result.success(teacherService.page(query));
    }

    @Operation(summary = "教师列表(下拉用)")
    @GetMapping("/options")
    public Result<List<TeaTeacher>> options(@RequestParam(required = false) Long deptId) {
        return Result.success(teacherService.listAll(deptId));
    }

    @Operation(summary = "班主任列表(下拉用)")
    @GetMapping("/head-teachers")
    public Result<List<TeaTeacher>> headTeachers() {
        return Result.success(teacherService.listHeadTeachers());
    }

    @Operation(summary = "教师详情")
    @GetMapping("/{id}")
    public Result<TeaTeacher> detail(@PathVariable Long id) {
        return Result.success(teacherService.detail(id));
    }

    @Operation(summary = "我的教师信息")
    @GetMapping("/my")
    public Result<TeaTeacher> my() {
        return Result.success(teacherService.myProfile(SecurityUtils.getUserId()));
    }

    @Operation(summary = "新增教师(班主任自动开通账号)")
    @OperLog(module = "教师管理", operation = "新增教师")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @PostMapping
    public Result<Long> create(@RequestBody TeaTeacher teacher) {
        return Result.success("新增成功", teacherService.create(teacher));
    }

    @Operation(summary = "修改教师")
    @OperLog(module = "教师管理", operation = "修改教师")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @PutMapping
    public Result<Void> update(@RequestBody TeaTeacher teacher) {
        teacherService.update(teacher);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除教师")
    @OperLog(module = "教师管理", operation = "删除教师")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "分配管理班级(班主任)")
    @OperLog(module = "教师管理", operation = "分配班主任班级")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @PutMapping("/{id}/classes")
    public Result<Void> assignClasses(@PathVariable Long id, @RequestBody List<Long> classIds) {
        teacherService.assignClasses(id, classIds);
        return Result.success("分配成功", null);
    }

    @Operation(summary = "修改在职状态")
    @OperLog(module = "教师管理", operation = "修改教师状态")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        teacherService.changeStatus(id, status);
        return Result.success("操作成功", null);
    }
}
