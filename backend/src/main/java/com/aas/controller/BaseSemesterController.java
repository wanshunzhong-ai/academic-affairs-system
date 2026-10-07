package com.aas.controller;

import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.entity.BaseSemester;
import com.aas.service.BaseSemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学期管理
 */
@Tag(name = "09-学期管理")
@RestController
@RequestMapping("/api/base/semester")
@RequiredArgsConstructor
public class BaseSemesterController {

    private final BaseSemesterService semesterService;

    @Operation(summary = "学期列表")
    @GetMapping("/list")
    public Result<List<BaseSemester>> list() {
        return Result.success(semesterService.listAll());
    }

    @Operation(summary = "当前学期")
    @GetMapping("/current")
    public Result<BaseSemester> current() {
        return Result.success(semesterService.current());
    }

    @Operation(summary = "学期详情")
    @GetMapping("/{id}")
    public Result<BaseSemester> detail(@PathVariable Long id) {
        return Result.success(semesterService.detail(id));
    }

    @Operation(summary = "新增学期")
    @OperLog(module = "学期管理", operation = "新增学期")
    @PreAuthorize("@ss.hasPerm('semester:list')")
    @PostMapping
    public Result<Long> create(@RequestBody BaseSemester semester) {
        return Result.success("新增成功", semesterService.create(semester));
    }

    @Operation(summary = "修改学期")
    @OperLog(module = "学期管理", operation = "修改学期")
    @PreAuthorize("@ss.hasPerm('semester:list')")
    @PutMapping
    public Result<Void> update(@RequestBody BaseSemester semester) {
        semesterService.update(semester);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "设为当前学期")
    @OperLog(module = "学期管理", operation = "设为当前学期")
    @PreAuthorize("@ss.hasPerm('semester:list')")
    @PutMapping("/{id}/current")
    public Result<Void> setCurrent(@PathVariable Long id) {
        semesterService.setCurrent(id);
        return Result.success("已设为当前学期", null);
    }

    @Operation(summary = "删除学期")
    @OperLog(module = "学期管理", operation = "删除学期")
    @PreAuthorize("@ss.hasPerm('semester:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        semesterService.delete(id);
        return Result.success("删除成功", null);
    }
}
