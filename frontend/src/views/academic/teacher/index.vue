<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, deptApi, teacherApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { genderText } from '@/utils/format'
import { EDUCATION_OPTIONS, GENDER_OPTIONS, TITLE_OPTIONS } from '@/utils/dict'

const {
  loading,
  list,
  total,
  query,
  load,
  search,
  reset,
  onPageChange,
  onSizeChange,
  refreshAfterRemove
} = useTable(teacherApi.page, {
  keyword: '',
  deptId: undefined,
  isHeadTeacher: undefined,
  title: undefined,
  status: undefined
})

const depts = ref([])
const allClasses = ref([])

async function loadOptions() {
  const [d, c] = await Promise.all([deptApi.options(), classApi.options()])
  depts.value = d.data || []
  allClasses.value = c.data || []
}

/* ==================== 新增 / 编辑 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null,
  teacherNo: '',
  name: '',
  gender: 1,
  birthDate: '',
  phone: '',
  email: '',
  idCard: '',
  deptId: null,
  title: '讲师',
  education: '硕士',
  hireDate: '',
  isHeadTeacher: 0,
  status: 1,
  remark: ''
})
const form = reactive(emptyForm())

const rules = {
  teacherNo: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]{3,20}$/, message: '工号为 3-20 位字母或数字', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属院系', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增教师'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑教师'
  const res = await teacherApi.detail(row.id)
  Object.assign(form, emptyForm(), res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    if (!payload.id) delete payload.id
    const res = payload.id ? await teacherApi.update(payload) : await teacherApi.create(payload)
    if (res.code === 200) {
      ElMessage.success(
        payload.id ? '修改成功' : '新增成功' + (payload.isHeadTeacher === 1 ? '（已创建班主任登录账号，密码 123456）' : '')
      )
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除教师「${row.name}（${row.teacherNo}）」吗？`, '删除确认', { type: 'warning' })
  const res = await teacherApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

async function changeStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认将「${row.name}」的状态改为「${next === 1 ? '正常' : '停用'}」吗？`, '提示', {
    type: 'warning'
  })
  const res = await teacherApi.changeStatus(row.id, next)
  if (res.code === 200) {
    ElMessage.success('状态已更新')
    load()
  }
}

/* ==================== 分配带班班级 ==================== */
const assignVisible = ref(false)
const assignTeacher = ref(null)
const assignIds = ref([])
const assigning = ref(false)

async function openAssign(row) {
  const res = await teacherApi.detail(row.id)
  assignTeacher.value = res.data
  assignIds.value = res.data.headClassIds || []
  assignVisible.value = true
}

async function submitAssign() {
  assigning.value = true
  try {
    const res = await teacherApi.assignClasses(assignTeacher.value.id, assignIds.value)
    if (res.code === 200) {
      ElMessage.success('带班班级已更新')
      assignVisible.value = false
      load()
    }
  } finally {
    assigning.value = false
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
            placeholder="工号 / 姓名 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 170px" @change="search">
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="query.title" placeholder="全部" clearable style="width: 130px" @change="search">
            <el-option v-for="t in TITLE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="query.isHeadTeacher" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
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
          <el-icon><Plus /></el-icon>新增教师
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 名教师</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="teacherNo" label="工号" width="105" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="院系" min-width="140">
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="100" align="center" />
        <el-table-column prop="education" label="学历" width="90" align="center" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="带班情况" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.isHeadTeacher === 1" type="success" size="small" effect="plain">班主任</el-tag>
            <span v-if="row.headClassNames" class="text-muted" style="margin-left: 6px">
              {{ row.headClassNames }}
            </span>
            <span v-else-if="row.isHeadTeacher !== 1" class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在职' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openAssign(row)">分配班级</el-button>
            <el-button link type="warning" @click="changeStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close top="8vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="工号" prop="teacherNo">
              <el-input v-model="form.teacherNo" placeholder="如 T1001" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="教师姓名" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio v-for="g in GENDER_OPTIONS" :key="g.value" :value="g.value">{{ g.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="所属院系" prop="deptId">
              <el-select v-model="form.deptId" placeholder="选择院系" style="width: 100%">
                <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称">
              <el-select v-model="form.title" style="width: 100%">
                <el-option v-for="t in TITLE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="form.education" style="width: 100%">
                <el-option v-for="e in EDUCATION_OPTIONS" :key="e.value" :label="e.label" :value="e.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期">
              <el-date-picker
                v-model="form.hireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="联系手机" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="电子邮箱" maxlength="60" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="18 位身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="是否班主任">
          <el-switch v-model="form.isHeadTeacher" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
          <span v-if="!form.id" class="text-muted" style="margin-left: 12px; font-size: 12px">
            选择「是」将自动创建班主任登录账号（用户名 = 工号，密码 123456）
          </span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在职</el-radio>
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

    <!-- 分配班级 -->
    <el-dialog v-model="assignVisible" title="分配带班班级" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="mb-16">
        <template #title>
          为「{{ assignTeacher?.name }}（{{ assignTeacher?.teacherNo }}）」设置所带班级。
          分配到班级后，该教师可查看并管理对应班级的学生、成绩与请假。
        </template>
      </el-alert>
      <el-select v-model="assignIds" multiple filterable placeholder="请选择班级" style="width: 100%">
        <el-option v-for="c in allClasses" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
