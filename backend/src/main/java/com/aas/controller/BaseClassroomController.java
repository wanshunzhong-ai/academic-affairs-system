package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseClassroom;
import com.aas.service.BaseClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教室管理
 */
@Tag(name = "10-教室管理")
@RestController
@RequestMapping("/api/base/classroom")
@RequiredArgsConstructor
public class BaseClassroomController {

    private final BaseClassroomService classroomService;

    @Operation(summary = "分页查询教室")
    @PreAuthorize("@ss.hasPerm('classroom:list')")
    @GetMapping("/page")
    public Result<PageResult<BaseClassroom>> page(CommonQuery query) {
        return Result.success(classroomService.page(query));
    }

    @Operation(summary = "教室列表(下拉用)")
    @GetMapping("/options")
    public Result<List<BaseClassroom>> options() {
        return Result.success(classroomService.listAll());
    }

    @Operation(summary = "新增教室")
    @OperLog(module = "教室管理", operation = "新增教室")
    @PreAuthorize("@ss.hasPerm('classroom:list')")
    @PostMapping
    public Result<Long> create(@RequestBody BaseClassroom classroom) {
        return Result.success("新增成功", classroomService.create(classroom));
    }

    @Operation(summary = "修改教室")
    @OperLog(module = "教室管理", operation = "修改教室")
    @PreAuthorize("@ss.hasPerm('classroom:list')")
    @PutMapping
    public Result<Void> update(@RequestBody BaseClassroom classroom) {
        classroomService.update(classroom);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除教室")
    @OperLog(module = "教室管理", operation = "删除教室")
    @PreAuthorize("@ss.hasPerm('classroom:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classroomService.delete(id);
        return Result.success("删除成功", null);
    }
}
