package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色标识: STUDENT/HEAD_TEACHER/ACADEMIC/ADMIN */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 数据范围: SELF/CLASS/ALL */
    private String dataScope;

    private Integer sort;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    // ================= 非数据库字段 =================

    /** 该角色拥有的菜单ID */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<Long> menuIds;
}
