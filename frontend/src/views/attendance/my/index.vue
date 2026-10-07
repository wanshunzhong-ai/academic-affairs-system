<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { attendanceApi, leaveApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import ChartBox from '@/components/ChartBox.vue'
import { leaveStatusText, leaveStatusType } from '@/utils/format'
import { LEAVE_STATUS_OPTIONS, LEAVE_TYPE_OPTIONS } from '@/utils/dict'

const userStore = useUserStore()

const {
  loading,
  list,
  total,
  query,
  load,
  search,
  reset,
  onPageChange,
  onSizeChange
} = useTable(leaveApi.page, { status: undefined, leaveType: undefined })

/* ==================== 考勤汇总 ==================== */
const summaryLoading = ref(false)
const attendance = ref({ total: 0, abnormal: 0, details: [] })

const ATTEND_COLORS = ['#16a34a', '#d97706', '#f59e0b', '#dc2626', '#64748b']

const attendanceOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12 } },
  color: ATTEND_COLORS,
  series: [
    {
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '44%'],
      label: { show: true, formatter: '{b}\n{c}', fontSize: 11, color: '#475569' },
      labelLine: { length: 8, length2: 8 },
      data: attendance.value.details || []
    }
  ]
}))

async function loadAttendance() {
  summaryLoading.value = true
  try {
    const res = await attendanceApi.my()
    attendance.value = res.data || { total: 0, abnormal: 0, details: [] }
  } finally {
    summaryLoading.value = false
  }
}

/* ==================== 申请请假 ==================== */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  leaveType: '事假',
  range: [],
  reason: ''
})

const rules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  reason: [
    { required: true, message: '请填写请假事由', trigger: 'blur' },
    { min: 4, message: '请假事由至少 4 个字', trigger: 'blur' }
  ]
}

/** 预计请假天数（保留 0.5 天） */
const estimatedDays = computed(() => {
  const [s, e] = form.range || []
  if (!s || !e) return 0
  const diff = (new Date(e).getTime() - new Date(s).getTime()) / 86400000
  if (diff < 0) return 0
  return Math.round(diff * 2) / 2
})

function openApply() {
  form.leaveType = '事假'
  form.range = []
  form.reason = ''
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  const [s, e] = form.range || []
  if (!s || !e) {
    ElMessage.warning('请选择请假起止时间')
    return
  }
  if (new Date(e) <= new Date(s)) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  submitting.value = true
  try {
    const res = await leaveApi.apply({
      leaveType: form.leaveType,
      startDate: s,
      endDate: e,
      reason: form.reason
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '申请已提交')
      dialogVisible.value = false
      userStore.refreshBadges()
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function cancel(row) {
  await ElMessageBox.confirm('确认撤销这条请假申请吗？撤销后需重新提交。', '撤销确认', { type: 'warning' })
  const res = await leaveApi.cancel(row.id)
  if (res.code === 200) {
    ElMessage.success('已撤销')
    load()
  }
}

const detailVisible = ref(false)
const detail = ref(null)

async function openDetail(row) {
  const res = await leaveApi.detail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(() => {
  loadAttendance()
  load()
})
</script>

<template>
  <div class="app-container">
    <el-row :gutter="16" class="mb-16">
      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="mb-16">
          <template #header><div class="card-head"><span>我的考勤概况</span></div></template>
          <div class="attend-stat">
            <div class="as-item">
              <div class="as-val">{{ attendance.total ?? 0 }}</div>
              <div class="as-label">考勤记录</div>
            </div>
            <div class="as-item">
              <div class="as-val text-danger">{{ attendance.abnormal ?? 0 }}</div>
              <div class="as-label">异常次数</div>
            </div>
          </div>
          <ChartBox
            :option="attendanceOption"
            height="240px"
            :loading="summaryLoading"
            :empty="!(attendance.details || []).length"
            empty-text="暂无考勤记录"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>我的请假记录</span>
              <el-button type="primary" size="small" @click="openApply">
                <el-icon><Plus /></el-icon>提交请假申请
              </el-button>
            </div>
          </template>

          <el-form class="search-bar" inline @submit.prevent>
            <el-form-item label="请假类型">
              <el-select v-model="query.leaveType" placeholder="全部" clearable style="width: 120px" @change="search">
                <el-option v-for="t in LEAVE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px" @change="search">
                <el-option v-for="s in LEAVE_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="search">
                <el-icon><Search /></el-icon>查询
              </el-button>
              <el-button @click="reset()">
                <el-icon><RefreshLeft /></el-icon>重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loading" :data="list" stripe border>
            <el-table-column type="index" label="#" width="56" align="center" />
            <el-table-column prop="leaveType" label="类型" width="85" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.leaveType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="请假时间" min-width="230">
              <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
            </el-table-column>
            <el-table-column label="天数" width="80" align="center">
              <template #default="{ row }">{{ row.days }} 天</template>
            </el-table-column>
            <el-table-column prop="reason" label="事由" min-width="170" show-overflow-tooltip />
            <el-table-column label="状态" width="95" align="center">
              <template #default="{ row }">
                <el-tag :type="leaveStatusType(row.status)" size="small" effect="plain">
                  {{ leaveStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审批人" width="90">
              <template #default="{ row }">{{ row.approverName || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="135" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 0" link type="warning" @click="cancel(row)">撤销</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              :current-page="query.pageNum"
              :page-size="query.pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="onPageChange"
              @size-change="onSizeChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 申请请假 -->
    <el-dialog v-model="dialogVisible" title="提交请假申请" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="请假类型" prop="leaveType">
          <el-radio-group v-model="form.leaveType">
            <el-radio-button v-for="t in LEAVE_TYPE_OPTIONS" :key="t.value" :value="t.value">
              {{ t.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="请假时间" required>
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="~"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预计天数">
          <el-tag type="info" effect="plain">{{ estimatedDays }} 天</el-tag>
          <span class="text-muted" style="margin-left: 10px; font-size: 12px">按 0.5 天为单位自动计算</span>
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请详细说明请假原因，便于班主任审批"
          />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon>
        <template #title>提交后将由班主任审批，审批结果可在「我的请假」中查看。</template>
      </el-alert>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="请假申请详情" size="460px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="请假类型">{{ detail.leaveType }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.endDate }}</el-descriptions-item>
          <el-descriptions-item label="请假天数">{{ detail.days }} 天</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="请假事由">{{ detail.reason }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="leaveStatusType(detail.status)" size="small" effect="plain">
              {{ leaveStatusText(detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审批人">{{ detail.approverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ detail.approveTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批意见">{{ detail.approveRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.attend-stat {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 8px;
}
.as-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}
.as-val {
  font-size: 24px;
  font-weight: 600;
}
.as-label {
  font-size: 12px;
  color: var(--aas-text-secondary);
  margin-top: 4px;
}
</style>
