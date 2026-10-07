<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { offeringApi, scoreApi, semesterApi } from '@/api'
import { useUserStore } from '@/store/user'
import { fixed, scoreColor, scoreStatusText, scoreStatusType } from '@/utils/format'
import { gradePointOf, levelOf } from '@/utils/dict'

const userStore = useUserStore()

const semesters = ref([])
const semesterId = ref(null)
const allOfferings = ref([])
const offeringId = ref(null)
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const summary = ref(null)
const keyword = ref('')

/** 当前可选的开课：班主任仅看本班 + 自己任课；管理员/教务处看全部 */
const offerings = computed(() => {
  let list = allOfferings.value
  if (userStore.isHeadTeacher) {
    const myClasses = userStore.manageClassIds || []
    const myTeacherId = userStore.teacherId
    list = list.filter((o) => myClasses.includes(o.classId) || o.teacherId === myTeacherId)
  }
  if (keyword.value.trim()) {
    const k = keyword.value.trim()
    list = list.filter((o) => (o.courseName || '').includes(k) || (o.className || '').includes(k))
  }
  return list
})

const currentOffering = computed(() => allOfferings.value.find((o) => o.id === offeringId.value))

/** 过滤后的成绩行 */
const filteredRows = computed(() => {
  const k = keyword.value.trim()
  if (!k) return rows.value
  return rows.value.filter(
    (r) => (r.studentName || '').includes(k) || (r.studentNo || '').includes(k)
  )
})

/** 未保存提示 */
const dirty = ref(false)

async function loadSemesters() {
  const res = await semesterApi.list()
  semesters.value = res.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1) || semesters.value[0]
  semesterId.value = cur?.id || null
}

async function loadOfferings() {
  if (!semesterId.value) return
  const res = await offeringApi.page({ pageNum: 1, pageSize: 500, semesterId: semesterId.value })
  allOfferings.value = res.data?.records || []
  offeringId.value = offerings.value[0]?.id || null
  await loadScores()
}

async function loadScores() {
  if (!offeringId.value) {
    rows.value = []
    summary.value = null
    return
  }
  loading.value = true
  try {
    const [s, sum] = await Promise.all([
      scoreApi.offeringScores(offeringId.value),
      scoreApi.offeringSummary(offeringId.value)
    ])
    rows.value = (s.data || []).map((r) => ({
      ...r,
      _total: computeTotal(r.usualScore, r.examScore)
    }))
    summary.value = sum.data
    dirty.value = false
  } finally {
    loading.value = false
  }
}

function computeTotal(usual, exam) {
  if (usual == null || usual === '' || exam == null || exam === '') return null
  return Math.round((Number(usual) * 0.3 + Number(exam) * 0.7) * 10) / 10
}

function onScoreChange(row) {
  row._total = computeTotal(row.usualScore, row.examScore)
  dirty.value = true
}

async function initScores() {
  await ElMessageBox.confirm(
    '将为该开课的所有选课学生生成成绩记录（已存在记录不会重复生成），是否继续？',
    '生成成绩单',
    { type: 'info' }
  )
  const res = await scoreApi.init(offeringId.value)
  if (res.code === 200) {
    ElMessage.success(res.data?.toString() || '生成成功')
    await loadScores()
  }
}

function validateRows() {
  for (const r of rows.value) {
    if (r.usualScore != null && (r.usualScore < 0 || r.usualScore > 100)) {
      ElMessage.warning(`${r.studentName} 的平时成绩须在 0-100 之间`)
      return false
    }
    if (r.examScore != null && (r.examScore < 0 || r.examScore > 100)) {
      ElMessage.warning(`${r.studentName} 的期末成绩须在 0-100 之间`)
      return false
    }
  }
  return true
}

async function save() {
  if (!validateRows()) return
  saving.value = true
  try {
    const payload = rows.value.map((r) => ({
      id: r.id,
      studentId: r.studentId,
      offeringId: r.offeringId,
      usualScore: r.usualScore,
      examScore: r.examScore,
      isRetake: r.isRetake,
      remark: r.remark
    }))
    const res = await scoreApi.saveBatch(offeringId.value, payload)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      await loadScores()
    }
  } finally {
    saving.value = false
  }
}

async function publish(all) {
  const ids = all ? null : rows.value.filter((r) => r._checked).map((r) => r.id)
  if (!all && !ids.length) {
    ElMessage.warning('请先勾选要发布的成绩')
    return
  }
  await ElMessageBox.confirm(
    all ? '确认发布该开课的全部成绩吗？发布后学生即可查询。' : `确认发布选中的 ${ids.length} 条成绩吗？`,
    '发布成绩',
    { type: 'warning' }
  )
  const res = await scoreApi.publish(offeringId.value, ids)
  if (res.code === 200) {
    ElMessage.success('发布成功')
    await loadScores()
  }
}

async function revoke() {
  const ids = rows.value.filter((r) => r._checked && r.status === 2).map((r) => r.id)
  if (!ids.length) {
    ElMessage.warning('请先勾选要撤回的已发布成绩')
    return
  }
  await ElMessageBox.confirm(`确认撤回选中的 ${ids.length} 条成绩吗？`, '撤回成绩', { type: 'warning' })
  const res = await scoreApi.revoke(offeringId.value, ids)
  if (res.code === 200) {
    ElMessage.success('撤回成功')
    await loadScores()
  }
}

function toggleAll(checked) {
  rows.value.forEach((r) => {
    r._checked = checked
  })
}

onMounted(async () => {
  await loadSemesters()
  await loadOfferings()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-16">
      <el-form inline @submit.prevent>
        <el-form-item label="学期">
          <el-select v-model="semesterId" style="width: 210px" @change="loadOfferings">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开课安排">
          <el-select
            v-model="offeringId"
            filterable
            placeholder="请选择要录入成绩的课程"
            style="width: 380px"
            @change="loadScores"
          >
            <el-option
              v-for="o in offerings"
              :key="o.id"
              :label="`${o.courseName} - ${o.className || '公共课'} - ${o.teacherName}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadScores">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="offeringId" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <span>{{ currentOffering?.courseName }}</span>
            <span class="text-muted" style="margin-left: 10px; font-size: 13px">
              {{ currentOffering?.className || '公共选修' }} · {{ currentOffering?.teacherName }} ·
              {{ currentOffering?.credit }} 学分
            </span>
          </div>
          <div class="head-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索学号 / 姓名"
              clearable
              size="small"
              style="width: 170px"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button v-if="!rows.length" type="info" plain size="small" @click="initScores">
              <el-icon><Plus /></el-icon>生成成绩单
            </el-button>
            <el-button size="small" @click="toggleAll(true)">全选</el-button>
            <el-button size="small" @click="toggleAll(false)">取消全选</el-button>
            <el-button type="primary" size="small" :loading="saving" @click="save">
              <el-icon><Check /></el-icon>保存成绩
            </el-button>
            <el-button type="success" size="small" @click="publish(false)">发布选中</el-button>
            <el-button type="success" plain size="small" @click="publish(true)">发布全部</el-button>
            <el-button type="warning" plain size="small" @click="revoke">撤回</el-button>
          </div>
        </div>
      </template>

      <!-- 统计 -->
      <div v-if="summary" class="score-summary">
        <div class="ss-item">
          <span class="ss-label">总人数</span>
          <span class="ss-val">{{ summary.totalCount ?? 0 }}</span>
        </div>
        <div class="ss-item">
          <span class="ss-label">平均分</span>
          <span class="ss-val" :style="{ color: scoreColor(summary.avgScore) }">
            {{ fixed(summary.avgScore, 2) }}
          </span>
        </div>
        <div class="ss-item">
          <span class="ss-label">最高分</span>
          <span class="ss-val" style="color: #16a34a">{{ fixed(summary.maxScore, 1) }}</span>
        </div>
        <div class="ss-item">
          <span class="ss-label">最低分</span>
          <span class="ss-val" style="color: #dc2626">{{ fixed(summary.minScore, 1) }}</span>
        </div>
        <div class="ss-item">
          <span class="ss-label">优秀</span>
          <span class="ss-val">{{ summary.excellentCount ?? 0 }}</span>
        </div>
        <div class="ss-item">
          <span class="ss-label">及格</span>
          <span class="ss-val">{{ summary.passCount ?? 0 }}</span>
        </div>
        <div class="ss-item">
          <span class="ss-label">不及格</span>
          <span class="ss-val text-danger">{{ summary.failCount ?? 0 }}</span>
        </div>
      </div>

      <el-alert v-if="dirty" type="warning" :closable="false" show-icon class="mb-16">
        <template #title>有未保存的成绩修改，请点击「保存成绩」提交。</template>
      </el-alert>

      <el-table v-loading="loading" :data="filteredRows" stripe border height="520">
        <el-table-column label="#" width="56" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" min-width="165" show-overflow-tooltip />
        <el-table-column label="平时(30%)" width="130" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.usualScore"
              :min="0"
              :max="100"
              :precision="1"
              :step="1"
              size="small"
              controls-position="right"
              style="width: 100%"
              @change="onScoreChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="期末(70%)" width="130" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.examScore"
              :min="0"
              :max="100"
              :precision="1"
              :step="1"
              size="small"
              controls-position="right"
              style="width: 100%"
              @change="onScoreChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="总评" width="90" align="center">
          <template #default="{ row }">
            <b :style="{ color: scoreColor(row._total) }">{{ fixed(row._total, 1) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="绩点" width="80" align="center">
          <template #default="{ row }">{{ fixed(gradePointOf(row._total), 1) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <span :style="{ color: scoreColor(row._total) }">{{ levelOf(row._total) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="scoreStatusType(row.status)" size="small" effect="plain">
              {{ scoreStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选择" width="70" align="center" fixed="right">
          <template #default="{ row }">
            <el-checkbox v-model="row._checked" />
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !rows.length" class="empty-tip">
        <el-empty description="该开课暂无成绩记录" :image-size="90" />
        <el-button type="primary" @click="initScores">生成成绩单</el-button>
      </div>
    </el-card>

    <el-empty v-else description="请先选择要录入成绩的开课安排" :image-size="120" />
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.score-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
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
.empty-tip {
  text-align: center;
  padding-bottom: 12px;
}
</style>
