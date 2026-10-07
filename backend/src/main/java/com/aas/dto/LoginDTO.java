package com.aas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录参数")
public class LoginDTO {

    @NotBlank(message = "请输入登录账号")
    @Schema(description = "登录账号", example = "admin")
    private String username;

    @NotBlank(message = "请输入密码")
    @Schema(description = "密码", example = "123456")
    private String password;
}
