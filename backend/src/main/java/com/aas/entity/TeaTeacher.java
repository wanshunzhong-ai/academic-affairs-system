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
import java.util.List;

/**
 * 教师
 */
@Data
@TableName("tea_teacher")
public class TeaTeacher implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    private String teacherNo;

    private String name;

    private Integer gender;

    private LocalDate birthDate;

    private String phone;

    private String email;

    private String idCard;

    private Long deptId;

    /** 助教/讲师/副教授/教授 */
    private String title;

    /** 本科/硕士/博士 */
    private String education;

    private LocalDate hireDate;

    /** 0否 1是 */
    private Integer isHeadTeacher;

    /** 0离职 1在职 */
    private Integer status;

    private String photo;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String deptName;

    /** 所带班级名称(班主任) */
    @TableField(exist = false)
    private String headClassNames;

    @TableField(exist = false)
    private List<Long> headClassIds;
}
