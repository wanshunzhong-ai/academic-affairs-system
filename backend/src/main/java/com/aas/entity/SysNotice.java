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
 * 通知公告
 */
@Data
@TableName("sys_notice")
public class SysNotice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 通知/公告/教务/紧急 */
    private String noticeType;

    /** ALL全校 CLASS指定班级 ROLE指定角色 */
    private String scope;

    private Long classId;

    private String targetRole;

    private Long publisherId;

    private String publisherName;

    private String publisherRole;

    /** 0草稿 1已发布 2已下架 */
    private Integer status;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String className;

    /** 当前用户是否已读 */
    @TableField(exist = false)
    private Boolean readFlag;
}
