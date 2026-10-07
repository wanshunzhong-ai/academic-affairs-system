<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, classroomApi, courseApi, offeringApi, selectionApi, semesterApi, teacherApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { offeringStatusText, offeringStatusType } from '@/utils/format'
import { OFFERING_STATUS_OPTIONS, SECTION_OPTIONS, WEEK_DAY_OPTIONS, WEEK_OPTIONS } from '@/utils/dict'

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
  onSizeChange,
  refreshAfterRemove
} = useTable(offeringApi.page, {
  keyword: '',
  semesterId: undefined,
  teacherId: undefined,
  classId: undefined,
  status: undefined
})

const semesters = ref([])
const courses = ref([])
const teachers = ref([])
const classes = ref([])
const classrooms = ref([])

const courseMap = computed(() => Object.fromEntries(courses.value.map((c) => [c.id, c])))
const courseFiltered = computed(() => courses.value)

async function loadOptions() {
  const [s, c, t, cl, cr] = await Promise.all([
    semesterApi.list(),
    courseApi.options(),
    teacherApi.options(),
    classApi.options(),
    classroomApi.options()
  ])
  semesters.value = s.data || []
  courses.value = c.data || []
  teachers.value = t.data || []
  classes.value = cl.data || []
  classrooms.value = cr.data || []
  // 默认筛选当前学期
  const cur = semesters.value.find((i) => i.isCurrent === 1)
  if (cur && !query.semesterId) {
    query.semesterId = cur.id
  }
}

/* ==================== 新增 / 编辑 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

const emptyOffering = () => ({
  id: null,
  offeringCode: '',
  courseId: null,
  semesterId: null,
  teacherId: null,
  classId: null,
  capacity: 60,
  isPublic: 0,
  status: 0,
  remark: ''
})

const form = reactive({ offering: emptyOffering(), schedules: [] })

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入课程容量', trigger: 'blur' }]
}

/** 所选课程信息 */
const selectedCourse = computed(() => courseMap.value[form.offering.courseId])

/** 公开课不需要指定班级 */
const needClass = computed(() => form.offering.isPublic !== 1)

function addSchedule() {
  form.schedules.push({
    weekDay: 1,
    startSection: 1,
    endSection: 2,
    startWeek: 1,
    endWeek: 16,
    classroomId: null
  })
}

function removeSchedule(index) {
  form.schedules.splice(index, 1)
}

async function openCreate() {
  dialogTitle.value = '新增开课安排'
  form.offering = emptyOffering()
  form.offering.semesterId = query.semesterId || semesters.value.find((i) => i.isCurrent === 1)?.id || null
  form.schedules = []
  addSchedule()
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑开课安排'
  const res = await offeringApi.detail(row.id)
  form.offering = { ...emptyOffering(), ...res.data }
  form.schedules = (res.data.schedules || []).map((s) => ({
    weekDay: s.weekDay,
    startSection: s.startSection,
    endSection: s.endSection,
    startWeek: s.startWeek,
    endWeek: s.endWeek,
    classroomId: s.classroomId
  }))
  if (!form.schedules.length) addSchedule()
  dialogVisible.value = true
}

function validateSchedules() {
  for (let i = 0; i < form.schedules.length; i += 1) {
    const s = form.schedules[i]
    if (!s.weekDay || !s.startSection || !s.endSection) {
      ElMessage.warning(`第 ${i + 1} 条排课信息不完整`)
      return false
    }
    if (s.startSection > s.endSection) {
      ElMessage.warning(`第 ${i + 1} 条排课的起始节次不能大于结束节次`)
      return false
    }
    if (s.startWeek > s.endWeek) {
      ElMessage.warning(`第 ${i + 1} 条排课的起始周不能大于结束周`)
      return false
    }
  }
  return true
}

async function submit() {
  await formRef.value.validate()
  if (!validateSchedules()) return
  if (needClass.value && !form.offering.classId) {
    ElMessage.warning('请选择上课班级，或将课程设为公共选修课')
    return
  }
  submitting.value = true
  try {
    const payload = {
      offering: {
        ...form.offering,
        classId: needClass.value ? form.offering.classId : null
      },
      schedules: form.schedules
    }
    const res = payload.offering.id
      ? await offeringApi.update(payload)
      : await offeringApi.create(payload)
    if (res.code === 200) {
      ElMessage.success(payload.offering.id ? '修改成功' : '开课成功')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确认删除开课「${row.courseName} - ${row.className || '公共课'}」吗？已选课学生记录将一并清除。`,
    '删除确认',
    { type: 'warning' }
  )
  const res = await offeringApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

async function removeBatch() {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条开课记录吗？`, '批量删除', {
    type: 'warning'
  })
  const res = await offeringApi.removeBatch(selectedIds.value)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    selection.value = []
    load()
  }
}

async function changeStatus(row, status) {
  const action = status === 1 ? '发布' : '结课'
  await ElMessageBox.confirm(`确认${action}「${row.courseName}」吗？`, `${action}确认`, { type: 'warning' })
  const res = await offeringApi.changeStatus(row.id, status)
  if (res.code === 200) {
    ElMessage.success(`${action}成功`)
    load()
  }
}

/* ==================== 选课统计 ==================== */
const statVisible = ref(false)
const statData = ref(null)
const statRows = ref([])

async function openStat(row) {
  const [s, r] = await Promise.all([offeringApi.stat(row.id), offeringApi.detail(row.id)])
  statData.value = { ...s.data, offering: r.data }
  const stu = await selectionApi.offeringStudents(row.id)
  statRows.value = stu.data || []
  statVisible.value = true
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
            placeholder="开课代码 / 课程名 / 教师"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="学期">
          <el-select v-model="query.semesterId" placeholder="全部学期" clearable style="width: 210px" @change="search">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师">
          <el-select v-model="query.teacherId" placeholder="全部" clearable filterable style="width: 160px" @change="search">
            <el-option v-for="t in teachers" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable filterable style="width: 200px" @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="s in OFFERING_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
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

      <div class="toolbar">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增开课
        </el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeBatch">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 条开课记录</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="offeringCode" label="开课代码" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.courseName }}
            <el-tag v-if="row.isPublic === 1" type="warning" size="small" effect="plain" style="margin-left: 6px">
              公共课
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column label="上课班级" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.className">{{ row.className }}</span>
            <span v-else class="text-muted">公共选修</span>
          </template>
        </el-table-column>
        <el-table-column label="学分" width="70" align="center">
          <template #default="{ row }">{{ row.credit }}</template>
        </el-table-column>
        <el-table-column label="选课/容量" width="110" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.selectedCount >= row.capacity }">
              {{ row.selectedCount }} / {{ row.capacity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="scheduleText" label="上课时间地点" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="offeringStatusType(row.status)" size="small" effect="plain">
              {{ offeringStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="info" @click="openStat(row)">名单</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="changeStatus(row, 1)">发布</el-button>
            <el-button v-else-if="row.status === 1" link type="warning" @click="changeStatus(row, 2)">结课</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="860px" destroy-on-close top="5vh">
      <el-form ref="formRef" :model="form.offering" :rules="rules" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="选择课程" prop="courseId">
              <el-select
                v-model="form.offering.courseId"
                placeholder="请选择课程"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="c in courseFiltered"
                  :key="c.id"
                  :label="`${c.courseName}（${c.courseCode} · ${c.credit}学分）`"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学期" prop="semesterId">
              <el-select v-model="form.offering.semesterId" placeholder="请选择学期" style="width: 100%">
                <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="授课教师" prop="teacherId">
              <el-select v-model="form.offering.teacherId" placeholder="请选择教师" filterable style="width: 100%">
                <el-option
                  v-for="t in teachers"
                  :key="t.id"
                  :label="`${t.name}（${t.teacherNo}）`"
                  :value="t.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程容量" prop="capacity">
              <el-input-number v-model="form.offering.capacity" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="课程性质">
              <el-radio-group v-model="form.offering.isPublic">
                <el-radio :value="0">班级课程</el-radio>
                <el-radio :value="1">公共选修课</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.offering.status" style="width: 100%">
                <el-option v-for="s in OFFERING_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="needClass" label="上课班级">
          <el-select v-model="form.offering.classId" placeholder="请选择班级" filterable style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedCourse" label="课程信息">
          <span class="text-muted">
            编码 {{ selectedCourse.courseCode }} · 学分 {{ selectedCourse.credit }} · 学时
            {{ selectedCourse.hours }} · {{ selectedCourse.courseType }} · {{ selectedCourse.examType }}
          </span>
        </el-form-item>

        <el-divider content-position="left">
          排课信息
          <el-button link type="primary" size="small" style="margin-left: 8px" @click="addSchedule">
            <el-icon><Plus /></el-icon>添加时间段
          </el-button>
        </el-divider>

        <el-table :data="form.schedules" size="small" border>
          <el-table-column label="星期" width="110">
            <template #default="{ row }">
              <el-select v-model="row.weekDay" size="small">
                <el-option v-for="d in WEEK_DAY_OPTIONS" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="节次" width="200">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 4px">
                <el-select v-model="row.startSection" size="small" style="width: 90px">
                  <el-option v-for="s in SECTION_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
                <span>-</span>
                <el-select v-model="row.endSection" size="small" style="width: 90px">
                  <el-option v-for="s in SECTION_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="周次范围" width="200">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 4px">
                <el-select v-model="row.startWeek" size="small" style="width: 90px">
                  <el-option v-for="w in WEEK_OPTIONS" :key="w.value" :label="w.label" :value="w.value" />
                </el-select>
                <span>-</span>
                <el-select v-model="row.endWeek" size="small" style="width: 90px">
                  <el-option v-for="w in WEEK_OPTIONS" :key="w.value" :label="w.label" :value="w.value" />
                </el-select>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="教室" min-width="170">
            <template #default="{ row }">
              <el-select v-model="row.classroomId" size="small" placeholder="选择教室" filterable clearable>
                <el-option v-for="r in classrooms" :key="r.id" :label="r.roomName" :value="r.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeSchedule($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="text-muted mt-16" style="font-size: 12px">
          保存时将自动校验教师、班级、教室在同一时间段是否冲突，如有冲突会给出提示。
        </div>

        <el-form-item label="备注" style="margin-top: 16px">
          <el-input v-model="form.offering.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 选课名单 -->
    <el-dialog v-model="statVisible" title="选课名单" width="720px" destroy-on-close>
      <template v-if="statData">
        <el-descriptions :column="4" border size="small" class="mb-16">
          <el-descriptions-item label="课程">{{ statData.offering.courseName }}</el-descriptions-item>
          <el-descriptions-item label="教师">{{ statData.offering.teacherName }}</el-descriptions-item>
          <el-descriptions-item label="容量">{{ statData.capacity }}</el-descriptions-item>
          <el-descriptions-item label="已选">
            {{ statData.selectedCount }}（{{ statData.rate }}%）
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="statRows" size="small" border max-height="380">
          <el-table-column type="index" label="#" width="56" align="center" />
          <el-table-column prop="studentNo" label="学号" width="110" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="className" label="班级" min-width="170" show-overflow-tooltip />
          <el-table-column label="选课方式" width="100" align="center">
            <template #default="{ row }">
              {{ row.selectType === 2 ? '系统分配' : '学生自选' }}
            </template>
          </el-table-column>
          <el-table-column prop="selectTime" label="选课时间" width="160" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>
