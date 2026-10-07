package com.aas.controller;

import com.aas.common.Result;
import com.aas.dto.ChangePasswordDTO;
import com.aas.dto.LoginDTO;
import com.aas.entity.SysMenu;
import com.aas.security.SecurityUtils;
import com.aas.service.AuthService;
import com.aas.util.IpUtils;
import com.aas.vo.LoginConfigVO;
import com.aas.vo.LoginVO;
import com.aas.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口
 */
@Tag(name = "01-认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 是否在登录页展示演示账号一键填充（演示账号本身只写在部署文档里） */
    @Value("${aas.demo-accounts:false}")
    private Boolean demoAccounts;

    @Operation(summary = "登录页配置(是否展示演示账号)")
    @GetMapping("/login-config")
    public Result<LoginConfigVO> loginConfig() {
        return Result.success(new LoginConfigVO(Boolean.TRUE.equals(demoAccounts), "/login"));
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success("登录成功", authService.login(dto, IpUtils.getIp(request)));
    }

    @Operation(summary = "获取当前登录用户信息(含菜单与权限)")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(authService.getInfo(SecurityUtils.getLoginUser()));
    }

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/menus")
    public Result<List<SysMenu>> menus() {
        return Result.success(authService.getMenuTree(SecurityUtils.getLoginUser()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @Operation(summary = "修改当前用户密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(SecurityUtils.getLoginUser(), dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "服务心跳检测")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }
}
