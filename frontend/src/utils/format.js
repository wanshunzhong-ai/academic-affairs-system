/** 通用格式化工具 */

export function genderText(gender) {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '-'
}

export function studentStatusText(status) {
  const map = { 1: '在读', 2: '休学', 3: '退学', 4: '毕业' }
  return map[status] || '-'
}

export function studentStatusType(status) {
  const map = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }
  return map[status] || 'info'
}

export function leaveStatusText(status) {
  const map = { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已撤销' }
  return map[status] || '-'
}

export function leaveStatusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

export function scoreStatusText(status) {
  const map = { 0: '未录入', 1: '已录入', 2: '已发布' }
  return map[status] || '-'
}

export function scoreStatusType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

export function scoreLevelType(level) {
  const map = { 优秀: 'success', 良好: 'primary', 中等: 'warning', 及格: 'info', 不及格: 'danger', 未录入: 'info' }
  return map[level] || 'info'
}

export function attendTypeText(type) {
  const map = { 1: '出勤', 2: '迟到', 3: '早退', 4: '缺勤', 5: '请假' }
  return map[type] || '-'
}

export function attendTypeTag(type) {
  const map = { 1: 'success', 2: 'warning', 3: 'warning', 4: 'danger', 5: 'info' }
  return map[type] || 'info'
}

export function offeringStatusText(status) {
  const map = { 0: '未发布', 1: '已发布', 2: '已结课' }
  return map[status] || '-'
}

export function offeringStatusType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

export function weekDayText(day) {
  const map = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return map[day] || ''
}

export function noticeTypeTag(type) {
  const map = { 通知: 'primary', 公告: 'success', 教务: 'warning', 紧急: 'danger' }
  return map[type] || 'info'
}

/** 满分 100 分对应颜色（中国习惯：分数越高越好，用蓝/绿） */
export function scoreColor(score) {
  if (score == null) return '#94a3b8'
  if (score >= 90) return '#16a34a'
  if (score >= 80) return '#2563eb'
  if (score >= 70) return '#d97706'
  if (score >= 60) return '#64748b'
  return '#dc2626'
}

/** 数字保留 n 位 */
export function fixed(value, n = 2) {
  if (value == null || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  return num.toFixed(n)
}

/** 生成学期下拉的学年 */
export function currentSchoolYear() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  return m >= 8 ? `${y}-${y + 1}` : `${y - 1}-${y}`
}

/** 简单深拷贝 */
export function clone(obj) {
  return JSON.parse(JSON.stringify(obj))
}

/** 数组转树 */
export function listToTree(list, idKey = 'id', parentKey = 'parentId', rootValue = 0) {
  const map = new Map()
  const roots = []
  list.forEach((item) => {
    map.set(item[idKey], { ...item, children: [] })
  })
  list.forEach((item) => {
    const node = map.get(item[idKey])
    const parent = map.get(item[parentKey])
    if (parent && parent[idKey] !== node[idKey]) {
      parent.children.push(node)
    } else if (item[parentKey] === rootValue || !parent) {
      roots.push(node)
    }
  })
  return roots
}
