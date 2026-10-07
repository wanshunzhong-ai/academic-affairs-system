<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, leaveApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { leaveStatusText, leaveStatusType } from '@/utils/format'
import { LEAVE_STATUS_OPTIONS, LEAVE_TYPE_OPTIONS } from '@/utils/dict'

const userStore = useUserStore()

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
} = useTable(leaveApi.page, {
  keyword: '',
  classId: undefined,
  status: 0,
  leaveType: undefined,
  startDate: undefined,
  endDate: undefined
})

const classes = ref([])
const dateRange = ref([])

async function loadOptions() {
  const res = await classApi.options()
  let all = res.data || []
  if (userStore.isHeadTeacher) {
    const mine = userStore.manageClassIds || []
    if (mine.length) all = all.filter((c) => mine.includes(c.id))
  }
  classes.value = all
}

function onDateRangeChange(val) {
  query.startDate = val?.[0] || undefined
  query.endDate = val?.[1] || undefined
  search()
}

/* ==================== 单条审批 ==================== */
const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const approveForm = reactive({ status: 1, remark: '' })

function openApprove(row) {
  current.value = row
  approveForm.status = 1
  approveForm.remark = ''
  dialogVisible.value = true
}

async function submitApprove() {
  submitting.value = true
  try {
    const res = await leaveApi.approve(current.value.id, approveForm.status, approveForm.remark)
    if (res.code === 200) {
      ElMessage.success(approveForm.status === 1 ? '已通过' : '已驳回')
      dialogVisible.value = false
      userStore.refreshBadges()
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function quickApprove(row, status) {
  const label = status === 1 ? '通过' : '驳回'
  if (status === 2) {
    const { value } = await ElMessageBox.prompt('请输入驳回理由', '驳回申请', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '如：事由不充分，请补充说明',
      inputValidator: (v) => (v && v.trim().length >= 2 ? true : '请填写驳回理由（至少 2 个字）')
    })
    const res = await leaveApi.approve(row.id, 2, value)
    if (res.code === 200) {
      ElMessage.success('已驳回')
      userStore.refreshBadges()
      load()
    }
    return
  }
  await ElMessageBox.confirm(
    `确认通过「${row.studentName}」的${row.leaveType}申请（${row.days} 天）吗？`,
    '审批确认',
    { type: 'info' }
  )
  const res = await leaveApi.approve(row.id, status, `审批${label}`)
  if (res.code === 200) {
    ElMessage.success('已通过')
    userStore.refreshBadges()
    load()
  }
}

async function batchApprove(status) {
  const ids = selection.value.filter((r) => r.status === 0).map((r) => r.id)
  if (!ids.length) {
    ElMessage.warning('请先勾选待审批的申请')
    return
  }
  let remark = ''
  if (status === 2) {
    const { value } = await ElMessageBox.prompt('请输入批量驳回理由', '批量驳回', {
      inputValidator: (v) => (v && v.trim().length >= 2 ? true : '请填写驳回理由（至少 2 个字）')
    })
    remark = value
  } else {
    await ElMessageBox.confirm(`确认批量通过选中的 ${ids.length} 条请假申请吗？`, '批量审批', { type: 'info' })
    remark = '批量审批通过'
  }
  const res = await leaveApi.batchApprove(ids, status, remark)
  if (res.code === 200) {
    ElMessage.success('审批完成')
    userStore.refreshBadges()
    load()
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.studentName}」的这条请假记录吗？`, '删除确认', { type: 'warning' })
  const res = await leaveApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
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

onMounted(async () => {
  await loadOptions()
  load()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form class="search-bar" inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="学号 / 姓名"
            clearable
            style="width: 180px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable filterable style="width: 200px" @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
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
        <el-form-item label="申请日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 240px"
            @change="onDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="dateRange = []; reset()">
            <el-icon><RefreshLeft /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="success" @click="batchApprove(1)">
          <el-icon><CircleCheck /></el-icon>批量通过
        </el-button>
        <el-button type="danger" plain @click="batchApprove(2)">
          <el-icon><CircleClose /></el-icon>批量驳回
        </el-button>
        <el-button plain @click="query.status = 0; search()">
          <el-icon><Clock /></el-icon>只看待审批
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 条申请</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" :selectable="(row) => row.status === 0" />
        <el-table-column prop="studentNo" label="学号" width="112" />
        <el-table-column prop="studentName" label="姓名" width="95" />
        <el-table-column prop="className" label="班级" min-width="165" show-overflow-tooltip />
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
        <el-table-column prop="reason" label="请假事由" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="leaveStatusType(row.status)" size="small" effect="plain">
              {{ leaveStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" width="95">
          <template #default="{ row }">{{ row.approverName || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 0">
              <el-button link type="success" @click="quickApprove(row, 1)">通过</el-button>
              <el-button link type="danger" @click="quickApprove(row, 2)">驳回</el-button>
            </template>
            <el-button v-else link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 审批弹窗 -->
    <el-dialog v-model="dialogVisible" title="请假审批" width="520px" destroy-on-close>
      <el-descriptions v-if="current" :column="1" border size="small" class="mb-16">
        <el-descriptions-item label="学生">
          {{ current.studentName }}（{{ current.studentNo }}）· {{ current.className }}
        </el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.leaveType }}</el-descriptions-item>
        <el-descriptions-item label="时间">
          {{ current.startDate }} ~ {{ current.endDate }}（{{ current.days }} 天）
        </el-descriptions-item>
        <el-descriptions-item label="事由">{{ current.reason }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.remark" type="textarea" :rows="3" maxlength="120" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApprove">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="请假申请详情" size="480px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ detail.className }}</el-descriptions-item>
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
