-- ============================================================================
--  教务管理系统 - 数据库结构脚本 (MySQL 8.0+ / 9.x)
--  数据库: academic_affairs   字符集: utf8mb4
--  说明: 本脚本为幂等脚本, 重复执行会重建所有表(数据会丢失), 请谨慎使用
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `academic_affairs`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `academic_affairs`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
--  一、系统权限模块 (RBAC)
-- ============================================================================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    `password`        VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt)',
    `real_name`       VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
    `user_type`       VARCHAR(20)  NOT NULL                COMMENT '用户类型: STUDENT/TEACHER/ADMIN',
    `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像地址',
    `gender`          TINYINT      DEFAULT 1               COMMENT '性别: 1男 2女',
    `phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `email`           VARCHAR(64)  DEFAULT NULL            COMMENT '邮箱',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1正常',
    `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(64)  DEFAULT NULL            COMMENT '最后登录IP',
    `remark`          VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_user_type` (`user_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_code`   VARCHAR(50)  NOT NULL                COMMENT '角色标识: STUDENT/HEAD_TEACHER/ACADEMIC/ADMIN',
    `role_name`   VARCHAR(50)  NOT NULL                COMMENT '角色名称',
    `data_scope`  VARCHAR(20)  NOT NULL DEFAULT 'SELF' COMMENT '数据范围: SELF本人 CLASS本班 ALL全部',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1正常',
    `description` VARCHAR(255) DEFAULT NULL            COMMENT '角色描述',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父菜单ID, 0为顶级',
    `menu_name`   VARCHAR(50)  NOT NULL                COMMENT '菜单名称',
    `menu_type`   CHAR(1)      NOT NULL                COMMENT '类型: M目录 C菜单 F按钮',
    `path`        VARCHAR(200) DEFAULT NULL            COMMENT '前端路由地址',
    `component`   VARCHAR(200) DEFAULT NULL            COMMENT '前端组件路径',
    `perms`       VARCHAR(100) DEFAULT NULL            COMMENT '权限标识, 如 student:list',
    `icon`        VARCHAR(100) DEFAULT NULL            COMMENT '菜单图标',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `visible`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否显示: 0隐藏 1显示',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统菜单权限表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联表';

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `username`    VARCHAR(50)  DEFAULT NULL COMMENT '操作人账号',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    `role_name`   VARCHAR(50)  DEFAULT NULL COMMENT '操作人角色',
    `module`      VARCHAR(50)  DEFAULT NULL COMMENT '业务模块',
    `operation`   VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
    `method`      VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
    `request_uri` VARCHAR(255) DEFAULT NULL COMMENT '请求地址',
    `request_param` TEXT        DEFAULT NULL COMMENT '请求参数',
    `ip`          VARCHAR(64)  DEFAULT NULL COMMENT '操作IP',
    `cost_time`   BIGINT       DEFAULT 0    COMMENT '耗时(毫秒)',
    `status`      TINYINT      DEFAULT 1    COMMENT '状态: 0失败 1成功',
    `error_msg`   TEXT         DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_user` (`user_id`),
    KEY `idx_log_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- ============================================================================
--  二、基础数据模块
-- ============================================================================

DROP TABLE IF EXISTS `base_dept`;
CREATE TABLE `base_dept` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dept_code`   VARCHAR(30)  NOT NULL                COMMENT '院系编码',
    `dept_name`   VARCHAR(80)  NOT NULL                COMMENT '院系名称',
    `dean`        VARCHAR(50)  DEFAULT NULL            COMMENT '院系负责人',
    `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '联系电话',
    `description` VARCHAR(255) DEFAULT NULL            COMMENT '院系简介',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '院系表';

DROP TABLE IF EXISTS `base_major`;
CREATE TABLE `base_major` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `major_code`  VARCHAR(30)  NOT NULL                COMMENT '专业编码',
    `major_name`  VARCHAR(80)  NOT NULL                COMMENT '专业名称',
    `dept_id`     BIGINT       NOT NULL                COMMENT '所属院系ID',
    `degree`      VARCHAR(20)  DEFAULT '工学学士'      COMMENT '授予学位',
    `duration`    INT          NOT NULL DEFAULT 4      COMMENT '学制(年)',
    `description` VARCHAR(255) DEFAULT NULL            COMMENT '专业简介',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_code` (`major_code`),
    KEY `idx_major_dept` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '专业表';

DROP TABLE IF EXISTS `base_class`;
CREATE TABLE `base_class` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `class_code`      VARCHAR(30) NOT NULL                COMMENT '班级编码',
    `class_name`      VARCHAR(80) NOT NULL                COMMENT '班级名称',
    `major_id`        BIGINT      NOT NULL                COMMENT '所属专业ID',
    `grade`           VARCHAR(10) NOT NULL                COMMENT '年级, 如 2023',
    `head_teacher_id` BIGINT      DEFAULT NULL            COMMENT '班主任ID(tea_teacher.id)',
    `enrollment_year` INT         DEFAULT NULL            COMMENT '入学年份',
    `student_count`   INT         NOT NULL DEFAULT 0      COMMENT '班级人数(冗余统计)',
    `classroom`       VARCHAR(50) DEFAULT NULL            COMMENT '固定教室',
    `status`          TINYINT     NOT NULL DEFAULT 1      COMMENT '状态: 0已毕业 1在读',
    `remark`          VARCHAR(255) DEFAULT NULL           COMMENT '备注',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_code` (`class_code`),
    KEY `idx_class_major` (`major_id`),
    KEY `idx_class_head` (`head_teacher_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级表';

DROP TABLE IF EXISTS `base_semester`;
CREATE TABLE `base_semester` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `semester_name` VARCHAR(50) NOT NULL                COMMENT '学期名称, 如 2024-2025学年第一学期',
    `school_year`   VARCHAR(20) DEFAULT NULL            COMMENT '学年, 如 2024-2025',
    `term`          TINYINT     DEFAULT NULL            COMMENT '学期: 1第一学期 2第二学期',
    `start_date`    DATE        DEFAULT NULL            COMMENT '开学日期',
    `end_date`      DATE        DEFAULT NULL            COMMENT '结束日期',
    `select_start`  DATETIME    DEFAULT NULL            COMMENT '选课开始时间',
    `select_end`    DATETIME    DEFAULT NULL            COMMENT '选课结束时间',
    `is_current`    TINYINT     NOT NULL DEFAULT 0      COMMENT '是否当前学期: 0否 1是',
    `status`        TINYINT     NOT NULL DEFAULT 1      COMMENT '状态: 0未启用 1进行中 2已结束',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学期表';

DROP TABLE IF EXISTS `base_classroom`;
CREATE TABLE `base_classroom` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_code`   VARCHAR(30) NOT NULL                COMMENT '教室编号',
    `room_name`   VARCHAR(50) NOT NULL                COMMENT '教室名称',
    `building`    VARCHAR(50) DEFAULT NULL            COMMENT '所在楼栋',
    `capacity`    INT         NOT NULL DEFAULT 60     COMMENT '容纳人数',
    `room_type`   VARCHAR(20) DEFAULT '普通教室'      COMMENT '教室类型: 普通教室/多媒体/机房/实验室',
    `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1可用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_code` (`room_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教室表';

-- ============================================================================
--  三、学生 / 教师
-- ============================================================================

DROP TABLE IF EXISTS `tea_teacher`;
CREATE TABLE `tea_teacher` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT      DEFAULT NULL            COMMENT '关联用户ID',
    `teacher_no`       VARCHAR(30) NOT NULL                COMMENT '工号',
    `name`             VARCHAR(50) NOT NULL                COMMENT '姓名',
    `gender`           TINYINT     DEFAULT 1               COMMENT '性别: 1男 2女',
    `birth_date`       DATE        DEFAULT NULL            COMMENT '出生日期',
    `phone`            VARCHAR(20) DEFAULT NULL            COMMENT '手机号',
    `email`            VARCHAR(64) DEFAULT NULL            COMMENT '邮箱',
    `id_card`          VARCHAR(20) DEFAULT NULL            COMMENT '身份证号',
    `dept_id`          BIGINT      DEFAULT NULL            COMMENT '所属院系ID',
    `title`            VARCHAR(30) DEFAULT NULL            COMMENT '职称: 助教/讲师/副教授/教授',
    `education`        VARCHAR(20) DEFAULT NULL            COMMENT '学历: 本科/硕士/博士',
    `hire_date`        DATE        DEFAULT NULL            COMMENT '入职日期',
    `is_head_teacher`  TINYINT     NOT NULL DEFAULT 0      COMMENT '是否班主任: 0否 1是',
    `status`           TINYINT     NOT NULL DEFAULT 1      COMMENT '状态: 0离职 1在职',
    `photo`            VARCHAR(255) DEFAULT NULL           COMMENT '照片',
    `remark`           VARCHAR(255) DEFAULT NULL           COMMENT '备注',
    `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_no` (`teacher_no`),
    KEY `idx_teacher_dept` (`dept_id`),
    KEY `idx_teacher_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师表';

DROP TABLE IF EXISTS `stu_student`;
CREATE TABLE `stu_student` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT      DEFAULT NULL            COMMENT '关联用户ID',
    `student_no`      VARCHAR(30) NOT NULL                COMMENT '学号',
    `name`            VARCHAR(50) NOT NULL                COMMENT '姓名',
    `gender`          TINYINT     DEFAULT 1               COMMENT '性别: 1男 2女',
    `birth_date`      DATE        DEFAULT NULL            COMMENT '出生日期',
    `id_card`         VARCHAR(20) DEFAULT NULL            COMMENT '身份证号',
    `phone`           VARCHAR(20) DEFAULT NULL            COMMENT '手机号',
    `email`           VARCHAR(64) DEFAULT NULL            COMMENT '邮箱',
    `dept_id`         BIGINT      DEFAULT NULL            COMMENT '所属院系ID',
    `major_id`        BIGINT      DEFAULT NULL            COMMENT '所属专业ID',
    `class_id`        BIGINT      DEFAULT NULL            COMMENT '所属班级ID',
    `enrollment_date` DATE        DEFAULT NULL            COMMENT '入学日期',
    `political_status` VARCHAR(20) DEFAULT '群众'         COMMENT '政治面貌',
    `address`         VARCHAR(255) DEFAULT NULL           COMMENT '家庭住址',
    `guardian_name`   VARCHAR(50) DEFAULT NULL            COMMENT '监护人姓名',
    `guardian_phone`  VARCHAR(20) DEFAULT NULL            COMMENT '监护人电话',
    `dormitory`       VARCHAR(50) DEFAULT NULL            COMMENT '宿舍号',
    `photo`           VARCHAR(255) DEFAULT NULL           COMMENT '照片',
    `status`          TINYINT     NOT NULL DEFAULT 1      COMMENT '学籍状态: 1在读 2休学 3退学 4毕业',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_stu_class` (`class_id`),
    KEY `idx_stu_major` (`major_id`),
    KEY `idx_stu_dept` (`dept_id`),
    KEY `idx_stu_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生表';

-- ============================================================================
--  四、教学模块: 课程 / 开课 / 排课 / 选课 / 成绩
-- ============================================================================

DROP TABLE IF EXISTS `course_course`;
CREATE TABLE `course_course` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_code` VARCHAR(30)  NOT NULL                COMMENT '课程编号',
    `course_name` VARCHAR(100) NOT NULL                COMMENT '课程名称',
    `dept_id`     BIGINT       DEFAULT NULL            COMMENT '开课院系ID',
    `credit`      DECIMAL(3,1) NOT NULL DEFAULT 3.0    COMMENT '学分',
    `hours`       INT          NOT NULL DEFAULT 48     COMMENT '总学时',
    `course_type` VARCHAR(20)  NOT NULL DEFAULT '必修' COMMENT '课程性质: 必修/选修/公共',
    `exam_type`   VARCHAR(20)  DEFAULT '考试'          COMMENT '考核方式: 考试/考查',
    `description` VARCHAR(500) DEFAULT NULL            COMMENT '课程简介',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0停用 1正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code` (`course_code`),
    KEY `idx_course_dept` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程表';

DROP TABLE IF EXISTS `course_offering`;
CREATE TABLE `course_offering` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `offering_code`  VARCHAR(40)  NOT NULL                COMMENT '开课班编号',
    `course_id`      BIGINT       NOT NULL                COMMENT '课程ID',
    `semester_id`    BIGINT       NOT NULL                COMMENT '学期ID',
    `teacher_id`     BIGINT       DEFAULT NULL            COMMENT '授课教师ID',
    `class_id`       BIGINT       DEFAULT NULL            COMMENT '面向班级ID(为空表示公共选修面向全校)',
    `capacity`       INT          NOT NULL DEFAULT 60     COMMENT '容量',
    `selected_count` INT          NOT NULL DEFAULT 0      COMMENT '已选人数',
    `is_public`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否公共选修课: 0否 1是',
    `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0未发布 1已发布(可选) 2已结课',
    `remark`         VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_offering_code` (`offering_code`),
    KEY `idx_off_course` (`course_id`),
    KEY `idx_off_semester` (`semester_id`),
    KEY `idx_off_teacher` (`teacher_id`),
    KEY `idx_off_class` (`class_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开课安排表';

DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
    `id`           BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `offering_id`  BIGINT     NOT NULL                COMMENT '开课安排ID',
    `week_day`     TINYINT    NOT NULL                COMMENT '星期几: 1-7',
    `start_section` TINYINT   NOT NULL                COMMENT '开始节次: 1-12',
    `end_section`  TINYINT    NOT NULL                COMMENT '结束节次',
    `start_week`   TINYINT    NOT NULL DEFAULT 1      COMMENT '起始周',
    `end_week`     TINYINT    NOT NULL DEFAULT 16     COMMENT '结束周',
    `classroom_id` BIGINT     DEFAULT NULL            COMMENT '教室ID',
    `create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sch_offering` (`offering_id`),
    KEY `idx_sch_room` (`classroom_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程排课时间表';

DROP TABLE IF EXISTS `course_selection`;
CREATE TABLE `course_selection` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`  BIGINT      NOT NULL                COMMENT '学生ID',
    `offering_id` BIGINT      NOT NULL                COMMENT '开课安排ID',
    `select_type` TINYINT     NOT NULL DEFAULT 1      COMMENT '选课方式: 1学生自选 2系统分配',
    `select_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态: 0已退选 1已选',
    `remark`      VARCHAR(255) DEFAULT NULL           COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stu_offering` (`student_id`, `offering_id`),
    KEY `idx_sel_offering` (`offering_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生选课表';

DROP TABLE IF EXISTS `score_record`;
CREATE TABLE `score_record` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`   BIGINT       NOT NULL                COMMENT '学生ID',
    `offering_id`  BIGINT       NOT NULL                COMMENT '开课安排ID',
    `usual_score`  DECIMAL(5,1) DEFAULT NULL            COMMENT '平时成绩',
    `exam_score`   DECIMAL(5,1) DEFAULT NULL            COMMENT '期末成绩',
    `total_score`  DECIMAL(5,1) DEFAULT NULL            COMMENT '总评成绩',
    `grade_point`  DECIMAL(3,2) DEFAULT NULL            COMMENT '绩点',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0未录入 1已录入 2已发布',
    `is_retake`    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否重修: 0否 1是',
    `input_by`     BIGINT       DEFAULT NULL            COMMENT '录入人ID',
    `input_time`   DATETIME     DEFAULT NULL            COMMENT '录入时间',
    `publish_time` DATETIME     DEFAULT NULL            COMMENT '发布时间',
    `remark`       VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stu_offering_score` (`student_id`, `offering_id`),
    KEY `idx_score_offering` (`offering_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '成绩表';

-- ============================================================================
--  五、考勤 / 请假 / 公告
-- ============================================================================

DROP TABLE IF EXISTS `att_attendance`;
CREATE TABLE `att_attendance` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`  BIGINT       NOT NULL                COMMENT '学生ID',
    `offering_id` BIGINT       DEFAULT NULL            COMMENT '开课安排ID',
    `attend_date` DATE         NOT NULL                COMMENT '考勤日期',
    `attend_type` TINYINT      NOT NULL DEFAULT 1      COMMENT '类型: 1出勤 2迟到 3早退 4缺勤 5请假',
    `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    `recorder_id` BIGINT       DEFAULT NULL            COMMENT '记录人ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_att_student` (`student_id`),
    KEY `idx_att_date` (`attend_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考勤记录表';

DROP TABLE IF EXISTS `att_leave`;
CREATE TABLE `att_leave` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`     BIGINT       NOT NULL                COMMENT '学生ID',
    `leave_type`     VARCHAR(20)  NOT NULL DEFAULT '事假' COMMENT '请假类型: 病假/事假/公假',
    `start_date`     DATETIME     NOT NULL                COMMENT '开始时间',
    `end_date`       DATETIME     NOT NULL                COMMENT '结束时间',
    `days`           DECIMAL(4,1) NOT NULL DEFAULT 1.0    COMMENT '请假天数',
    `reason`         VARCHAR(500) NOT NULL                COMMENT '请假事由',
    `status`         TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0待审批 1已通过 2已驳回 3已撤销',
    `approver_id`    BIGINT       DEFAULT NULL            COMMENT '审批人ID',
    `approver_name`  VARCHAR(50)  DEFAULT NULL            COMMENT '审批人姓名',
    `approve_time`   DATETIME     DEFAULT NULL            COMMENT '审批时间',
    `approve_remark` VARCHAR(255) DEFAULT NULL            COMMENT '审批意见',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (`id`),
    KEY `idx_leave_student` (`student_id`),
    KEY `idx_leave_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生请假表';

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`         VARCHAR(200) NOT NULL                COMMENT '标题',
    `content`       TEXT         NOT NULL                COMMENT '正文内容',
    `notice_type`   VARCHAR(20)  NOT NULL DEFAULT '通知' COMMENT '类型: 通知/公告/教务/紧急',
    `scope`         VARCHAR(20)  NOT NULL DEFAULT 'ALL'  COMMENT '范围: ALL全校 CLASS指定班级 ROLE指定角色',
    `class_id`      BIGINT       DEFAULT NULL            COMMENT '目标班级ID(scope=CLASS时)',
    `target_role`   VARCHAR(50)  DEFAULT NULL            COMMENT '目标角色(scope=ROLE时)',
    `publisher_id`  BIGINT       DEFAULT NULL            COMMENT '发布人ID',
    `publisher_name` VARCHAR(50) DEFAULT NULL            COMMENT '发布人姓名',
    `publisher_role` VARCHAR(50) DEFAULT NULL            COMMENT '发布人角色',
    `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0草稿 1已发布 2已下架',
    `publish_time`  DATETIME     DEFAULT NULL            COMMENT '发布时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_notice_scope` (`scope`),
    KEY `idx_notice_time` (`publish_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告表';

DROP TABLE IF EXISTS `sys_notice_read`;
CREATE TABLE `sys_notice_read` (
    `id`        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `notice_id` BIGINT   NOT NULL COMMENT '公告ID',
    `user_id`   BIGINT   NOT NULL COMMENT '用户ID',
    `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告阅读记录表';

SET FOREIGN_KEY_CHECKS = 1;
