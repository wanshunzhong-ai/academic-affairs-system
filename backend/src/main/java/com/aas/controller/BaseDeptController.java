package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseDept;
import com.aas.service.BaseDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 院系管理
 */
@Tag(name = "06-院系管理")
@RestController
@RequestMapping("/api/base/dept")
@RequiredArgsConstructor
public class BaseDeptController {

    private final BaseDeptService deptService;

    @Operation(summary = "分页查询院系")
    @PreAuthorize("@ss.hasPerm('dept:list')")
    @GetMapping("/page")
    public Result<PageResult<BaseDept>> page(CommonQuery query) {
        return Result.success(deptService.page(query));
    }

    @Operation(summary = "全部院系(下拉用)")
    @GetMapping("/options")
    public Result<List<BaseDept>> options() {
        return Result.success(deptService.listAll());
    }

    @Operation(summary = "院系详情")
    @PreAuthorize("@ss.hasPerm('dept:list')")
    @GetMapping("/{id}")
    public Result<BaseDept> detail(@PathVariable Long id) {
        return Result.success(deptService.detail(id));
    }

    @Operation(summary = "新增院系")
    @OperLog(module = "院系管理", operation = "新增院系")
    @PreAuthorize("@ss.hasPerm('dept:list')")
    @PostMapping
    public Result<Long> create(@RequestBody BaseDept dept) {
        return Result.success("新增成功", deptService.create(dept));
    }

    @Operation(summary = "修改院系")
    @OperLog(module = "院系管理", operation = "修改院系")
    @PreAuthorize("@ss.hasPerm('dept:list')")
    @PutMapping
    public Result<Void> update(@RequestBody BaseDept dept) {
        deptService.update(dept);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除院系")
    @OperLog(module = "院系管理", operation = "删除院系")
    @PreAuthorize("@ss.hasPerm('dept:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success("删除成功", null);
    }
}
