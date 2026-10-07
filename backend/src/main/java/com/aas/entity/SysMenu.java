package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    /** M目录 C菜单 F按钮 */
    private String menuType;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    private LocalDateTime createTime;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
