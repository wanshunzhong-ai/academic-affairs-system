package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseMajor;
import com.aas.service.BaseMajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业管理
 */
@Tag(name = "07-专业管理")
@RestController
@RequestMapping("/api/base/major")
@RequiredArgsConstructor
public class BaseMajorController {

    private final BaseMajorService majorService;

    @Operation(summary = "分页查询专业")
    @PreAuthorize("@ss.hasPerm('major:list')")
    @GetMapping("/page")
    public Result<PageResult<BaseMajor>> page(CommonQuery query) {
        return Result.success(majorService.page(query));
    }

    @Operation(summary = "专业列表(下拉用)")
    @GetMapping("/options")
    public Result<List<BaseMajor>> options(@RequestParam(required = false) Long deptId) {
        return Result.success(majorService.listAll(deptId));
    }

    @Operation(summary = "专业详情")
    @PreAuthorize("@ss.hasPerm('major:list')")
    @GetMapping("/{id}")
    public Result<BaseMajor> detail(@PathVariable Long id) {
        return Result.success(majorService.detail(id));
    }

    @Operation(summary = "新增专业")
    @OperLog(module = "专业管理", operation = "新增专业")
    @PreAuthorize("@ss.hasPerm('major:list')")
    @PostMapping
    public Result<Long> create(@RequestBody BaseMajor major) {
        return Result.success("新增成功", majorService.create(major));
    }

    @Operation(summary = "修改专业")
    @OperLog(module = "专业管理", operation = "修改专业")
    @PreAuthorize("@ss.hasPerm('major:list')")
    @PutMapping
    public Result<Void> update(@RequestBody BaseMajor major) {
        majorService.update(major);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除专业")
    @OperLog(module = "专业管理", operation = "删除专业")
    @PreAuthorize("@ss.hasPerm('major:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        majorService.delete(id);
        return Result.success("删除成功", null);
    }
}
