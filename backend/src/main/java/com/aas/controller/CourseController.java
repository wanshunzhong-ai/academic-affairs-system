package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.CourseCourse;
import com.aas.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程管理
 */
@Tag(name = "13-课程管理")
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "分页查询课程")
    @PreAuthorize("@ss.hasPerm('course:list')")
    @GetMapping("/page")
    public Result<PageResult<CourseCourse>> page(CommonQuery query) {
        return Result.success(courseService.page(query));
    }

    @Operation(summary = "课程列表(下拉用)")
    @GetMapping("/options")
    public Result<List<CourseCourse>> options() {
        return Result.success(courseService.listAll());
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public Result<CourseCourse> detail(@PathVariable Long id) {
        return Result.success(courseService.detail(id));
    }

    @Operation(summary = "新增课程")
    @OperLog(module = "课程管理", operation = "新增课程")
    @PreAuthorize("@ss.hasPerm('course:list')")
    @PostMapping
    public Result<Long> create(@RequestBody CourseCourse course) {
        return Result.success("新增成功", courseService.create(course));
    }

    @Operation(summary = "修改课程")
    @OperLog(module = "课程管理", operation = "修改课程")
    @PreAuthorize("@ss.hasPerm('course:list')")
    @PutMapping
    public Result<Void> update(@RequestBody CourseCourse course) {
        courseService.update(course);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除课程")
    @OperLog(module = "课程管理", operation = "删除课程")
    @PreAuthorize("@ss.hasPerm('course:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用课程")
    @OperLog(module = "课程管理", operation = "修改课程状态")
    @PreAuthorize("@ss.hasPerm('course:list')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        courseService.changeStatus(id, status);
        return Result.success("操作成功", null);
    }
}
