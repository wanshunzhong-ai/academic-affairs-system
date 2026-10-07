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
 * 学生请假
 */
@Data
@TableName("att_leave")
public class AttLeave implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** 病假/事假/公假 */
    private String leaveType;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private BigDecimal days;

    private String reason;

    /** 0待审批 1已通过 2已驳回 3已撤销 */
    private Integer status;

    private Long approverId;

    private String approverName;

    private LocalDateTime approveTime;

    private String approveRemark;

    private LocalDateTime createTime;

    // ================= 非数据库字段 =================

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private Long classId;

    @TableField(exist = false)
    private String statusText;
}
