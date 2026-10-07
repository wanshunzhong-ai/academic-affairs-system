import request, { download } from '@/utils/request'

/* ==================== 认证 ==================== */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  getInfo: () => request.get('/auth/info'),
  getMenus: () => request.get('/auth/menus'),
  changePassword: (data) => request.post('/auth/change-password', data),
  ping: () => request.get('/auth/ping'),
  /** 登录页配置（是否展示演示账号一键填充），登录前即可读取 */
  loginConfig: () => request.get('/auth/login-config')
}

/* ==================== 工作台 ==================== */
export const dashboardApi = {
  overview: () => request.get('/dashboard/overview'),
  deptDistribution: () => request.get('/dashboard/dept-distribution'),
  majorDistribution: () => request.get('/dashboard/major-distribution'),
  classDistribution: () => request.get('/dashboard/class-distribution')
}

/* ==================== 系统管理 ==================== */
export const userApi = {
  page: (params) => request.get('/system/user/page', { params }),
  detail: (id) => request.get(`/system/user/${id}`),
  create: (data) => request.post('/system/user', data),
  update: (data) => request.put('/system/user', data),
  remove: (id) => request.delete(`/system/user/${id}`),
  removeBatch: (ids) => request.delete('/system/user/batch', { data: ids }),
  resetPassword: (id, password) => request.put(`/system/user/${id}/reset-password`, null, { params: { password } }),
  changeStatus: (id, status) => request.put(`/system/user/${id}/status`, null, { params: { status } }),
  assignRoles: (id, roleIds) => request.put(`/system/user/${id}/roles`, roleIds)
}

export const roleApi = {
  list: (params) => request.get('/system/role/list', { params }),
  options: () => request.get('/system/role/options'),
  detail: (id) => request.get(`/system/role/${id}`),
  menuIds: (id) => request.get(`/system/role/${id}/menus`),
  create: (data) => request.post('/system/role', data),
  update: (data) => request.put('/system/role', data),
  remove: (id) => request.delete(`/system/role/${id}`),
  assignMenus: (id, menuIds) => request.put(`/system/role/${id}/menus`, menuIds),
  changeStatus: (id, status) => request.put(`/system/role/${id}/status`, null, { params: { status } })
}

export const menuApi = {
  tree: (params) => request.get('/system/menu/tree', { params }),
  list: () => request.get('/system/menu/list'),
  create: (data) => request.post('/system/menu', data),
  update: (data) => request.put('/system/menu', data),
  remove: (id) => request.delete(`/system/menu/${id}`)
}

export const logApi = {
  page: (params) => request.get('/system/log/page', { params }),
  modules: () => request.get('/system/log/modules'),
  removeBatch: (ids) => request.delete('/system/log/batch', { data: ids }),
  clear: () => request.delete('/system/log/clear'),
  clean: (days) => request.delete('/system/log/clean', { params: { days } })
}

/* ==================== 基础数据 ==================== */
export const deptApi = {
  page: (params) => request.get('/base/dept/page', { params }),
  options: () => request.get('/base/dept/options'),
  detail: (id) => request.get(`/base/dept/${id}`),
  create: (data) => request.post('/base/dept', data),
  update: (data) => request.put('/base/dept', data),
  remove: (id) => request.delete(`/base/dept/${id}`)
}

export const majorApi = {
  page: (params) => request.get('/base/major/page', { params }),
  options: (deptId) => request.get('/base/major/options', { params: { deptId } }),
  detail: (id) => request.get(`/base/major/${id}`),
  create: (data) => request.post('/base/major', data),
  update: (data) => request.put('/base/major', data),
  remove: (id) => request.delete(`/base/major/${id}`)
}

export const classApi = {
  page: (params) => request.get('/base/class/page', { params }),
  options: () => request.get('/base/class/options'),
  my: () => request.get('/base/class/my'),
  detail: (id) => request.get(`/base/class/${id}`),
  create: (data) => request.post('/base/class', data),
  update: (data) => request.put('/base/class', data),
  remove: (id) => request.delete(`/base/class/${id}`)
}

export const semesterApi = {
  list: () => request.get('/base/semester/list'),
  current: () => request.get('/base/semester/current'),
  detail: (id) => request.get(`/base/semester/${id}`),
  create: (data) => request.post('/base/semester', data),
  update: (data) => request.put('/base/semester', data),
  setCurrent: (id) => request.put(`/base/semester/${id}/current`),
  remove: (id) => request.delete(`/base/semester/${id}`)
}

export const classroomApi = {
  page: (params) => request.get('/base/classroom/page', { params }),
  options: () => request.get('/base/classroom/options'),
  create: (data) => request.post('/base/classroom', data),
  update: (data) => request.put('/base/classroom', data),
  remove: (id) => request.delete(`/base/classroom/${id}`)
}

/* ==================== 学生 ==================== */
export const studentApi = {
  page: (params) => request.get('/student/page', { params }),
  detail: (id) => request.get(`/student/${id}`),
  my: () => request.get('/student/my'),
  mySummary: () => request.get('/student/my/summary'),
  listByClass: (classId) => request.get(`/student/class/${classId}`),
  options: (keyword) => request.get('/student/options', { params: { keyword } }),
  create: (data) => request.post('/student', data),
  update: (data) => request.put('/student', data),
  remove: (id) => request.delete(`/student/${id}`),
  removeBatch: (ids) => request.delete('/student/batch', { data: ids }),
  changeStatus: (id, status) => request.put(`/student/${id}/status`, null, { params: { status } }),
  export: (params) => download('/student/export', params, '学生信息表.xlsx'),
  downloadTemplate: () => download('/student/template', {}, '学生导入模板.xlsx'),
  import: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/student/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  }
}

/* ==================== 教师 ==================== */
export const teacherApi = {
  page: (params) => request.get('/teacher/page', { params }),
  options: (deptId) => request.get('/teacher/options', { params: { deptId } }),
  headTeachers: () => request.get('/teacher/head-teachers'),
  detail: (id) => request.get(`/teacher/${id}`),
  my: () => request.get('/teacher/my'),
  create: (data) => request.post('/teacher', data),
  update: (data) => request.put('/teacher', data),
  remove: (id) => request.delete(`/teacher/${id}`),
  assignClasses: (id, classIds) => request.put(`/teacher/${id}/classes`, classIds),
  changeStatus: (id, status) => request.put(`/teacher/${id}/status`, null, { params: { status } })
}

/* ==================== 课程 ==================== */
export const courseApi = {
  page: (params) => request.get('/course/page', { params }),
  options: () => request.get('/course/options'),
  detail: (id) => request.get(`/course/${id}`),
  create: (data) => request.post('/course', data),
  update: (data) => request.put('/course', data),
  remove: (id) => request.delete(`/course/${id}`),
  changeStatus: (id, status) => request.put(`/course/${id}/status`, null, { params: { status } })
}

/* ==================== 开课与课表 ==================== */
export const offeringApi = {
  page: (params) => request.get('/offering/page', { params }),
  detail: (id) => request.get(`/offering/${id}`),
  schedules: (id) => request.get(`/offering/${id}/schedules`),
  create: (data) => request.post('/offering', data),
  update: (data) => request.put('/offering', data),
  saveSchedules: (id, schedules) => request.put(`/offering/${id}/schedules`, schedules),
  remove: (id) => request.delete(`/offering/${id}`),
  removeBatch: (ids) => request.delete('/offering/batch', { data: ids }),
  changeStatus: (id, status) => request.put(`/offering/${id}/status`, null, { params: { status } }),
  stat: (id) => request.get(`/offering/${id}/stat`),
  publicList: (semesterId) => request.get('/offering/public', { params: { semesterId } }),
  selectable: (params) => request.get('/offering/selectable', { params }),
  myTimetable: (semesterId) => request.get('/offering/timetable', { params: { semesterId } }),
  studentTimetable: (studentId, semesterId) =>
    request.get(`/offering/timetable/student/${studentId}`, { params: { semesterId } }),
  teacherTimetable: (teacherId, semesterId) =>
    request.get(`/offering/timetable/teacher/${teacherId}`, { params: { semesterId } }),
  classTimetable: (classId, semesterId) =>
    request.get(`/offering/timetable/class/${classId}`, { params: { semesterId } })
}

/* ==================== 选课 ==================== */
export const selectionApi = {
  page: (params) => request.get('/selection/page', { params }),
  my: (semesterId) => request.get('/selection/my', { params: { semesterId } }),
  myCredit: (semesterId) => request.get('/selection/my/credit', { params: { semesterId } }),
  select: (offeringId) => request.post('/selection/select', null, { params: { offeringId } }),
  batchSelect: (offeringIds) => request.post('/selection/select/batch', offeringIds),
  drop: (offeringId) => request.post('/selection/drop', null, { params: { offeringId } }),
  offeringStudents: (offeringId) => request.get(`/selection/offering/${offeringId}`),
  assign: (offeringId, studentIds) => request.post('/selection/assign', studentIds, { params: { offeringId } }),
  remove: (id) => request.delete(`/selection/${id}`),
  export: (params) => download('/selection/export', params, '选课名单.xlsx')
}

/* ==================== 成绩 ==================== */
export const scoreApi = {
  page: (params) => request.get('/score/page', { params }),
  my: (semesterId, onlyPublished = true) => request.get('/score/my', { params: { semesterId, onlyPublished } }),
  myDistribution: () => request.get('/score/my/distribution'),
  myTrend: () => request.get('/score/my/trend'),
  offeringScores: (offeringId) => request.get(`/score/offering/${offeringId}`),
  init: (offeringId) => request.post(`/score/offering/${offeringId}/init`),
  saveBatch: (offeringId, records) => request.post(`/score/offering/${offeringId}/save`, records),
  saveOne: (data) => request.post('/score/save', data),
  publish: (offeringId, ids) => request.put('/score/publish', ids, { params: { offeringId } }),
  revoke: (offeringId, ids) => request.put('/score/revoke', ids, { params: { offeringId } }),
  remove: (id) => request.delete(`/score/${id}`),
  offeringSummary: (offeringId) => request.get(`/score/offering/${offeringId}/summary`),
  offeringDistribution: (offeringId) => request.get(`/score/offering/${offeringId}/distribution`),
  courseStats: (classId, semesterId) => request.get('/score/stat/course', { params: { classId, semesterId } }),
  levelDistribution: () => request.get('/score/stat/level'),
  classRank: (classId, semesterId, limit = 20) =>
    request.get('/score/stat/rank', { params: { classId, semesterId, limit } }),
  export: (params) => download('/score/export', params, '成绩表.xlsx')
}

/* ==================== 考勤 ==================== */
export const attendanceApi = {
  page: (params) => request.get('/attendance/page', { params }),
  my: () => request.get('/attendance/my'),
  create: (data) => request.post('/attendance', data),
  update: (data) => request.put('/attendance', data),
  remove: (id) => request.delete(`/attendance/${id}`),
  removeBatch: (ids) => request.delete('/attendance/batch', { data: ids }),
  draft: (offeringId) => request.get(`/attendance/draft/${offeringId}`),
  batchRecord: (offeringId, date, records) =>
    request.post(`/attendance/batch/${offeringId}`, records, { params: { date } }),
  classStats: (classId) => request.get(`/attendance/stat/class/${classId}`)
}

/* ==================== 请假 ==================== */
export const leaveApi = {
  page: (params) => request.get('/leave/page', { params }),
  detail: (id) => request.get(`/leave/${id}`),
  apply: (data) => request.post('/leave', data),
  approve: (id, status, remark) => request.put(`/leave/${id}/approve`, null, { params: { status, remark } }),
  batchApprove: (ids, status, remark) =>
    request.put('/leave/batch-approve', ids, { params: { status, remark } }),
  cancel: (id) => request.put(`/leave/${id}/cancel`),
  pendingCount: () => request.get('/leave/pending-count'),
  remove: (id) => request.delete(`/leave/${id}`)
}

/* ==================== 公告 ==================== */
export const noticeApi = {
  page: (params) => request.get('/notice/page', { params }),
  managePage: (params) => request.get('/notice/manage/page', { params }),
  detail: (id) => request.get(`/notice/${id}`),
  markRead: (id) => request.put(`/notice/${id}/read`),
  unreadCount: () => request.get('/notice/unread-count'),
  create: (data) => request.post('/notice', data),
  update: (data) => request.put('/notice', data),
  changeStatus: (id, status) => request.put(`/notice/${id}/status`, null, { params: { status } }),
  remove: (id) => request.delete(`/notice/${id}`),
  removeBatch: (ids) => request.delete('/notice/batch', { data: ids })
}
