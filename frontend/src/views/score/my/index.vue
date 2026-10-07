<script setup>
import { computed, onMounted, ref } from 'vue'
import { scoreApi, semesterApi, studentApi } from '@/api'
import ChartBox from '@/components/ChartBox.vue'
import StatCard from '@/components/StatCard.vue'
import { fixed, scoreColor, scoreStatusText, scoreStatusType } from '@/utils/format'

const semesters = ref([])
const semesterId = ref(null)
const onlyPublished = ref(true)
const loading = ref(false)
const rows = ref([])
const summaryData = ref({})
const trend = ref([])
const distribution = ref([])

const LEVEL_COLORS = ['#16a34a', '#2563eb', '#d97706', '#64748b', '#dc2626']

/** 绩点换算百分制近似分，用于着色 */
function pointColor(point) {
  if (point == null) return '#94a3b8'
  return scoreColor((Number(point) / 4) * 100)
}

const summaryCards = computed(() => {
  const s = summaryData.value || {}
  return [
    { label: '课程门数', value: s.courseCount ?? 0, suffix: '门', color: 'linear-gradient(135deg,#3b82f6,#2563eb)' },
    { label: '平均成绩', value: fixed(s.avgScore, 2), color: 'linear-gradient(135deg,#22c55e,#16a34a)' },
    { label: '平均绩点', value: fixed(s.avgPoint, 2), color: 'linear-gradient(135deg,#8b5cf6,#7c3aed)' },
    { label: '最高分', value: fixed(s.maxScore, 1), color: 'linear-gradient(135deg,#0ea5e9,#0284c7)' },
    { label: '已获学分', value: fixed(s.earnedCredit, 1), color: 'linear-gradient(135deg,#14b8a6,#0d9488)' },
    { label: '不及格课程', value: s.failCount ?? 0, suffix: '门', color: 'linear-gradient(135deg,#ef4444,#dc2626)' }
  ]
})

const levelOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12 } },
  color: LEVEL_COLORS,
  series: [
    {
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '44%'],
      label: { show: true, formatter: '{b}\n{c}', fontSize: 11, color: '#475569' },
      labelLine: { length: 8, length2: 8 },
      data: distribution.value
    }
  ]
}))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['平均分', '平均绩点×25'], top: 0, textStyle: { fontSize: 12 } },
  grid: { left: 8, right: 20, top: 40, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: trend.value.map((i) => i.semester),
    axisLabel: { fontSize: 11, color: '#64748b', rotate: trend.value.length > 4 ? 25 : 0 },
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e5e7eb' } }
  },
  yAxis: {
    type: 'value',
    max: 100,
    splitLine: { lineStyle: { color: '#f1f5f9' } },
    axisLabel: { fontSize: 11, color: '#94a3b8' }
  },
  series: [
    {
      name: '平均分',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      lineStyle: { width: 3, color: '#2563eb' },
      itemStyle: { color: '#2563eb' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(37,99,235,0.28)' },
            { offset: 1, color: 'rgba(37,99,235,0.02)' }
          ]
        }
      },
      label: { show: true, fontSize: 11, color: '#2563eb', formatter: '{c}' },
      data: trend.value.map((i) => i.avgScore)
    },
    {
      name: '平均绩点×25',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      lineStyle: { width: 2, type: 'dashed', color: '#d97706' },
      itemStyle: { color: '#d97706' },
      data: trend.value.map((i) => (Number(i.avgPoint || 0) * 25).toFixed(2))
    }
  ]
}))

async function loadSemesters() {
  const res = await semesterApi.list()
  semesters.value = res.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1) || semesters.value[0]
  semesterId.value = cur?.id || null
}

async function load() {
  loading.value = true
  try {
    const [s, sum, t, d] = await Promise.all([
      scoreApi.my(semesterId.value, onlyPublished.value),
      studentApi.mySummary(),
      scoreApi.myTrend(),
      scoreApi.myDistribution()
    ])
    rows.value = s.data || []
    summaryData.value = sum.data || {}
    trend.value = t.data || []
    distribution.value = d.data || []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadSemesters()
  await load()
})
</script>

<template>
  <div class="app-container">
    <div class="stat-grid">
      <StatCard
        v-for="c in summaryCards"
        :key="c.label"
        :label="c.label"
        :value="c.value"
        :suffix="c.suffix"
        :color="c.color"
      />
    </div>

    <el-row :gutter="16" class="mb-16">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never">
          <template #header><div class="card-head"><span>成绩趋势（各学期平均分）</span></div></template>
          <ChartBox
            :option="trendOption"
            height="300px"
            :loading="loading"
            :empty="!trend.length"
            empty-text="暂无历史成绩数据"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card shadow="never">
          <template #header><div class="card-head"><span>成绩等级分布</span></div></template>
          <ChartBox
            :option="levelOption"
            height="300px"
            :loading="loading"
            :empty="!distribution.length"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>我的成绩单</span>
          <div class="head-actions">
            <el-select v-model="semesterId" placeholder="全部学期" clearable size="small" style="width: 200px" @change="load">
              <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
            </el-select>
            <el-switch
              v-model="onlyPublished"
              active-text="仅看已发布"
              size="small"
              @change="load"
            />
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="courseCode" label="课程编码" width="105" />
        <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseType" label="性质" width="80" align="center" />
        <el-table-column prop="credit" label="学分" width="70" align="center" />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column prop="usualScore" label="平时" width="80" align="center">
          <template #default="{ row }">{{ fixed(row.usualScore, 1) }}</template>
        </el-table-column>
        <el-table-column prop="examScore" label="期末" width="80" align="center">
          <template #default="{ row }">{{ fixed(row.examScore, 1) }}</template>
        </el-table-column>
        <el-table-column label="总评" width="90" align="center" sortable :sort-by="(r) => r.totalScore ?? -1">
          <template #default="{ row }">
            <b :style="{ color: scoreColor(row.totalScore) }">{{ fixed(row.totalScore, 1) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="绩点" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: pointColor(row.gradePoint) }">{{ fixed(row.gradePoint, 1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              effect="plain"
              :type="row.level === '不及格' ? 'danger' : row.level === '优秀' ? 'success' : 'primary'"
            >
              {{ row.level || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="scoreStatusType(row.status)" size="small" effect="plain">
              {{ scoreStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="semesterName" label="学期" min-width="180" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无成绩记录" :image-size="90" />
    </el-card>
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}
</style>
