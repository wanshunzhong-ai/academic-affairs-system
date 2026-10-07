package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录
 */
@Data
@TableName("att_attendance")
public class AttAttendance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long offeringId;

    private LocalDate attendDate;

    /** 1出勤 2迟到 3早退 4缺勤 5请假 */
    private Integer attendType;

    private String remark;

    private Long recorderId;

    private LocalDateTime createTime;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String courseName;

    @TableField(exist = false)
    private String attendTypeText;

    @TableField(exist = false)
    private String recorderName;
}
