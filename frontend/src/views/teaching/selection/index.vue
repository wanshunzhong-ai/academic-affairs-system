<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, courseApi, offeringApi, selectionApi, semesterApi, studentApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { SELECT_TYPE_TEXT } from '@/utils/dict'

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
} = useTable(selectionApi.page, {
  keyword: '',
  semesterId: undefined,
  classId: undefined,
  courseId: undefined,
  offeringId: undefined,
  status: undefined
})

const semesters = ref([])
const courses = ref([])
const classes = ref([])

async function loadOptions() {
  const [s, c, cl] = await Promise.all([semesterApi.list(), courseApi.options(), classApi.options()])
  semesters.value = s.data || []
  courses.value = c.data || []
  classes.value = cl.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1)
  if (cur && !query.semesterId) query.semesterId = cur.id
}

async function doExport() {
  await selectionApi.export({ ...query })
  ElMessage.success('导出已开始，请查看浏览器下载')
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确认删除「${row.studentName}」的《${row.courseName}》选课记录吗？`,
    '删除确认',
    { type: 'warning' }
  )
  const res = await selectionApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

/* ==================== 批量分配选课 ==================== */
const assignVisible = ref(false)
const assigning = ref(false)
const assignForm = reactive({ offeringId: null, studentIds: [] })
const offerings = ref([])
const students = ref([])
const studentKeyword = ref('')

async function loadOfferings() {
  const res = await offeringApi.page({ pageNum: 1, pageSize: 500, semesterId: query.semesterId })
  offerings.value = res.data?.records || []
}

async function loadStudents(kw) {
  const res = await studentApi.options(kw)
  students.value = res.data || []
}

async function openAssign() {
  assignForm.offeringId = null
  assignForm.studentIds = []
  studentKeyword.value = ''
  await Promise.all([loadOfferings(), loadStudents('')])
  assignVisible.value = true
}

async function submitAssign() {
  if (!assignForm.offeringId) {
    ElMessage.warning('请选择要分配的开课安排')
    return
  }
  if (!assignForm.studentIds.length) {
    ElMessage.warning('请选择要分配的学生')
    return
  }
  assigning.value = true
  try {
    const res = await selectionApi.assign(assignForm.offeringId, assignForm.studentIds)
    if (res.code === 200) {
      ElMessage.success(`已为 ${assignForm.studentIds.length} 名学生分配课程`)
      assignVisible.value = false
      load()
    }
  } finally {
    assigning.value = false
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
            placeholder="学号 / 姓名 / 课程名"
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
        <el-form-item label="课程">
          <el-select v-model="query.courseId" placeholder="全部课程" clearable filterable style="width: 190px" @change="search">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部班级" clearable filterable style="width: 200px" @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
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
        <el-button type="primary" @click="openAssign">
          <el-icon><Plus /></el-icon>批量分配选课
        </el-button>
        <el-button plain @click="doExport">
          <el-icon><Download /></el-icon>导出名单
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 条选课记录</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" min-width="170" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="credit" label="学分" width="70" align="center" />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column label="选课方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.selectType === 2 ? 'info' : 'success'" size="small" effect="plain">
              {{ SELECT_TYPE_TEXT[row.selectType] || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="selectTime" label="选课时间" width="160" />
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

    <el-dialog v-model="assignVisible" title="批量分配选课" width="680px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="开课安排" required>
          <el-select v-model="assignForm.offeringId" placeholder="选择开课安排" filterable style="width: 100%">
            <el-option
              v-for="o in offerings"
              :key="o.id"
              :label="`${o.courseName} - ${o.className || '公共课'} - ${o.teacherName}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学生" required>
          <el-select
            v-model="assignForm.studentIds"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="loadStudents"
            placeholder="输入学号或姓名搜索，可多选"
            style="width: 100%"
          >
            <el-option
              v-for="s in students"
              :key="s.id"
              :label="`${s.name}（${s.studentNo}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          已选择 {{ assignForm.studentIds.length }} 名学生。系统会自动校验课程容量与时间冲突，冲突的学生将被跳过。
        </template>
      </el-alert>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">确定分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>
