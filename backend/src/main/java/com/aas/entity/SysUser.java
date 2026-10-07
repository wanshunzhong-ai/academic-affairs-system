package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码(BCrypt) */
    @JsonIgnore
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 用户类型: STUDENT/TEACHER/ADMIN */
    private String userType;

    private String avatar;

    /** 1男 2女 */
    private Integer gender;

    private String phone;

    private String email;

    /** 0停用 1正常 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    // ================= 非数据库字段 =================

    /** 角色ID列表 */
    @TableField(exist = false)
    private java.util.List<Long> roleIds;

    /** 角色名称列表 */
    @TableField(exist = false)
    private java.util.List<String> roleNames;

    /** 角色标识列表 */
    @TableField(exist = false)
    private java.util.List<String> roleCodes;

    /** 关联的业务档案编号(学号/工号) */
    @TableField(exist = false)
    private String bizNo;
}
