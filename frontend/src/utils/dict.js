/** 全局字典：下拉选项与常量 */

export const GENDER_OPTIONS = [
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

export const STATUS_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '停用', value: 0 }
]

export const STUDENT_STATUS_OPTIONS = [
  { label: '在读', value: 1 },
  { label: '休学', value: 2 },
  { label: '退学', value: 3 },
  { label: '毕业', value: 4 }
]

export const USER_TYPE_OPTIONS = [
  { label: '学生', value: 'STUDENT' },
  { label: '教师', value: 'TEACHER' },
  { label: '管理员', value: 'ADMIN' }
]

export const USER_TYPE_TEXT = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }

export const DATA_SCOPE_OPTIONS = [
  { label: '仅本人数据', value: 'SELF' },
  { label: '本班数据', value: 'CLASS' },
  { label: '全部数据', value: 'ALL' }
]

export const DATA_SCOPE_TEXT = { SELF: '仅本人', CLASS: '本班', ALL: '全部' }

export const COURSE_TYPE_OPTIONS = [
  { label: '必修', value: '必修' },
  { label: '选修', value: '选修' },
  { label: '公共', value: '公共' }
]

export const EXAM_TYPE_OPTIONS = [
  { label: '考试', value: '考试' },
  { label: '考查', value: '考查' }
]

export const OFFERING_STATUS_OPTIONS = [
  { label: '未发布', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已结课', value: 2 }
]

export const TITLE_OPTIONS = ['助教', '讲师', '副教授', '教授'].map((v) => ({ label: v, value: v }))

export const EDUCATION_OPTIONS = ['本科', '硕士', '博士'].map((v) => ({ label: v, value: v }))

export const POLITICAL_OPTIONS = ['群众', '共青团员', '中共党员', '中共预备党员', '民主党派'].map((v) => ({
  label: v,
  value: v
}))

export const LEAVE_TYPE_OPTIONS = ['事假', '病假', '公假'].map((v) => ({ label: v, value: v }))

export const LEAVE_STATUS_OPTIONS = [
  { label: '待审批', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已驳回', value: 2 },
  { label: '已撤销', value: 3 }
]

export const ATTEND_TYPE_OPTIONS = [
  { label: '出勤', value: 1 },
  { label: '迟到', value: 2 },
  { label: '早退', value: 3 },
  { label: '缺勤', value: 4 },
  { label: '请假', value: 5 }
]

export const SCORE_STATUS_OPTIONS = [
  { label: '未录入', value: 0 },
  { label: '已录入', value: 1 },
  { label: '已发布', value: 2 }
]

export const NOTICE_TYPE_OPTIONS = ['通知', '公告', '教务', '紧急'].map((v) => ({ label: v, value: v }))

export const NOTICE_SCOPE_OPTIONS = [
  { label: '全校', value: 'ALL' },
  { label: '指定班级', value: 'CLASS' },
  { label: '指定角色', value: 'ROLE' }
]

export const NOTICE_SCOPE_TEXT = { ALL: '全校', CLASS: '指定班级', ROLE: '指定角色' }

export const ROLE_OPTIONS = [
  { label: '学生', value: 'STUDENT' },
  { label: '班主任', value: 'HEAD_TEACHER' },
  { label: '教务处', value: 'ACADEMIC' },
  { label: '系统管理员', value: 'ADMIN' }
]

export const SELECT_TYPE_TEXT = { 1: '自选', 2: '分配' }

/** 节次选项：1-12 */
export const SECTION_OPTIONS = Array.from({ length: 12 }, (_, i) => ({
  label: `第${i + 1}节`,
  value: i + 1
}))

/** 周次选项：1-20 */
export const WEEK_OPTIONS = Array.from({ length: 20 }, (_, i) => ({
  label: `第${i + 1}周`,
  value: i + 1
}))

export const WEEK_DAY_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

/** 上课节次对应的时间段文案 */
export const SECTION_TIME = {
  1: '08:00-08:45',
  2: '08:55-09:40',
  3: '10:00-10:45',
  4: '10:55-11:40',
  5: '14:00-14:45',
  6: '14:55-15:40',
  7: '16:00-16:45',
  8: '16:55-17:40',
  9: '19:00-19:45',
  10: '19:55-20:40',
  11: '20:50-21:35',
  12: '21:45-22:30'
}

/** 成绩等级（与后端 calcGradePoint 保持一致） */
export function gradePointOf(score) {
  if (score == null) return 0
  const s = Number(score)
  if (s >= 90) return 4.0
  if (s >= 85) return 3.7
  if (s >= 82) return 3.3
  if (s >= 78) return 3.0
  if (s >= 75) return 2.7
  if (s >= 72) return 2.3
  if (s >= 68) return 2.0
  if (s >= 64) return 1.5
  if (s >= 60) return 1.0
  return 0.0
}

export function levelOf(score) {
  if (score == null || score === '') return '未录入'
  const s = Number(score)
  if (s >= 90) return '优秀'
  if (s >= 80) return '良好'
  if (s >= 70) return '中等'
  if (s >= 60) return '及格'
  return '不及格'
}
