package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程
 */
@Data
@TableName("course_course")
public class CourseCourse implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String courseCode;

    private String courseName;

    private Long deptId;

    private BigDecimal credit;

    /** 总学时 */
    private Integer hours;

    /** 必修/选修/公共 */
    private String courseType;

    /** 考试/考查 */
    private String examType;

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
    private Integer offeringCount;
}
