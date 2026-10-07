/**
 * 登录入口与身份配置（单一来源）
 * ---------------------------------------------------------------------------
 * 系统**只有一个登录入口**：/login
 *
 * 登录成功后，后端按账号自身的身份（roleCode）下发菜单、按钮权限与数据范围，
 * 不同身份进入**各自的后台** —— 工作台内容、左边菜单、能看的数据都不一样。
 * 用户不需要（也无法）记住四个不同的登录地址。
 *
 * 本文件维护的「四种身份」用于三件事：
 *   1. 登录页展示「同一入口，不同身份进入不同后台」的说明；
 *   2. 演示账号（是否在登录页展示由后端配置 aas.demo-accounts 决定，默认不展示）；
 *   3. 校验脚本 docs/login_entry_crosscheck.py 与数据库 sys_role 比对。
 *
 * 新增身份时在 LOGIN_ROLES 追加一项即可。
 *
 * 注意：这里的身份只影响**前端展示**。真正的权限边界在后端 ——
 * 即使有人绕过前端直接调接口，返回的菜单与数据范围仍按账号真实身份收敛。
 */

/** 唯一登录入口 */
export const LOGIN_PATH = '/login'

/** 登录后的落地页：各身份的工作台（页面内容与左侧菜单按 roleCode 区分） */
export const HOME_PATH = '/dashboard'

export const LOGIN_ROLES = [
  {
    key: 'student',
    roleCode: 'STUDENT',
    label: '学生',
    slogan: '选课、课表、成绩、请假一站办理',
    demoUsername: '2023001',
    tagType: 'primary',
    accent: '#2563eb',
    entryIcon: 'User',
    features: [
      '在线选课退课，实时查看课程余量与上课时间',
      '我的课表、我的成绩、学分与绩点一目了然',
      '在线提交请假申请，随时查看审批进度',
      '接收通知公告，不遗漏任何教务安排'
    ]
  },
  {
    key: 'headteacher',
    roleCode: 'HEAD_TEACHER',
    label: '班主任',
    slogan: '本班学生、成绩录入、请假审批',
    demoUsername: 'T1001',
    tagType: 'success',
    accent: '#0d9488',
    entryIcon: 'Avatar',
    features: [
      '查看所带班级的学生名册与学籍信息',
      '录入、提交并修改本班各科成绩',
      '审批本班学生的请假申请',
      '查看班级成绩分布与平均分统计'
    ]
  },
  {
    key: 'academic',
    roleCode: 'ACADEMIC',
    label: '教务处',
    slogan: '排课、教务管理、成绩审核统计',
    demoUsername: 'jwc001',
    tagType: 'warning',
    accent: '#d97706',
    entryIcon: 'School',
    features: [
      '维护院系、专业、班级、教室与学期等基础数据',
      '课程管理、开课排课，自动检测时间冲突',
      '选课名单管理、成绩审核与统计分析',
      '发布通知公告，导出各类教务报表'
    ]
  },
  {
    key: 'admin',
    roleCode: 'ADMIN',
    label: '管理员',
    slogan: '用户、角色、权限、日志全把控',
    demoUsername: 'admin',
    tagType: 'danger',
    accent: '#6d28d9',
    entryIcon: 'Setting',
    features: [
      '用户、角色、菜单与权限分配全掌控',
      '配置数据权限范围（全校 / 本班 / 仅本人）',
      '查看系统操作日志，追溯关键数据变更',
      '与教务处共享全校教务数据视图'
    ]
  }
]

/** 由后端角色码（roleCode）查身份配置 */
export function findLoginRoleByCode(code) {
  if (!code) return null
  return LOGIN_ROLES.find((r) => r.roleCode === code) || null
}

/** 由后端角色码取中文名，找不到则返回空串（调用方自行兜底） */
export function roleLabelOf(code) {
  return findLoginRoleByCode(code)?.label || ''
}

/** 由后端角色码取标签配色（用于顶部身份标签） */
export function roleTagTypeOf(code) {
  return findLoginRoleByCode(code)?.tagType || 'info'
}
