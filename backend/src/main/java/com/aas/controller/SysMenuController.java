package com.aas.controller;

import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.entity.SysMenu;
import com.aas.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@Tag(name = "04-菜单管理")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单树")
    @PreAuthorize("@ss.hasPerm('menu:list')")
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree(@RequestParam(required = false) String keyword) {
        return Result.success(menuService.tree(keyword));
    }

    @Operation(summary = "菜单平铺列表")
    @PreAuthorize("@ss.hasPerm('menu:list')")
    @GetMapping("/list")
    public Result<List<SysMenu>> list() {
        return Result.success(menuService.list());
    }

    @Operation(summary = "新增菜单")
    @OperLog(module = "菜单管理", operation = "新增菜单")
    @PreAuthorize("@ss.hasPerm('menu:list')")
    @PostMapping
    public Result<Long> create(@RequestBody SysMenu menu) {
        return Result.success("新增成功", menuService.create(menu));
    }

    @Operation(summary = "修改菜单")
    @OperLog(module = "菜单管理", operation = "修改菜单")
    @PreAuthorize("@ss.hasPerm('menu:list')")
    @PutMapping
    public Result<Void> update(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除菜单")
    @OperLog(module = "菜单管理", operation = "删除菜单")
    @PreAuthorize("@ss.hasPerm('menu:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success("删除成功", null);
    }
}
