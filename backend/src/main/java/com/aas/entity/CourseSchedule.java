package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程排课时间
 */
@Data
@TableName("course_schedule")
public class CourseSchedule implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long offeringId;

    /** 星期几 1-7 */
    private Integer weekDay;

    private Integer startSection;

    private Integer endSection;

    private Integer startWeek;

    private Integer endWeek;

    private Long classroomId;

    private LocalDateTime createTime;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String classroomName;

    @TableField(exist = false)
    private String building;

    /** 星期中文 */
    @TableField(exist = false)
    private String weekDayText;
}
