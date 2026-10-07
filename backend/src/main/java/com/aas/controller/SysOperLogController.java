package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.PageQuery;
import com.aas.entity.SysOperLog;
import com.aas.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志
 */
@Tag(name = "05-操作日志")
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService operLogService;

    @Operation(summary = "分页查询操作日志")
    @PreAuthorize("@ss.hasPerm('log:list')")
    @GetMapping("/page")
    public Result<PageResult<SysOperLog>> page(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String module,
                                               @RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               PageQuery pageQuery) {
        return Result.success(operLogService.page(keyword, status, module, startDate, endDate, pageQuery));
    }

    @Operation(summary = "日志模块列表")
    @PreAuthorize("@ss.hasPerm('log:list')")
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        return Result.success(operLogService.modules());
    }

    @Operation(summary = "删除日志")
    @OperLog(module = "操作日志", operation = "删除日志")
    @PreAuthorize("@ss.hasPerm('log:list')")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        operLogService.deleteBatch(ids);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "清空日志")
    @OperLog(module = "操作日志", operation = "清空日志")
    @PreAuthorize("@ss.hasPerm('log:list')")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        operLogService.clear();
        return Result.success("已清空", null);
    }

    @Operation(summary = "清理N天前的日志")
    @OperLog(module = "操作日志", operation = "清理历史日志")
    @PreAuthorize("@ss.hasPerm('log:list')")
    @DeleteMapping("/clean")
    public Result<Integer> clean(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success("清理完成", operLogService.cleanBefore(days));
    }
}
