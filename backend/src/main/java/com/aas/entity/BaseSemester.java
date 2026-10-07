package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期
 */
@Data
@TableName("base_semester")
public class BaseSemester implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String semesterName;

    private String schoolYear;

    /** 1第一学期 2第二学期 */
    private Integer term;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime selectStart;

    private LocalDateTime selectEnd;

    /** 0否 1是 */
    private Integer isCurrent;

    /** 0未启用 1进行中 2已结束 */
    private Integer status;

    private LocalDateTime createTime;
}
