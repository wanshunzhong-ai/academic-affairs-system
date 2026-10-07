-- ============================================================================
-- 增量脚本：补充「教师管理」菜单
--   背景：后端 TeaTeacherController 已提供完整的教师 / 班主任 CRUD 接口，
--         但初始菜单数据未包含入口，导致管理员与教务处无法在界面上维护教师。
--   影响：新增菜单 id=17（教师管理），并授权给 管理员(4) 与 教务处(3)。
--   幂等：可重复执行。
-- ============================================================================

USE `academic_affairs`;

-- 1. 新增「教师管理」菜单，放在「学生管理」之后
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
VALUES (17, 10, '教师管理', 'C', '/academic/teacher', 'academic/teacher/index', 'teacher:list', 'Postcard', 2, 1, 1)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `menu_name` = VALUES(`menu_name`),
    `menu_type` = VALUES(`menu_type`),
    `path`      = VALUES(`path`),
    `component` = VALUES(`component`),
    `perms`     = VALUES(`perms`),
    `icon`      = VALUES(`icon`),
    `sort`      = VALUES(`sort`),
    `visible`   = VALUES(`visible`),
    `status`    = VALUES(`status`);

-- 2. 重排「学籍管理」下的菜单顺序，使教师管理紧随学生管理
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 12;  -- 班级管理
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 13;  -- 院系管理
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 14;  -- 专业管理
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 15;  -- 学期管理
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 16;  -- 教室管理

-- 3. 授予 管理员(4) / 教务处(3)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (4, 17),
    (3, 17);
