package com.aas.common.exception;

import com.aas.common.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /** 条件成立时抛出异常 */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BizException(message);
        }
    }

    public static void throwIfNull(Object obj, String message) {
        if (obj == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST.getCode(), message);
        }
    }

    public static void throwIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BizException(resultCode);
        }
    }
}
