package com.aas.controller;

import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.entity.SysRole;
import com.aas.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 */
@Tag(name = "03-角色管理")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色列表")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @GetMapping("/list")
    public Result<List<SysRole>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(roleService.list(keyword, status));
    }

    @Operation(summary = "全部启用角色(下拉用)")
    @GetMapping("/options")
    public Result<List<SysRole>> options() {
        return Result.success(roleService.list(null, 1));
    }

    @Operation(summary = "角色详情")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @GetMapping("/{id}")
    public Result<SysRole> detail(@PathVariable Long id) {
        return Result.success(roleService.detail(id));
    }

    @Operation(summary = "角色已分配的菜单ID")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> menuIds(@PathVariable Long id) {
        return Result.success(roleService.menuIds(id));
    }

    @Operation(summary = "新增角色")
    @OperLog(module = "角色管理", operation = "新增角色")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @PostMapping
    public Result<Long> create(@RequestBody SysRole role) {
        return Result.success("新增成功", roleService.create(role, role.getMenuIds()));
    }

    @Operation(summary = "修改角色")
    @OperLog(module = "角色管理", operation = "修改角色")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.update(role, role.getMenuIds());
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除角色")
    @OperLog(module = "角色管理", operation = "删除角色")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "分配菜单权限")
    @OperLog(module = "角色管理", operation = "分配菜单权限")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success("分配成功", null);
    }

    @Operation(summary = "启用/停用角色")
    @OperLog(module = "角色管理", operation = "修改角色状态")
    @PreAuthorize("@ss.hasPerm('role:list')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        roleService.changeStatus(id, status);
        return Result.success("操作成功", null);
    }
}
