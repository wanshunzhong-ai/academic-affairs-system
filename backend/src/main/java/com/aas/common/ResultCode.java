package com.aas.common;

import lombok.Getter;

/**
 * 响应状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    PARAM_ERROR(400, "请求参数校验失败"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),
    FORBIDDEN(403, "抱歉，您没有该操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被支持"),

    LOGIN_ERROR(1001, "账号或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已被停用，请联系管理员"),
    ACCOUNT_NOT_EXIST(1003, "账号不存在"),
    PASSWORD_ERROR(1004, "原密码不正确"),
    TOKEN_EXPIRED(1005, "登录已过期，请重新登录"),

    DATA_NOT_EXIST(2001, "数据不存在"),
    DATA_DUPLICATE(2002, "数据已存在，请勿重复添加"),
    DATA_REFERENCED(2003, "该数据已被引用，无法删除"),
    OPERATION_NOT_ALLOWED(2004, "当前状态不允许该操作"),

    BIZ_ERROR(3000, "业务处理异常"),
    FILE_ERROR(4001, "文件处理失败"),
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
