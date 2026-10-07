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
 * 专业
 */
@Data
@TableName("base_major")
public class BaseMajor implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String majorCode;

    private String majorName;

    private Long deptId;

    /** 授予学位 */
    private String degree;

    /** 学制(年) */
    private Integer duration;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private Integer classCount;
}
