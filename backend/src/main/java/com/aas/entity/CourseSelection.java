package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生选课
 */
@Data
@TableName("course_selection")
public class CourseSelection implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long offeringId;

    /** 1学生自选 2系统分配 */
    private Integer selectType;

    private LocalDateTime selectTime;

    /** 0已退选 1已选 */
    private Integer status;

    private String remark;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String courseCode;

    @TableField(exist = false)
    private String courseName;

    @TableField(exist = false)
    private BigDecimal credit;

    @TableField(exist = false)
    private String courseType;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String semesterName;

    @TableField(exist = false)
    private String scheduleText;
}
