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
 * 班级
 */
@Data
@TableName("base_class")
public class BaseClass implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String classCode;

    private String className;

    private Long majorId;

    /** 年级，如 2023 */
    private String grade;

    /** 班主任ID(tea_teacher.id) */
    private Long headTeacherId;

    private Integer enrollmentYear;

    private Integer studentCount;

    /** 固定教室 */
    private String classroom;

    /** 0已毕业 1在读 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String majorName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private Long deptId;

    @TableField(exist = false)
    private String headTeacherName;

    @TableField(exist = false)
    private String headTeacherPhone;
}
