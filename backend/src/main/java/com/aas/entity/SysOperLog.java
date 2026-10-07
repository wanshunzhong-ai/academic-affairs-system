package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String roleName;

    /** 业务模块 */
    private String module;

    /** 操作描述 */
    private String operation;

    private String method;

    private String requestUri;

    private String requestParam;

    private String ip;

    private Long costTime;

    /** 0失败 1成功 */
    private Integer status;

    private String errorMsg;

    private LocalDateTime createTime;
}
