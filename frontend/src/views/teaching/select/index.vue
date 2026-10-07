<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { offeringApi, selectionApi, semesterApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { fixed } from '@/utils/format'

const loadingMine = ref(false)
const mine = ref([])
const credit = ref({ credit: 0, maxCredit: 30 })
const semesters = ref([])
const semesterId = ref(null)
const keyword = ref('')
const currentSemester = computed(() => semesters.value.find((s) => s.id === semesterId.value))

const { loading, list, total, query, search, onPageChange, onSizeChange } = useTable(
  offeringApi.selectable,
  { semesterId: undefined, keyword: '' }
)

/** 已选课程的 offeringId 集合，用于按钮态 */
const selectedIds = computed(() => new Set(mine.value.map((i) => i.offeringId)))

/** 学分进度 */
const creditPercent = computed(() => {
  const max = credit.value.maxCredit || 30
  return Math.min(100, Math.round(((credit.value.credit || 0) / max) * 100))
})
const creditFull = computed(() => (credit.value.credit || 0) >= (credit.value.maxCredit || 30))

async function loadSemesters() {
  const res = await semesterApi.list()
  semesters.value = res.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1) || semesters.value[0]
  semesterId.value = cur?.id || null
  query.semesterId = semesterId.value
}

async function loadMine() {
  if (!semesterId.value) return
  loadingMine.value = true
  try {
    const [m, c] = await Promise.all([
      selectionApi.my(semesterId.value),
      selectionApi.myCredit(semesterId.value)
    ])
    mine.value = m.data || []
    credit.value = c.data || { credit: 0, maxCredit: 30 }
  } finally {
    loadingMine.value = false
  }
}

async function selectCourse(row) {
  if (selectedIds.value.has(row.id)) return
  await ElMessageBox.confirm(
    `确认选择《${row.courseName}》吗？该课程 ${row.credit} 学分，授课教师 ${row.teacherName}。`,
    '选课确认',
    { type: 'info' }
  )
  const res = await selectionApi.select(row.id)
  if (res.code === 200) {
    ElMessage.success('选课成功')
    await loadMine()
    search()
  }
}

async function dropCourse(row) {
  await ElMessageBox.confirm(`确认退选《${row.courseName}》吗？`, '退课确认', { type: 'warning' })
  const res = await selectionApi.drop(row.offeringId)
  if (res.code === 200) {
    ElMessage.success('退课成功')
    await loadMine()
    search()
  }
}

const mineColumns = [
  { prop: 'courseCode', label: '课程编码', width: 100 },
  { prop: 'courseName', label: '课程名称', minWidth: 160 },
  { prop: 'credit', label: '学分', width: 70, align: 'center' },
  { prop: 'courseType', label: '性质', width: 80, align: 'center' },
  { prop: 'teacherName', label: '教师', width: 90 },
  { prop: 'selectTime', label: '选课时间', width: 155 }
]

onMounted(async () => {
  await loadSemesters()
  await loadMine()
  search()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-16">
      <div class="credit-bar">
        <div class="cb-left">
          <div class="cb-label">
            当前学期：<b>{{ currentSemester?.semesterName || '-' }}</b>
          </div>
          <div class="cb-tip text-muted">
            选课时间：{{ currentSemester?.selectStart || '-' }} ~ {{ currentSemester?.selectEnd || '-' }}
          </div>
        </div>
        <div class="cb-right">
          <el-progress
            :percentage="creditPercent"
            :stroke-width="12"
            :color="creditFull ? '#dc2626' : '#2563eb'"
            style="width: 260px"
          />
          <div class="cb-credit">
            已选学分 <b :class="{ 'text-danger': creditFull }">{{ fixed(credit.credit, 1) }}</b>
            / {{ credit.maxCredit }}
          </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>可选课程</span>
              <div class="head-search">
                <el-input
                  v-model="keyword"
                  placeholder="课程名 / 教师"
                  clearable
                  style="width: 200px"
                  @keyup.enter="query.keyword = keyword; search()"
                  @clear="query.keyword = ''; search()"
                >
                  <template #append>
                    <el-button @click="query.keyword = keyword; search()">
                      <el-icon><Search /></el-icon>
                    </el-button>
                  </template>
                </el-input>
              </div>
            </div>
          </template>

          <el-table v-loading="loading" :data="list" stripe border>
            <el-table-column prop="courseName" label="课程名称" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.courseName }}
                <el-tag v-if="row.isPublic === 1" type="warning" size="small" effect="plain" style="margin-left: 6px">
                  公共课
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="teacherName" label="教师" width="90" />
            <el-table-column prop="credit" label="学分" width="70" align="center" />
            <el-table-column label="余量" width="90" align="center">
              <template #default="{ row }">
                <span :class="{ 'text-danger': row.capacity - row.selectedCount <= 0 }">
                  {{ Math.max(0, row.capacity - row.selectedCount) }} / {{ row.capacity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="scheduleText" label="上课时间地点" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="selectedIds.has(row.id)"
                  link
                  type="success"
                  disabled
                >已选</el-button>
                <el-button
                  v-else-if="row.capacity - row.selectedCount <= 0"
                  link
                  disabled
                >已满</el-button>
                <el-button v-else link type="primary" @click="selectCourse(row)">选课</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              :current-page="query.pageNum"
              :page-size="query.pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, prev, pager, next"
              background
              @current-change="onPageChange"
              @size-change="onSizeChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>我的选课（{{ mine.length }} 门）</span>
            </div>
          </template>
          <el-table v-loading="loadingMine" :data="mine" stripe border size="small" max-height="520">
            <el-table-column
              v-for="col in mineColumns"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              :width="col.width"
              :min-width="col.minWidth"
              :align="col.align || 'left'"
              show-overflow-tooltip
            />
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="danger"
                  :disabled="row.courseType === '必修'"
                  @click="dropCourse(row)"
                >
                  {{ row.courseType === '必修' ? '不可退' : '退选' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!mine.length" class="text-muted" style="text-align: center; padding: 20px 0">
            本学期还未选课
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.credit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.cb-label {
  font-size: 15px;
}
.cb-tip {
  font-size: 12.5px;
  margin-top: 6px;
}
.cb-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.cb-credit {
  font-size: 13px;
  color: var(--aas-text-secondary);
}
.cb-credit b {
  font-size: 18px;
  color: var(--aas-primary);
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
