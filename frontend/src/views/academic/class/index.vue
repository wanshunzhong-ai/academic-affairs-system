<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, majorApi, teacherApi } from '@/api'
import { useTable } from '@/composables/useTable'

const { loading, list, total, query, load, search, reset, onPageChange, onSizeChange, refreshAfterRemove } =
  useTable(classApi.page, { keyword: '', majorId: undefined, status: undefined })

const majors = ref([])
const headTeachers = ref([])

const majorMap = computed(() => {
  const m = {}
  majors.value.forEach((i) => {
    m[i.id] = i
  })
  return m
})

/** 按所选院系过滤专业下拉 */
const formMajors = computed(() => {
  const row = majors.value.find((i) => i.id === form.majorId)
  if (!row) return majors.value
  return majors.value.filter((i) => i.deptId === row.deptId)
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  classCode: '',
  className: '',
  majorId: null,
  grade: String(new Date().getFullYear()),
  headTeacherId: null,
  enrollmentYear: new Date().getFullYear(),
  classroom: '',
  status: 1,
  remark: ''
})

const rules = {
  classCode: [{ required: true, message: '请输入班级编码', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  majorId: [{ required: true, message: '请选择所属专业', trigger: 'change' }]
}

async function loadOptions() {
  const [m, t] = await Promise.all([majorApi.options(), teacherApi.headTeachers()])
  majors.value = m.data || []
  headTeachers.value = t.data || []
}

function openCreate() {
  dialogTitle.value = '新增班级'
  Object.assign(form, {
    id: null,
    classCode: '',
    className: '',
    majorId: query.majorId || null,
    grade: String(new Date().getFullYear()),
    headTeacherId: null,
    enrollmentYear: new Date().getFullYear(),
    classroom: '',
    status: 1,
    remark: ''
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑班级'
  const res = await classApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await classApi.update({ ...form }) : await classApi.create({ ...form })
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
  await ElMessageBox.confirm(
    `确认删除班级「${row.className}」吗？该班须无在读学生方可删除。`,
    '删除确认',
    { type: 'warning' }
  )
  const res = await classApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

onMounted(() => {
  loadOptions()
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
            placeholder="班级编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="所属专业">
          <el-select v-model="query.majorId" placeholder="全部专业" clearable filterable style="width: 190px" @change="search">
            <el-option
              v-for="m in majors"
              :key="m.id"
              :label="`${m.majorName}（${m.deptName || ''}）`"
              :value="m.id"
            />
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
          <el-icon><Plus /></el-icon>新增班级
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 个班级</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="classCode" label="班级编码" width="105" />
        <el-table-column prop="className" label="班级名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="所属专业" min-width="150">
          <template #default="{ row }">
            {{ row.majorName || majorMap[row.majorId]?.majorName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="年级" width="80" align="center" />
        <el-table-column label="班主任" width="110">
          <template #default="{ row }">{{ row.headTeacherName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="学生数" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.studentCount || 0 }} 人</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="classroom" label="固定教室" width="110" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="班级编码" prop="classCode">
              <el-input v-model="form.classCode" placeholder="如 CS2301" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级">
              <el-input v-model="form.grade" placeholder="如 2023" maxlength="10" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如 计算机科学与技术2301班" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请选择专业" filterable style="width: 100%">
            <el-option
              v-for="m in formMajors"
              :key="m.id"
              :label="`${m.majorName}（${m.deptName || ''}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.headTeacherId" placeholder="可稍后分配" clearable filterable style="width: 100%">
            <el-option
              v-for="t in headTeachers"
              :key="t.id"
              :label="`${t.name}（${t.teacherNo}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="入学年份">
              <el-input-number v-model="form.enrollmentYear" :min="2000" :max="2100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="固定教室">
              <el-input v-model="form.classroom" placeholder="如 R002" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
