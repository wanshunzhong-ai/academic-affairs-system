package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 院系
 */
@Data
@TableName("base_dept")
public class BaseDept implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptCode;

    private String deptName;

    /** 院系负责人 */
    private String dean;

    private String phone;

    private String description;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
