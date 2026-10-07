package com.aas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录页需要的公开配置（登录前就能拿到，不涉及任何敏感信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录页配置")
public class LoginConfigVO {

    @Schema(description = "是否在登录页展示演示账号一键填充（aas.demo-accounts）")
    private Boolean demoAccounts;

    @Schema(description = "登录入口路径（系统只有一个入口，登录后按账号身份进入对应后台）")
    private String loginPath = "/login";
}
