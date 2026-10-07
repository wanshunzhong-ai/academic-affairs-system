package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseClass;
import com.aas.security.SecurityUtils;
import com.aas.service.BaseClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级管理
 */
@Tag(name = "08-班级管理")
@RestController
@RequestMapping("/api/base/class")
@RequiredArgsConstructor
public class BaseClassController {

    private final BaseClassService classService;

    @Operation(summary = "分页查询班级")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @GetMapping("/page")
    public Result<PageResult<BaseClass>> page(CommonQuery query) {
        return Result.success(classService.page(query));
    }

    @Operation(summary = "班级列表(下拉用)")
    @GetMapping("/options")
    public Result<List<BaseClass>> options() {
        return Result.success(classService.listAll());
    }

    @Operation(summary = "我管理的班级(班主任)")
    @GetMapping("/my")
    public Result<List<BaseClass>> myClasses() {
        Long teacherId = SecurityUtils.getTeacherId();
        if (teacherId == null) {
            return Result.success(List.of());
        }
        return Result.success(classService.listByHeadTeacher(teacherId));
    }

    @Operation(summary = "班级详情")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @GetMapping("/{id}")
    public Result<BaseClass> detail(@PathVariable Long id) {
        return Result.success(classService.detail(id));
    }

    @Operation(summary = "新增班级")
    @OperLog(module = "班级管理", operation = "新增班级")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @PostMapping
    public Result<Long> create(@RequestBody BaseClass clazz) {
        return Result.success("新增成功", classService.create(clazz));
    }

    @Operation(summary = "修改班级")
    @OperLog(module = "班级管理", operation = "修改班级")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @PutMapping
    public Result<Void> update(@RequestBody BaseClass clazz) {
        classService.update(clazz);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除班级")
    @OperLog(module = "班级管理", operation = "删除班级")
    @PreAuthorize("@ss.hasPerm('class:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return Result.success("删除成功", null);
    }
}
