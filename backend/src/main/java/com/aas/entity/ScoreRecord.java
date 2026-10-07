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
 * 成绩
 */
@Data
@TableName("score_record")
public class ScoreRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long offeringId;

    private BigDecimal usualScore;

    private BigDecimal examScore;

    private BigDecimal totalScore;

    private BigDecimal gradePoint;

    /** 0未录入 1已录入 2已发布 */
    private Integer status;

    /** 0否 1是重修 */
    private Integer isRetake;

    private Long inputBy;

    private LocalDateTime inputTime;

    private LocalDateTime publishTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

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
    private Long semesterId;

    @TableField(exist = false)
    private String inputByName;

    /** 等级: 优秀/良好/中等/及格/不及格 */
    @TableField(exist = false)
    private String level;
}
