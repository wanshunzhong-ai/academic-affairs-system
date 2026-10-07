package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生
 */
@Data
@TableName("stu_student")
public class StuStudent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String studentNo;

    private String name;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private String phone;

    private String email;

    private Long deptId;

    private Long majorId;

    private Long classId;

    private LocalDate enrollmentDate;

    /** 政治面貌 */
    private String politicalStatus;

    private String address;

    private String guardianName;

    private String guardianPhone;

    private String dormitory;

    private String photo;

    /** 1在读 2休学 3退学 4毕业 */
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
    private String majorName;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String grade;

    /** 平均绩点 */
    @TableField(exist = false)
    private java.math.BigDecimal avgPoint;

    /** 已修学分 */
    @TableField(exist = false)
    private java.math.BigDecimal earnedCredit;
}
