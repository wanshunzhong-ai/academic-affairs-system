<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '@/api'
import { useUserStore } from '@/store/user'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import TimetableGrid from '@/components/TimetableGrid.vue'
import { fixed, scoreColor, studentStatusText, genderText } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const data = ref({})

const roleCode = computed(() => data.value.roleCode || userStore.roleCode)
const isAdmin = computed(() => roleCode.value === 'ADMIN')
const isAcademic = computed(() => roleCode.value === 'ACADEMIC')
const isHeadTeacher = computed(() => roleCode.value === 'HEAD_TEACHER')
const isStudent = computed(() => roleCode.value === 'STUDENT')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

async function load() {
  loading.value = true
  try {
    const res = await dashboardApi.overview()
    data.value = res.data || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)

/* ==================== 通用图表工具 ==================== */
function pieOption(list, colors) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12 } },
    color: colors,
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: { show: true, formatter: '{b}\n{c}', fontSize: 11, color: '#475569' },
        labelLine: { length: 8, length2: 8 },
        data: list
      }
    ]
  }
}

function barOption(list, nameKey, valueKey, color) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map((i) => i[nameKey]),
      axisLabel: { fontSize: 11, color: '#64748b', interval: 0, rotate: list.length > 8 ? 35 : 0 },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5e7eb' } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { fontSize: 11, color: '#94a3b8' }
    },
    series: [
      {
        name: '数量',
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: { borderRadius: [4, 4, 0, 0], color },
        data: list.map((i) => i[valueKey])
      }
    ]
  }
}

const LEVEL_COLORS = ['#16a34a', '#2563eb', '#d97706', '#64748b', '#dc2626']
const DEPT_COLORS = ['#2563eb', '#059669', '#d97706', '#7c3aed', '#db2777', '#0284c7']

/* ==================== 管理员 ==================== */
const adminCards = computed(() => [
  { label: '系统用户', value: data.value.userCount, color: 'linear-gradient(135deg,#3b82f6,#2563eb)', icon: 'User' },
  { label: '在校学生', value: data.value.studentCount, color: 'linear-gradient(135deg,#22c55e,#16a34a)', icon: 'Avatar' },
  { label: '教师人数', value: data.value.teacherCount, color: 'linear-gradient(135deg,#f59e0b,#d97706)', icon: 'Postcard' },
  { label: '班级数量', value: data.value.classCount, color: 'linear-gradient(135deg,#8b5cf6,#7c3aed)', icon: 'OfficeBuilding' },
  { label: '专业数量', value: data.value.majorCount, color: 'linear-gradient(135deg,#06b6d4,#0891b2)', icon: 'Collection' },
  { label: '院系数量', value: data.value.deptCount, color: 'linear-gradient(135deg,#ec4899,#db2777)', icon: 'Grid' },
  { label: '课程总数', value: data.value.courseCount, color: 'linear-gradient(135deg,#6366f1,#4f46e5)', icon: 'Notebook' },
  { label: '开课班级', value: data.value.offeringCount, color: 'linear-gradient(135deg,#14b8a6,#0d9488)', icon: 'Tickets' },
  { label: '选课记录', value: data.value.selectionCount, color: 'linear-gradient(135deg,#f97316,#ea580c)', icon: 'ShoppingCart' },
  { label: '角色数量', value: data.value.roleCount, color: 'linear-gradient(135deg,#64748b,#475569)', icon: 'Key' }
])

/* ==================== 教务处 ==================== */
const academicCards = computed(() => [
  { label: '在校学生', value: data.value.studentCount, color: 'linear-gradient(135deg,#22c55e,#16a34a)' },
  { label: '教师人数', value: data.value.teacherCount, color: 'linear-gradient(135deg,#f59e0b,#d97706)' },
  { label: '班级数量', value: data.value.classCount, color: 'linear-gradient(135deg,#8b5cf6,#7c3aed)' },
  { label: '课程总数', value: data.value.courseCount, color: 'linear-gradient(135deg,#6366f1,#4f46e5)' },
  { label: '开课班级', value: data.value.offeringCount, color: 'linear-gradient(135deg,#14b8a6,#0d9488)' },
  { label: '选课记录', value: data.value.selectionCount, color: 'linear-gradient(135deg,#f97316,#ea580c)' },
  { label: '待审批请假', value: data.value.pendingLeave, color: 'linear-gradient(135deg,#ef4444,#dc2626)' },
  { label: '未发布成绩', value: data.value.unpublishedScore, color: 'linear-gradient(135deg,#0ea5e9,#0284c7)' },
  { label: '异常考勤', value: data.value.abnormalAttendance, color: 'linear-gradient(135deg,#f43f5e,#e11d48)' },
  { label: '未读公告', value: data.value.unreadNotice, color: 'linear-gradient(135deg,#a855f7,#9333ea)' }
])

/* ==================== 班主任 ==================== */
const headTeacherCards = computed(() => [
  { label: '管理班级', value: data.value.manageClassCount, suffix: '个', color: 'linear-gradient(135deg,#3b82f6,#2563eb)' },
  { label: '班级学生', value: data.value.studentCount, suffix: '人', color: 'linear-gradient(135deg,#22c55e,#16a34a)' },
  { label: '待审批请假', value: data.value.pendingLeave, suffix: '条', color: 'linear-gradient(135deg,#ef4444,#dc2626)' },
  { label: '今日考勤记录', value: data.value.todayAttendance, suffix: '条', color: 'linear-gradient(135deg,#0ea5e9,#0284c7)' },
  { label: '班级平均分', value: fixed(data.value.avgScore, 2), color: 'linear-gradient(135deg,#f59e0b,#d97706)' },
  { label: '未读公告', value: data.value.unreadNotice, suffix: '条', color: 'linear-gradient(135deg,#a855f7,#9333ea)' }
])

/* ==================== 学生 ==================== */
const studentCards = computed(() => {
  const s = data.value.scoreSummary || {}
  return [
    { label: '本学期课程', value: data.value.courseCount, suffix: '门', color: 'linear-gradient(135deg,#3b82f6,#2563eb)' },
    { label: '已选学分', value: fixed(data.value.currentCredit, 1), color: 'linear-gradient(135deg,#14b8a6,#0d9488)' },
    { label: '平均成绩', value: fixed(s.avgScore, 2), color: 'linear-gradient(135deg,#22c55e,#16a34a)' },
    { label: '平均绩点', value: fixed(s.avgPoint, 2), color: 'linear-gradient(135deg,#8b5cf6,#7c3aed)' },
    { label: '已获学分', value: fixed(s.earnedCredit, 1), color: 'linear-gradient(135deg,#0ea5e9,#0284c7)' },
    { label: '不及格课程', value: s.failCount ?? 0, suffix: '门', color: 'linear-gradient(135deg,#ef4444,#dc2626)' },
    { label: '我的请假', value: data.value.myLeaveCount, suffix: '条', color: 'linear-gradient(135deg,#f59e0b,#d97706)' },
    { label: '未读公告', value: data.value.unreadNotice, suffix: '条', color: 'linear-gradient(135deg,#a855f7,#9333ea)' }
  ]
})

/* ==================== 图表 ==================== */
const scoreLevelOption = computed(() =>
  pieOption(data.value.scoreDistribution || [], LEVEL_COLORS)
)
const deptOption = computed(() => pieOption(data.value.deptDistribution || [], DEPT_COLORS))
const courseRankOption = computed(() => {
  const list = (data.value.classScoreRank || []).slice(0, 10)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 24, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { fontSize: 11, color: '#94a3b8' }
    },
    yAxis: {
      type: 'category',
      data: list.map((i) => i.name).reverse(),
      axisLabel: { fontSize: 11, color: '#64748b' },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5e7eb' } }
    },
    series: [
      {
        name: '平均分',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: (p) => scoreColor(p.value)
        },
        label: { show: true, position: 'right', fontSize: 11, color: '#475569', formatter: '{c}' },
        data: list.map((i) => i.avgScore).reverse()
      }
    ]
  }
})

const attendanceOption = computed(() => {
  const details = data.value.attendance?.details || []
  return pieOption(details, ['#16a34a', '#d97706', '#f59e0b', '#dc2626', '#64748b'])
})

/* ==================== 列表数据 ==================== */
const rankColumns = [
  { prop: 'name', label: '课程名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'avgScore', label: '平均分', width: 90, align: 'right' },
  { prop: 'maxScore', label: '最高分', width: 90, align: 'right' },
  { prop: 'minScore', label: '最低分', width: 90, align: 'right' },
  { prop: 'excellentCount', label: '优秀', width: 70, align: 'right' },
  { prop: 'passCount', label: '及格', width: 70, align: 'right' },
  { prop: 'totalCount', label: '人数', width: 70, align: 'right' }
]

const studentColumns = [
  { prop: 'studentNo', label: '学号', width: 110 },
  { prop: 'name', label: '姓名', width: 90 },
  { prop: 'gender', label: '性别', width: 64, align: 'center' },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'dormitory', label: '宿舍', minWidth: 100 },
  { prop: 'status', label: '状态', width: 80, align: 'center' }
]

function go(path) {
  router.push(path)
}
</script>

<template>
  <div class="app-container">
    <!-- 欢迎条 -->
    <div class="welcome-bar">
      <div class="wb-left">
        <div class="wb-avatar">{{ (data.realName || userStore.realName || '?').slice(0, 1) }}</div>
        <div>
          <div class="wb-title">
            {{ greeting }}，{{ data.realName || userStore.realName }}
            <el-tag size="small" effect="dark" :color="'#2563eb'" style="border: none; margin-left: 8px">
              {{ data.roleName || userStore.roleName }}
            </el-tag>
          </div>
          <div class="wb-sub">
            当前学期：{{ data.currentSemester || '未设置' }}
            <template v-if="isStudent">
              · {{ data.deptName }} / {{ data.majorName }} / {{ data.className }}
            </template>
            <template v-if="isHeadTeacher">
              · 带班：{{ (data.manageClassNames || []).join('、') }}
            </template>
          </div>
        </div>
      </div>
      <el-button text style="color: #fff" @click="load">
        <el-icon style="margin-right: 4px"><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div v-loading="loading" class="stat-grid">
      <template v-if="isAdmin">
        <StatCard
          v-for="c in adminCards"
          :key="c.label"
          :label="c.label"
          :value="c.value ?? '-'"
          :color="c.color"
        >
          <template #icon><el-icon><component :is="c.icon" /></el-icon></template>
        </StatCard>
      </template>

      <template v-else-if="isAcademic">
        <StatCard
          v-for="c in academicCards"
          :key="c.label"
          :label="c.label"
          :value="c.value ?? '-'"
          :color="c.color"
        />
      </template>

      <template v-else-if="isHeadTeacher">
        <StatCard
          v-for="c in headTeacherCards"
          :key="c.label"
          :label="c.label"
          :value="c.value ?? '-'"
          :suffix="c.suffix"
          :color="c.color"
        />
      </template>

      <template v-else-if="isStudent">
        <StatCard
          v-for="c in studentCards"
          :key="c.label"
          :label="c.label"
          :value="c.value ?? '-'"
          :suffix="c.suffix"
          :color="c.color"
        />
      </template>
    </div>

    <!-- 管理端 / 教务处 -->
    <el-row v-if="isAdmin || isAcademic" :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="mb-16">
          <template #header>
            <div class="card-head">
              <span>各课程成绩概况（Top 10）</span>
              <el-button text type="primary" size="small" @click="go('/score/stat')">成绩统计</el-button>
            </div>
          </template>
          <ChartBox
            :option="courseRankOption"
            height="340px"
            :loading="loading"
            :empty="!(data.classScoreRank || []).length"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="mb-16">
          <template #header>
            <div class="card-head">
              <span>成绩等级分布</span>
              <el-button text type="primary" size="small" @click="go('/score/stat')">查看明细</el-button>
            </div>
          </template>
          <ChartBox
            :option="scoreLevelOption"
            height="340px"
            :loading="loading"
            :empty="!(data.scoreDistribution || []).length"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="isAdmin" shadow="never" class="mb-16">
      <template #header>
        <div class="card-head">
          <span>院系学生分布</span>
          <el-button text type="primary" size="small" @click="go('/academic/dept')">院系管理</el-button>
        </div>
      </template>
      <ChartBox
        :option="deptOption"
        height="300px"
        :loading="loading"
        :empty="!(data.deptDistribution || []).length"
      />
    </el-card>

    <el-card v-if="isAcademic" shadow="never" class="mb-16">
      <template #header>
        <div class="card-head"><span>院系学生分布</span></div>
      </template>
      <ChartBox
        :option="deptOption"
        height="300px"
        :loading="loading"
        :empty="!(data.deptDistribution || []).length"
      />
    </el-card>

    <!-- 班主任 -->
    <template v-if="isHeadTeacher">
      <el-row :gutter="16">
        <el-col :xs="24" :lg="14">
          <el-card shadow="never" class="mb-16">
            <template #header>
              <div class="card-head">
                <span>本班各科平均分</span>
                <el-button text type="primary" size="small" @click="go('/score/input')">录入成绩</el-button>
              </div>
            </template>
            <ChartBox
              :option="courseRankOption"
              height="340px"
              :loading="loading"
              :empty="!(data.classScoreRank || []).length"
            />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="10">
          <el-card shadow="never" class="mb-16">
            <template #header><div class="card-head"><span>快捷入口</span></div></template>
            <div class="quick-grid">
              <div class="quick-item" @click="go('/academic/student')">
                <el-icon size="22" color="#2563eb"><User /></el-icon>
                <span>本班学生</span>
              </div>
              <div class="quick-item" @click="go('/attendance/approve')">
                <el-icon size="22" color="#ef4444"><Stamp /></el-icon>
                <span>请假审批</span>
                <el-badge v-if="data.pendingLeave" :value="data.pendingLeave" class="quick-badge" />
              </div>
              <div class="quick-item" @click="go('/attendance/record')">
                <el-icon size="22" color="#16a34a"><Finished /></el-icon>
                <span>考勤记录</span>
              </div>
              <div class="quick-item" @click="go('/teaching/timetable')">
                <el-icon size="22" color="#d97706"><Clock /></el-icon>
                <span>班级课表</span>
              </div>
              <div class="quick-item" @click="go('/notice/manage')">
                <el-icon size="22" color="#7c3aed"><Promotion /></el-icon>
                <span>发布公告</span>
              </div>
              <div class="quick-item" @click="go('/score/input')">
                <el-icon size="22" color="#0d9488"><EditPen /></el-icon>
                <span>成绩录入</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="mb-16">
        <template #header>
          <div class="card-head">
            <span>本班学生（前 10 位）</span>
            <el-button text type="primary" size="small" @click="go('/academic/student')">全部学生</el-button>
          </div>
        </template>
        <el-table :data="(data.studentList || []).slice(0, 10)" stripe size="small">
          <el-table-column
            v-for="col in studentColumns"
            :key="col.prop"
            :prop="col.prop"
            :label="col.label"
            :width="col.width"
            :min-width="col.minWidth"
            :align="col.align || 'left'"
          >
            <template v-if="col.prop === 'gender'" #default="{ row }">{{ genderText(row.gender) }}</template>
            <template v-else-if="col.prop === 'status'" #default="{ row }">
              {{ studentStatusText(row.status) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 学生 -->
    <template v-if="isStudent">
      <el-row :gutter="16">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="mb-16">
            <template #header><div class="card-head"><span>成绩等级分布</span></div></template>
            <ChartBox
              :option="scoreLevelOption"
              height="280px"
              :loading="loading"
              :empty="!(data.scoreDistribution || []).length"
            />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="mb-16">
            <template #header><div class="card-head"><span>考勤情况</span></div></template>
            <ChartBox
              :option="attendanceOption"
              height="280px"
              :loading="loading"
              :empty="!(data.attendance?.details || []).length"
            />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="mb-16">
            <template #header><div class="card-head"><span>快捷入口</span></div></template>
            <div class="quick-grid">
              <div class="quick-item" @click="go('/teaching/select')">
                <el-icon size="22" color="#2563eb"><ShoppingCart /></el-icon>
                <span>在线选课</span>
              </div>
              <div class="quick-item" @click="go('/score/my')">
                <el-icon size="22" color="#16a34a"><TrendCharts /></el-icon>
                <span>我的成绩</span>
              </div>
              <div class="quick-item" @click="go('/attendance/my')">
                <el-icon size="22" color="#d97706"><Document /></el-icon>
                <span>我的请假</span>
              </div>
              <div class="quick-item" @click="go('/notice/list')">
                <el-icon size="22" color="#7c3aed"><Bell /></el-icon>
                <span>通知公告</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="mb-16">
        <template #header>
          <div class="card-head">
            <span>我的课表（{{ data.currentSemester || '-' }}）</span>
            <el-button text type="primary" size="small" @click="go('/teaching/timetable')">课表查询</el-button>
          </div>
        </template>
        <el-empty v-if="!(data.timetable || []).length" description="本学期暂无课程安排" :image-size="90" />
        <div v-else class="timetable-wrap">
          <TimetableGrid :data="data.timetable" />
        </div>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #1e3a8a 0%, #2563eb 55%, #3b82f6 100%);
  color: #fff;
  border-radius: 10px;
  padding: 18px 22px;
  margin-bottom: 16px;
}
.wb-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.wb-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}
.wb-title {
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.wb-sub {
  font-size: 12.5px;
  opacity: 0.85;
  margin-top: 4px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.quick-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 14px;
  border: 1px solid var(--aas-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.18s;
  font-size: 13px;
  color: var(--aas-text);
}
.quick-item:hover {
  border-color: #93c5fd;
  background: #f8fafc;
  transform: translateY(-2px);
}
.quick-badge {
  position: absolute;
  top: 6px;
  right: 10px;
}
.timetable-wrap {
  overflow-x: auto;
}
</style>
