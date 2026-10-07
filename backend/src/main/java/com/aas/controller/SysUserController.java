package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.UserQuery;
import com.aas.entity.SysUser;
import com.aas.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理
 */
@Tag(name = "02-用户管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "分页查询用户")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(UserQuery query) {
        return Result.success(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @Operation(summary = "新增用户")
    @OperLog(module = "用户管理", operation = "新增用户")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @PostMapping
    public Result<Long> create(@RequestBody SysUser user) {
        return Result.success("新增成功", userService.create(user, user.getRoleIds()));
    }

    @Operation(summary = "修改用户")
    @OperLog(module = "用户管理", operation = "修改用户")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        userService.update(user, user.getRoleIds());
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除用户")
    @OperLog(module = "用户管理", operation = "删除用户")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除用户")
    @OperLog(module = "用户管理", operation = "批量删除用户")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        userService.deleteBatch(ids);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "重置密码")
    @OperLog(module = "用户管理", operation = "重置密码")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @PutMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id,
                                                     @RequestParam(required = false) String password) {
        String pwd = userService.resetPassword(id, password);
        return Result.success("密码重置成功", Map.of("password", pwd));
    }

    @Operation(summary = "启用/停用账号")
    @OperLog(module = "用户管理", operation = "修改账号状态")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @NotNull @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "分配角色")
    @OperLog(module = "用户管理", operation = "分配角色")
    @PreAuthorize("@ss.hasPerm('user:list')")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success("分配成功", null);
    }
}
