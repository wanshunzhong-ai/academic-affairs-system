<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, courseApi, offeringApi, scoreApi, semesterApi } from '@/api'
import { useTable } from '@/composables/useTable'
import ChartBox from '@/components/ChartBox.vue'
import { fixed, scoreColor, scoreStatusText, scoreStatusType } from '@/utils/format'
import { SCORE_STATUS_OPTIONS } from '@/utils/dict'

const {
  loading,
  list,
  total,
  query,
  selection,
  selectedIds,
  load,
  search,
  reset,
  onSelectionChange,
  onPageChange,
  onSizeChange
} = useTable(scoreApi.page, {
  keyword: '',
  semesterId: undefined,
  offeringId: undefined,
  classId: undefined,
  status: undefined,
  onlyFail: undefined
})

const semesters = ref([])
const courses = ref([])
const classes = ref([])
const offerings = ref([])

const summary = ref(null)
const distribution = ref([])

const LEVEL_COLORS = ['#16a34a', '#2563eb', '#d97706', '#64748b', '#dc2626']
const distributionOption = computed(() => ({
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

const selectedOffering = computed(() =>
  offerings.value.find((o) => o.id === query.offeringId)
)

async function loadOptions() {
  const [s, c, cl] = await Promise.all([semesterApi.list(), courseApi.options(), classApi.options()])
  semesters.value = s.data || []
  courses.value = c.data || []
  classes.value = cl.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1)
  if (cur && !query.semesterId) query.semesterId = cur.id
  await loadOfferings()
}

async function loadOfferings() {
  if (!query.semesterId) {
    offerings.value = []
    return
  }
  const res = await offeringApi.page({ pageNum: 1, pageSize: 500, semesterId: query.semesterId })
  offerings.value = res.data?.records || []
}

async function onSemesterChange() {
  query.offeringId = undefined
  summary.value = null
  distribution.value = []
  await loadOfferings()
  search()
}

async function onOfferingChange() {
  if (!query.offeringId) {
    summary.value = null
    distribution.value = []
  } else {
    const [s, d] = await Promise.all([
      scoreApi.offeringSummary(query.offeringId),
      scoreApi.offeringDistribution(query.offeringId)
    ])
    summary.value = s.data
    distribution.value = d.data || []
  }
  search()
}

async function publish(all) {
  const ids = all ? null : selectedIds.value
  if (!all && !ids.length) {
    ElMessage.warning('请先勾选要发布的成绩')
    return
  }
  await ElMessageBox.confirm(
    all
      ? `确认发布「${selectedOffering?.courseName || '当前开课'}」的全部成绩吗？`
      : `确认发布选中的 ${ids.length} 条成绩吗？`,
    '发布成绩',
    { type: 'warning' }
  )
  const res = await scoreApi.publish(all ? query.offeringId : null, ids)
  if (res.code === 200) {
    ElMessage.success(`已发布 ${res.data ?? ''} 条成绩`)
    onOfferingChange()
  }
}

async function revoke() {
  const ids = selection.value.filter((r) => r.status === 2).map((r) => r.id)
  if (!ids.length) {
    ElMessage.warning('请先勾选已发布的成绩')
    return
  }
  await ElMessageBox.confirm(`确认撤回选中的 ${ids.length} 条成绩吗？`, '撤回成绩', { type: 'warning' })
  const res = await scoreApi.revoke(null, ids)
  if (res.code === 200) {
    ElMessage.success(`已撤回 ${res.data ?? ''} 条成绩`)
    onOfferingChange()
  }
}

async function doExport() {
  await scoreApi.export({ ...query })
  ElMessage.success('导出已开始，请查看浏览器下载')
}

onMounted(async () => {
  await loadOptions()
  load()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-16">
      <el-form class="search-bar" inline @submit.prevent>
        <el-form-item label="学期">
          <el-select v-model="query.semesterId" style="width: 200px" @change="onSemesterChange">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开课安排">
          <el-select
            v-model="query.offeringId"
            placeholder="全部课程"
            clearable
            filterable
            style="width: 340px"
            @change="onOfferingChange"
          >
            <el-option
              v-for="o in offerings"
              :key="o.id"
              :label="`${o.courseName} - ${o.className || '公共课'} - ${o.teacherName}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable filterable style="width: 190px" @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="学号 / 姓名 / 课程"
            clearable
            style="width: 180px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="s in SCORE_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="reset({ semesterId: query.semesterId }); onOfferingChange()">
            <el-icon><RefreshLeft /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row v-if="summary" :gutter="16" class="mb-16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>成绩概览 —— {{ selectedOffering?.courseName }}</span>
            </div>
          </template>
          <div class="score-summary">
            <div class="ss-item">
              <span class="ss-label">人数</span><span class="ss-val">{{ summary.totalCount ?? 0 }}</span>
            </div>
            <div class="ss-item">
              <span class="ss-label">平均分</span>
              <span class="ss-val" :style="{ color: scoreColor(summary.avgScore) }">{{ fixed(summary.avgScore, 2) }}</span>
            </div>
            <div class="ss-item">
              <span class="ss-label">最高分</span><span class="ss-val" style="color: #16a34a">{{ fixed(summary.maxScore, 1) }}</span>
            </div>
            <div class="ss-item">
              <span class="ss-label">最低分</span><span class="ss-val" style="color: #dc2626">{{ fixed(summary.minScore, 1) }}</span>
            </div>
            <div class="ss-item">
              <span class="ss-label">优秀</span><span class="ss-val">{{ summary.excellentCount ?? 0 }}</span>
            </div>
            <div class="ss-item">
              <span class="ss-label">及格</span><span class="ss-val">{{ summary.passCount ?? 0 }}</span>
            </div>
            <div class="ss-item">
              <span class="ss-label">不及格</span><span class="ss-val text-danger">{{ summary.failCount ?? 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <template #header><div class="card-head"><span>等级分布</span></div></template>
          <ChartBox :option="distributionOption" height="220px" :empty="!distribution.length" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :disabled="!selectedIds.length" @click="publish(false)">
          <el-icon><CircleCheck /></el-icon>发布选中
        </el-button>
        <el-button type="success" :disabled="!query.offeringId" @click="publish(true)">
          <el-icon><CircleCheckFilled /></el-icon>发布本课程全部
        </el-button>
        <el-button type="warning" plain @click="revoke">
          <el-icon><RefreshLeft /></el-icon>撤回选中
        </el-button>
        <el-button plain @click="doExport">
          <el-icon><Download /></el-icon>导出成绩
        </el-button>
        <el-checkbox
          v-model="query.onlyFail"
          :true-value="true"
          :false-value="undefined"
          label="只看不及格"
          style="margin-left: 8px"
          @change="search"
        />
        <div class="toolbar-right text-muted">共 {{ total }} 条成绩记录</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="studentNo" label="学号" width="112" />
        <el-table-column prop="studentName" label="姓名" width="95" />
        <el-table-column prop="className" label="班级" min-width="165" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程名称" min-width="165" show-overflow-tooltip />
        <el-table-column prop="credit" label="学分" width="66" align="center" />
        <el-table-column label="平时" width="72" align="center">
          <template #default="{ row }">{{ fixed(row.usualScore, 1) }}</template>
        </el-table-column>
        <el-table-column label="期末" width="72" align="center">
          <template #default="{ row }">{{ fixed(row.examScore, 1) }}</template>
        </el-table-column>
        <el-table-column label="总评" width="82" align="center">
          <template #default="{ row }">
            <b :style="{ color: scoreColor(row.totalScore) }">{{ fixed(row.totalScore, 1) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="绩点" width="72" align="center">
          <template #default="{ row }">{{ fixed(row.gradePoint, 1) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="86" align="center">
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
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="scoreStatusType(row.status)" size="small" effect="plain">
              {{ scoreStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" width="95" />
        <el-table-column prop="publishTime" label="发布时间" width="155" />
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.score-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 12px;
}
.ss-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ss-label {
  font-size: 12px;
  color: var(--aas-text-secondary);
}
.ss-val {
  font-size: 19px;
  font-weight: 600;
}
</style>
