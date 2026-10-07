package com.aas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告阅读记录
 */
@Data
@TableName("sys_notice_read")
public class SysNoticeRead implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long userId;

    private LocalDateTime readTime;
}
