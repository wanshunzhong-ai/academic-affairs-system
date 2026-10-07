<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { classApi, scoreApi, semesterApi } from '@/api'
import ChartBox from '@/components/ChartBox.vue'
import { fixed, scoreColor } from '@/utils/format'

const semesters = ref([])
const classes = ref([])
const semesterId = ref(null)
const classId = ref(null)
const loading = ref(false)

const levelDist = ref([])
const courseStats = ref([])
const rank = ref([])

const LEVEL_COLORS = ['#16a34a', '#2563eb', '#d97706', '#64748b', '#dc2626']

const levelOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12 } },
  color: LEVEL_COLORS,
  series: [
    {
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '44%'],
      label: { show: true, formatter: '{b}\n{c}', fontSize: 11, color: '#475569' },
      labelLine: { length: 8, length2: 8 },
      data: levelDist.value
    }
  ]
}))

/** 课程平均分对比（Top 12） */
const courseOption = computed(() => {
  const list = [...courseStats.value].sort((a, b) => b.avgScore - a.avgScore).slice(0, 12)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 30, top: 24, bottom: 8, containLabel: true },
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
        itemStyle: { borderRadius: [0, 4, 4, 0], color: (p) => scoreColor(p.value) },
        label: { show: true, position: 'right', fontSize: 11, color: '#475569', formatter: '{c}' },
        data: list.map((i) => i.avgScore).reverse()
      }
    ]
  }
})

/** 班级排名折线/柱状 */
const rankOption = computed(() => {
  const list = rank.value.slice(0, 20)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 20, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map((i) => i.studentName),
      axisLabel: { fontSize: 11, color: '#64748b', rotate: list.length > 8 ? 35 : 0 },
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
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: { borderRadius: [4, 4, 0, 0], color: (p) => scoreColor(p.value) },
        label: { show: true, position: 'top', fontSize: 10, color: '#475569', formatter: '{c}' },
        data: list.map((i) => i.avgScore)
      }
    ]
  }
})

/** 整体概况 */
const overall = computed(() => {
  const totalCount = courseStats.value.reduce((s, i) => s + Number(i.totalCount || 0), 0)
  const passCount = courseStats.value.reduce((s, i) => s + Number(i.passCount || 0), 0)
  const excellent = courseStats.value.reduce((s, i) => s + Number(i.excellentCount || 0), 0)
  const avg =
    courseStats.value.length === 0
      ? 0
      : courseStats.value.reduce((s, i) => s + Number(i.avgScore || 0), 0) / courseStats.value.length
  return {
    courseCount: courseStats.value.length,
    totalCount,
    passRate: totalCount ? ((passCount / totalCount) * 100).toFixed(2) : '0.00',
    excellentRate: totalCount ? ((excellent / totalCount) * 100).toFixed(2) : '0.00',
    avgScore: fixed(avg, 2)
  }
})

async function loadOptions() {
  const [s, c] = await Promise.all([semesterApi.list(), classApi.options()])
  semesters.value = s.data || []
  classes.value = c.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1) || semesters.value[0]
  semesterId.value = cur?.id || null
  classId.value = classes.value[0]?.id || null
}

async function load() {
  loading.value = true
  try {
    const tasks = [
      scoreApi.levelDistribution(),
      scoreApi.courseStats(classId.value, semesterId.value)
    ]
    const useRank = !!classId.value
    if (useRank) tasks.push(scoreApi.classRank(classId.value, semesterId.value, 20))
    const [l, c, r] = await Promise.all(tasks)
    levelDist.value = l.data || []
    courseStats.value = c.data || []
    rank.value = r?.data || []
  } finally {
    loading.value = false
  }
}

async function exportData() {
  await scoreApi.export({ semesterId: semesterId.value, classId: classId.value })
  ElMessage.success('导出已开始，请查看浏览器下载')
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-16">
      <el-form inline @submit.prevent>
        <el-form-item label="学期">
          <el-select v-model="semesterId" clearable placeholder="全部学期" style="width: 210px" @change="load">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="classId" clearable filterable placeholder="全校 / 选择班级" style="width: 230px" @change="load">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">
            <el-icon><Search /></el-icon>统计
          </el-button>
          <el-button plain @click="exportData">
            <el-icon><Download /></el-icon>导出成绩
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #3b82f6, #2563eb)">
          <el-icon><Notebook /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ overall.courseCount }}</div>
          <div class="stat-label">统计课程数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #22c55e, #16a34a)">
          <el-icon><Avatar /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ overall.totalCount }}</div>
          <div class="stat-label">成绩记录数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #0ea5e9, #0284c7)">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ overall.avgScore }}</div>
          <div class="stat-label">平均分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706)">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ overall.passRate }}<span class="stat-suffix">%</span></div>
          <div class="stat-label">及格率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #7c3aed)">
          <el-icon><Medal /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ overall.excellentRate }}<span class="stat-suffix">%</span></div>
          <div class="stat-label">优秀率</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="mb-16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header><div class="card-head"><span>课程平均分对比（Top 12）</span></div></template>
          <ChartBox
            :option="courseOption"
            height="380px"
            :loading="loading"
            :empty="!courseStats.length"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header><div class="card-head"><span>全校成绩等级分布</span></div></template>
          <ChartBox :option="levelOption" height="380px" :loading="loading" :empty="!levelDist.length" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mb-16">
      <template #header>
        <div class="card-head">
          <span>班级学生成绩排名{{ classId ? '' : '（请选择班级）' }}</span>
        </div>
      </template>
      <ChartBox
        :option="rankOption"
        height="320px"
        :loading="loading"
        :empty="!rank.length"
        empty-text="请选择班级查看排名"
      />
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-head"><span>课程成绩明细统计</span></div></template>
      <el-table v-loading="loading" :data="courseStats" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="totalCount" label="人数" width="80" align="center" />
        <el-table-column label="平均分" width="100" align="center" sortable :sort-by="(r) => r.avgScore">
          <template #default="{ row }">
            <b :style="{ color: scoreColor(row.avgScore) }">{{ fixed(row.avgScore, 2) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="最高分" width="90" align="center">
          <template #default="{ row }">{{ fixed(row.maxScore, 1) }}</template>
        </el-table-column>
        <el-table-column label="最低分" width="90" align="center">
          <template #default="{ row }">{{ fixed(row.minScore, 1) }}</template>
        </el-table-column>
        <el-table-column prop="excellentCount" label="优秀人数" width="100" align="center" />
        <el-table-column prop="passCount" label="及格人数" width="100" align="center" />
        <el-table-column label="不及格" width="90" align="center">
          <template #default="{ row }">
            <span class="text-danger">{{ (row.totalCount || 0) - (row.passCount || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="及格率" width="100" align="center">
          <template #default="{ row }">
            {{
              row.totalCount
                ? (((row.passCount || 0) / row.totalCount) * 100).toFixed(1) + '%'
                : '-'
            }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !courseStats.length" description="暂无统计数据" :image-size="90" />
    </el-card>
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-suffix {
  font-size: 13px;
  font-weight: 400;
  color: var(--aas-text-secondary);
  margin-left: 3px;
}
</style>
