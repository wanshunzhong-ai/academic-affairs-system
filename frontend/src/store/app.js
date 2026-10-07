import { defineStore } from 'pinia'
import { semesterApi } from '@/api'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('aas_sidebar_collapsed') === '1',
    /** 学期列表 */
    semesters: [],
    /** 当前学期 */
    currentSemester: null
  }),

  getters: {
    currentSemesterId: (state) => state.currentSemester?.id || null,
    currentSemesterName: (state) => state.currentSemester?.semesterName || ''
  },

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('aas_sidebar_collapsed', this.sidebarCollapsed ? '1' : '0')
    },

    async loadSemesters() {
      if (this.semesters.length) return this.semesters
      const res = await semesterApi.list()
      this.semesters = res.data || []
      this.currentSemester = this.semesters.find((s) => s.isCurrent === 1) || this.semesters[0] || null
      return this.semesters
    }
  }
})
