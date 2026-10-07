<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { attendanceApi, classApi, offeringApi, semesterApi, studentApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { attendTypeTag, attendTypeText } from '@/utils/format'
import { ATTEND_TYPE_OPTIONS } from '@/utils/dict'

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
} = useTable(attendanceApi.page, {
  keyword: '',
  classId: undefined,
  offeringId: undefined,
  attendType: undefined,
  startDate: undefined,
  endDate: undefined
})

const semesters = ref([])
const classes = ref([])
const offerings = ref([])
const dateRange = ref([])

const visibleOfferings = computed(() => {
  if (!userStore.isHeadTeacher) return offerings.value
  const myClasses = userStore.manageClassIds || []
  return offerings.value.filter((o) => myClasses.includes(o.classId) || o.teacherId === userStore.teacherId)
})

async function loadOptions() {
  const [s, c] = await Promise.all([semesterApi.list(), classApi.options()])
  semesters.value = s.data || []
  classes.value = c.data || []
  await loadOfferings()
}

async function loadOfferings() {
  const cur = semesters.value.find((i) => i.isCurrent === 1) || semesters.value[0]
  if (!cur) return
  const res = await offeringApi.page({ pageNum: 1, pageSize: 500, semesterId: cur.id })
  offerings.value = res.data?.records || []
}

function onDateRangeChange(val) {
  query.startDate = val?.[0] || undefined
  query.endDate = val?.[1] || undefined
  search()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确认删除「${row.studentName}」在 ${row.attendDate} 的考勤记录吗？`,
    '删除确认',
    { type: 'warning' }
  )
  const res = await attendanceApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    load()
  }
}

async function removeBatch() {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条考勤记录吗？`, '批量删除', {
    type: 'warning'
  })
  const res = await attendanceApi.removeBatch(selectedIds.value)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    selection.value = []
    load()
  }
}

/* ==================== 点名 ==================== */
const rollVisible = ref(false)
const rollForm = reactive({ offeringId: null, attendDate: new Date().toISOString().slice(0, 10) })
const rollRows = ref([])
const rolling = ref(false)
const loadingDraft = ref(false)

function openRoll() {
  rollForm.offeringId = visibleOfferings.value[0]?.id || null
  rollForm.attendDate = new Date().toISOString().slice(0, 10)
  rollRows.value = []
  rollVisible.value = true
  if (rollForm.offeringId) loadDraft()
}

async function loadDraft() {
  if (!rollForm.offeringId) {
    rollRows.value = []
    return
  }
  loadingDraft.value = true
  try {
    const res = await attendanceApi.draft(rollForm.offeringId)
    rollRows.value = (res.data || []).map((r) => ({ ...r, attendType: r.attendType || 1, remark: '' }))
  } finally {
    loadingDraft.value = false
  }
}

function setAll(type) {
  rollRows.value.forEach((r) => {
    r.attendType = type
  })
}

async function submitRoll() {
  if (!rollForm.offeringId) {
    ElMessage.warning('请选择上课课程')
    return
  }
  if (!rollRows.value.length) {
    ElMessage.warning('没有可点名的学生')
    return
  }
  rolling.value = true
  try {
    const payload = rollRows.value.map((r) => ({
      studentId: r.studentId,
      attendType: r.attendType,
      remark: r.remark
    }))
    const res = await attendanceApi.batchRecord(rollForm.offeringId, rollForm.attendDate, payload)
    if (res.code === 200) {
      ElMessage.success(res.message || '点名已保存')
      rollVisible.value = false
      load()
    }
  } finally {
    rolling.value = false
  }
}

const rollSummary = computed(() => {
  const map = {}
  rollRows.value.forEach((r) => {
    map[r.attendType] = (map[r.attendType] || 0) + 1
  })
  return ATTEND_TYPE_OPTIONS.filter((o) => map[o.value]).map((o) => `${o.label} ${map[o.value]}`)
})

/* ==================== 单条新增 ==================== */
const editVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive({ studentId: null, offeringId: null, attendDate: '', attendType: 1, remark: '' })
const students = ref([])

async function loadStudents(kw) {
  const res = await studentApi.options(kw)
  students.value = res.data || []
}

async function openCreate() {
  editForm.studentId = null
  editForm.offeringId = null
  editForm.attendDate = new Date().toISOString().slice(0, 10)
  editForm.attendType = 1
  editForm.remark = ''
  await loadStudents('')
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.studentId || !editForm.offeringId || !editForm.attendDate) {
    ElMessage.warning('请填写完整信息')
    return
  }
  editSubmitting.value = true
  try {
    const res = await attendanceApi.create({ ...editForm })
    if (res.code === 200) {
      ElMessage.success('新增成功')
      editVisible.value = false
      load()
    }
  } finally {
    editSubmitting.value = false
  }
}

/* ==================== 班级考勤统计 ==================== */
const statVisible = ref(false)
const statClassId = ref(null)
const statRows = ref([])
const statLoading = ref(false)

async function openStat() {
  statClassId.value = classes.value[0]?.id || null
  statVisible.value = true
  await loadStat()
}

async function loadStat() {
  if (!statClassId.value) {
    statRows.value = []
    return
  }
  statLoading.value = true
  try {
    const res = await attendanceApi.classStats(statClassId.value)
    statRows.value = res.data || []
  } finally {
    statLoading.value = false
  }
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
            placeholder="学号 / 姓名 / 课程"
            clearable
            style="width: 190px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable filterable style="width: 190px" @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="query.offeringId" placeholder="全部" clearable filterable style="width: 220px" @change="search">
            <el-option v-for="o in visibleOfferings" :key="o.id" :label="o.courseName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤类型">
          <el-select v-model="query.attendType" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="t in ATTEND_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
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
        <el-button type="primary" @click="openRoll">
          <el-icon><Finished /></el-icon>课堂点名
        </el-button>
        <el-button plain @click="openCreate">
          <el-icon><Plus /></el-icon>补录记录
        </el-button>
        <el-button plain @click="openStat">
          <el-icon><DataAnalysis /></el-icon>班级考勤统计
        </el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeBatch">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 条考勤记录</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" min-width="170" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程名称" min-width="165" show-overflow-tooltip />
        <el-table-column prop="attendDate" label="考勤日期" width="120" align="center" />
        <el-table-column label="考勤结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="attendTypeTag(row.attendType)" size="small" effect="plain">
              {{ attendTypeText(row.attendType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
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

    <!-- 课堂点名 -->
    <el-dialog v-model="rollVisible" title="课堂点名" width="820px" destroy-on-close top="6vh">
      <el-form inline>
        <el-form-item label="上课课程">
          <el-select v-model="rollForm.offeringId" filterable style="width: 340px" @change="loadDraft">
            <el-option
              v-for="o in visibleOfferings"
              :key="o.id"
              :label="`${o.courseName} - ${o.className || '公共课'} - ${o.teacherName}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上课日期">
          <el-date-picker v-model="rollForm.attendDate" type="date" value-format="YYYY-MM-DD" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button @click="loadDraft">
            <el-icon><Refresh /></el-icon>重新载入名单
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <span class="text-muted">快捷设置：</span>
        <el-button size="small" v-for="t in ATTEND_TYPE_OPTIONS" :key="t.value" @click="setAll(t.value)">
          {{ t.label }}
        </el-button>
        <div class="toolbar-right text-muted">
          <span v-for="(s, i) in rollSummary" :key="i" style="margin-left: 10px">{{ s }}</span>
        </div>
      </div>

      <el-table v-loading="loadingDraft" :data="rollRows" size="small" border max-height="380">
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column label="考勤结果" width="320">
          <template #default="{ row }">
            <el-radio-group v-model="row.attendType" size="small">
              <el-radio-button v-for="t in ATTEND_TYPE_OPTIONS" :key="t.value" :value="t.value">
                {{ t.label }}
              </el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="选填" maxlength="60" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="rollVisible = false">取消</el-button>
        <el-button type="primary" :loading="rolling" @click="submitRoll">保存点名结果</el-button>
      </template>
    </el-dialog>

    <!-- 补录 -->
    <el-dialog v-model="editVisible" title="补录考勤记录" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="学生" required>
          <el-select v-model="editForm.studentId" filterable remote :remote-method="loadStudents" placeholder="输入学号 / 姓名搜索" style="width: 100%">
            <el-option v-for="s in students" :key="s.id" :label="`${s.name}（${s.studentNo}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" required>
          <el-select v-model="editForm.offeringId" filterable placeholder="选择开课" style="width: 100%">
            <el-option
              v-for="o in visibleOfferings"
              :key="o.id"
              :label="`${o.courseName} - ${o.className || '公共课'}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤日期" required>
          <el-date-picker v-model="editForm.attendDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考勤结果">
          <el-radio-group v-model="editForm.attendType">
            <el-radio-button v-for="t in ATTEND_TYPE_OPTIONS" :key="t.value" :value="t.value">
              {{ t.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" maxlength="60" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 班级统计 -->
    <el-dialog v-model="statVisible" title="班级考勤统计" width="760px" destroy-on-close>
      <el-form inline>
        <el-form-item label="班级">
          <el-select v-model="statClassId" filterable style="width: 240px" @change="loadStat">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table v-loading="statLoading" :data="statRows" size="small" border max-height="420">
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="totalCount" label="记录数" width="90" align="center" />
        <el-table-column prop="absentCount" label="缺勤" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.absentCount > 0 }">{{ row.absentCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lateCount" label="迟到" width="80" align="center" />
        <el-table-column prop="earlyCount" label="早退" width="80" align="center" />
        <el-table-column prop="leaveCount" label="请假" width="80" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>
