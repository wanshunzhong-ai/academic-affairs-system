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
 * 教室
 */
@Data
@TableName("base_classroom")
public class BaseClassroom implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomCode;

    private String roomName;

    private String building;

    private Integer capacity;

    /** 普通教室/多媒体/机房/实验室 */
    private String roomType;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
