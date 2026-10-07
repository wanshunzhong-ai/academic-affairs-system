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
import java.util.List;

/**
 * 开课安排
 */
@Data
@TableName("course_offering")
public class CourseOffering implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String offeringCode;

    private Long courseId;

    private Long semesterId;

    private Long teacherId;

    /** 面向班级(为空表示公共选修) */
    private Long classId;

    private Integer capacity;

    private Integer selectedCount;

    /** 0否 1是公共选修 */
    private Integer isPublic;

    /** 0未发布 1已发布 2已结课 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String courseCode;

    @TableField(exist = false)
    private String courseName;

    @TableField(exist = false)
    private BigDecimal credit;

    @TableField(exist = false)
    private Integer hours;

    @TableField(exist = false)
    private String courseType;

    @TableField(exist = false)
    private String examType;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String teacherNo;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String semesterName;

    @TableField(exist = false)
    private Long deptId;

    @TableField(exist = false)
    private String deptName;

    /** 排课时间段 */
    @TableField(exist = false)
    private List<CourseSchedule> schedules;

    /** 排课时间文本，如 周一 第1-2节 */
    @TableField(exist = false)
    private String scheduleText;

    /** 是否已被当前学生选中 */
    @TableField(exist = false)
    private Boolean selected;

    /** 冲突提示 */
    @TableField(exist = false)
    private String conflictMsg;
}
