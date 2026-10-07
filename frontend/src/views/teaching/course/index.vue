<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, deptApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { COURSE_TYPE_OPTIONS, EXAM_TYPE_OPTIONS } from '@/utils/dict'

const { loading, list, total, query, load, search, reset, onPageChange, onSizeChange, refreshAfterRemove } =
  useTable(courseApi.page, { keyword: '', deptId: undefined, courseType: undefined, status: undefined })

const depts = ref([])

async function loadDepts() {
  const res = await deptApi.options()
  depts.value = res.data || []
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  courseCode: '',
  courseName: '',
  deptId: null,
  credit: 2,
  hours: 32,
  courseType: '必修',
  examType: '考试',
  description: '',
  status: 1
})

const rules = {
  courseCode: [{ required: true, message: '请输入课程编码', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择开课院系', trigger: 'change' }],
  credit: [{ required: true, message: '请输入学分', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增课程'
  Object.assign(form, {
    id: null,
    courseCode: '',
    courseName: '',
    deptId: null,
    credit: 2,
    hours: 32,
    courseType: '必修',
    examType: '考试',
    description: '',
    status: 1
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑课程'
  const res = await courseApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await courseApi.update({ ...form }) : await courseApi.create({ ...form })
    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除课程「${row.courseName}」吗？`, '删除确认', { type: 'warning' })
  const res = await courseApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

function typeTag(t) {
  if (t === '必修') return 'danger'
  if (t === '选修') return 'warning'
  return 'info'
}

onMounted(() => {
  loadDepts()
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
            placeholder="课程编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="开课院系">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 170px" @change="search">
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程性质">
          <el-select v-model="query.courseType" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="t in COURSE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
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
          <el-icon><Plus /></el-icon>新增课程
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 门课程</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="courseCode" label="课程编码" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="190" show-overflow-tooltip />
        <el-table-column label="开课院系" min-width="140">
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="credit" label="学分" width="80" align="center" />
        <el-table-column prop="hours" label="学时" width="80" align="center" />
        <el-table-column label="课程性质" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.courseType)" size="small" effect="plain">{{ row.courseType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="examType" label="考核方式" width="100" align="center" />
        <el-table-column prop="description" label="课程简介" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="课程编码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="如 C0001" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="如 高等数学(上)" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开课院系" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择院系" style="width: 100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="学分" prop="credit">
              <el-input-number v-model="form.credit" :min="0.5" :max="20" :step="0.5" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学时">
              <el-input-number v-model="form.hours" :min="8" :max="300" :step="8" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="课程性质">
              <el-select v-model="form.courseType" style="width: 100%">
                <el-option v-for="t in COURSE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核方式">
              <el-select v-model="form.examType" style="width: 100%">
                <el-option v-for="t in EXAM_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
