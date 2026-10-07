package com.aas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";

    @Schema(description = "有效期(秒)")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}
