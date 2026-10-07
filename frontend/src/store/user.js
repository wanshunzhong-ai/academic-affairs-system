import { defineStore } from 'pinia'
import { authApi, leaveApi, noticeApi } from '@/api'
import { getToken, setToken, clearAll, getStoredUser, setStoredUser } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: getStoredUser() || null,
    /** 后端返回的菜单树 */
    menus: getStoredUser()?.menus || [],
    /** 权限标识集合 */
    permissions: new Set(getStoredUser()?.permissions || []),
    /** 待办数量 */
    badges: { leavePending: 0, noticeUnread: 0 },
    /** 是否已加载用户信息 */
    loaded: false
  }),

  getters: {
    roleCode: (state) => state.userInfo?.roleCode || '',
    roleName: (state) => state.userInfo?.roleName || '',
    realName: (state) => state.userInfo?.realName || '',
    isAdmin: (state) => state.userInfo?.roleCode === 'ADMIN',
    isAcademic: (state) => state.userInfo?.roleCode === 'ACADEMIC',
    isHeadTeacher: (state) => state.userInfo?.roleCode === 'HEAD_TEACHER',
    isStudent: (state) => state.userInfo?.roleCode === 'STUDENT',
    manageClassIds: (state) => state.userInfo?.manageClassIds || [],
    studentId: (state) => state.userInfo?.studentId,
    teacherId: (state) => state.userInfo?.teacherId,
    classId: (state) => state.userInfo?.classId
  },

  actions: {
    /** 登录 */
    async login(form) {
      const res = await authApi.login(form)
      const data = res.data
      this.token = data.token
      setToken(data.token)
      await this.applyUserInfo(data.userInfo)
      return data
    },

    /** 拉取当前用户信息 */
    async fetchInfo() {
      const res = await authApi.getInfo()
      await this.applyUserInfo(res.data)
      return res.data
    },

    /** 应用用户信息 */
    async applyUserInfo(info) {
      this.userInfo = info
      this.menus = info.menus || []
      this.permissions = new Set(info.permissions || [])
      this.loaded = true
      setStoredUser(info)
      this.refreshBadges()
    },

    /** 刷新角标 */
    async refreshBadges() {
      try {
        if (this.isHeadTeacher || this.isAcademic || this.isAdmin) {
          const res = await leaveApi.pendingCount()
          this.badges.leavePending = res.data?.pending || 0
        }
      } catch {
        /* 忽略 */
      }
      try {
        const res = await noticeApi.unreadCount()
        this.badges.noticeUnread = res.data || 0
      } catch {
        /* 忽略 */
      }
    },

    /** 是否拥有某权限 */
    hasPerm(perm) {
      if (!perm) return true
      if (this.isAdmin) return true
      return this.permissions.has(perm)
    },

    /** 是否拥有任一权限 */
    hasAnyPerm(...perms) {
      if (this.isAdmin) return true
      return perms.some((p) => this.permSafeHas(p))
    },

    permSafeHas(p) {
      return this.permissions.has(p)
    },

    /** 是否拥有角色 */
    hasRole(...roles) {
      return roles.includes(this.userInfo?.roleCode)
    },

    /** 退出登录 */
    async logout() {
      try {
        await authApi.logout()
      } catch {
        /* 忽略 */
      }
      this.reset()
    },

    reset() {
      this.token = ''
      this.userInfo = null
      this.menus = []
      this.permissions = new Set()
      this.badges = { leavePending: 0, noticeUnread: 0 }
      this.loaded = false
      clearAll()
    }
  }
})
