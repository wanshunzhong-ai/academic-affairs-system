<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, deptApi, majorApi, studentApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import {
  genderText,
  studentStatusText,
  studentStatusType,
  fixed,
  scoreColor
} from '@/utils/format'
import { GENDER_OPTIONS, POLITICAL_OPTIONS, STUDENT_STATUS_OPTIONS } from '@/utils/dict'

const userStore = useUserStore()
const canEdit = computed(() => userStore.hasAnyPerm('student:edit', 'student:add'))

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
} = useTable(studentApi.page, {
  keyword: '',
  deptId: undefined,
  majorId: undefined,
  classId: undefined,
  status: undefined
})

const depts = ref([])
const majors = ref([])
const classes = ref([])
const loadingOptions = ref(false)

/** 表单里可选的专业（若选了院系则联动过滤） */
const formMajors = computed(() => {
  if (!form.deptId) return majors.value
  return majors.value.filter((m) => m.deptId === form.deptId)
})

/** 表单里可选的班级 */
const formClasses = computed(() => {
  if (!form.majorId) return classes.value
  return classes.value.filter((c) => c.majorId === form.majorId)
})

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [d, m, c] = await Promise.all([deptApi.options(), majorApi.options(), classApi.options()])
    depts.value = d.data || []
    majors.value = m.data || []
    classes.value = c.data || []
  } finally {
    loadingOptions.value = false
  }
}

/* ==================== 新增 / 编辑 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null,
  studentNo: '',
  name: '',
  gender: 1,
  birthDate: '',
  idCard: '',
  phone: '',
  email: '',
  deptId: null,
  majorId: null,
  classId: null,
  enrollmentDate: '',
  politicalStatus: '共青团员',
  address: '',
  guardianName: '',
  guardianPhone: '',
  dormitory: '',
  status: 1
})
const form = reactive(emptyForm())

const rules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]{4,20}$/, message: '学号为 4-20 位字母或数字', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增学生'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑学生'
  const res = await studentApi.detail(row.id)
  Object.assign(form, emptyForm(), res.data)
  dialogVisible.value = true
}

/** 院系变化时清空专业/班级 */
function onDeptChange() {
  form.majorId = null
  form.classId = null
}

function onMajorChange() {
  form.classId = null
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    if (!payload.id) delete payload.id
    const res = payload.id ? await studentApi.update(payload) : await studentApi.create(payload)
    if (res.code === 200) {
      ElMessage.success(payload.id ? '修改成功' : '新增成功（登录账号密码默认为 123456）')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除学生「${row.name}（${row.studentNo}）」吗？`, '删除确认', {
    type: 'warning'
  })
  const res = await studentApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

async function removeBatch() {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 名学生吗？`, '批量删除', {
    type: 'warning'
  })
  const res = await studentApi.removeBatch(selectedIds.value)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    selection.value = []
    load()
  }
}

async function changeStatus(row) {
  const next = row.status === 1 ? 2 : 1
  await ElMessageBox.confirm(
    `确认将「${row.name}」的学籍状态改为「${studentStatusText(next)}」吗？`,
    '修改学籍状态',
    { type: 'warning' }
  )
  const res = await studentApi.changeStatus(row.id, next)
  if (res.code === 200) {
    ElMessage.success('状态已更新')
    load()
  }
}

/* ==================== 导入 / 导出 ==================== */
const importVisible = ref(false)
const importFile = ref(null)
const importing = ref(false)
const importResult = ref(null)
const fileList = ref([])

function downloadTemplate() {
  studentApi.downloadTemplate()
}

async function doExport() {
  await studentApi.export({ ...query })
  ElMessage.success('导出已开始，请查看浏览器下载')
}

async function doImport() {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的 Excel 文件')
    return
  }
  importing.value = true
  try {
    const res = await studentApi.import(importFile.value)
    if (res.code === 200) {
      importResult.value = res.data
      ElMessage.success('导入完成')
      load()
    }
  } finally {
    importing.value = false
  }
}

function onFileChange(file) {
  importFile.value = file.raw
  importResult.value = null
}

function openImport() {
  importFile.value = null
  importResult.value = null
  fileList.value = []
  importVisible.value = true
}

/* ==================== 详情抽屉 ==================== */
const detailVisible = ref(false)
const detail = ref(null)

async function openDetail(row) {
  const res = await studentApi.detail(row.id)
  detail.value = res.data
  detailVisible.value = true
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
            placeholder="学号 / 姓名 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="院系">
          <el-select
            v-model="query.deptId"
            placeholder="全部"
            clearable
            style="width: 160px"
            @change="query.majorId = undefined; query.classId = undefined; search()"
          >
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable filterable style="width: 200px" @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学籍状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="s in STUDENT_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
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
          <el-icon><Plus /></el-icon>新增学生
        </el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeBatch">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
        <el-button plain @click="openImport">
          <el-icon><Upload /></el-icon>批量导入
        </el-button>
        <el-button plain @click="doExport">
          <el-icon><Download /></el-icon>导出 Excel
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 名学生</div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        border
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="班级" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.className || '-' }}</template>
        </el-table-column>
        <el-table-column label="专业" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.majorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="dormitory" label="宿舍" width="110" />
        <el-table-column label="学籍状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="studentStatusType(row.status)" size="small" effect="plain">
              {{ studentStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="changeStatus(row)">
              {{ row.status === 1 ? '休学' : '复学' }}
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" v-loading="loadingOptions">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="如 2023001" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="学生姓名" maxlength="30" />
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
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="18 位身份证号" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="政治面貌">
              <el-select v-model="form.politicalStatus" style="width: 100%">
                <el-option v-for="p in POLITICAL_OPTIONS" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
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

        <el-divider content-position="left">学籍信息</el-divider>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="所属院系" prop="deptId">
              <el-select v-model="form.deptId" placeholder="选择院系" style="width: 100%" @change="onDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属专业" prop="majorId">
              <el-select
                v-model="form.majorId"
                placeholder="选择专业"
                style="width: 100%"
                filterable
                @change="onMajorChange"
              >
                <el-option v-for="m in formMajors" :key="m.id" :label="m.majorName" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="所属班级" prop="classId">
              <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%" filterable>
                <el-option v-for="c in formClasses" :key="c.id" :label="c.className" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学日期">
              <el-date-picker
                v-model="form.enrollmentDate"
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
            <el-form-item label="宿舍">
              <el-input v-model="form.dormitory" placeholder="如 2号楼416" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学籍状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="s in STUDENT_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">其他信息</el-divider>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="监护人">
              <el-input v-model="form.guardianName" placeholder="监护人姓名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监护人电话">
              <el-input v-model="form.guardianPhone" placeholder="监护人手机号" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="家庭住址">
          <el-input v-model="form.address" type="textarea" :rows="2" maxlength="120" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入 -->
    <el-dialog v-model="importVisible" title="批量导入学生" width="560px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="mb-16">
        <template #title>
          请先下载模板，按模板格式填写后上传。导入成功后系统会自动为每位学生创建登录账号
          （用户名 = 学号，初始密码 = 123456）。
        </template>
      </el-alert>
      <div class="mb-16">
        <el-button link type="primary" @click="downloadTemplate">
          <el-icon><Download /></el-icon>下载导入模板
        </el-button>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :file-list="fileList"
        :on-change="onFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
      </el-upload>

      <el-descriptions v-if="importResult" :column="2" border size="small" class="mt-16">
        <el-descriptions-item label="总行数">{{ importResult.total ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ importResult.success ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ importResult.fail ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="提示">{{ importResult.message || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="学生详细信息" size="620px">
      <template v-if="detail">
        <div class="detail-head">
          <div class="dh-avatar">{{ (detail.name || '?').slice(0, 1) }}</div>
          <div>
            <div class="dh-name">
              {{ detail.name }}
              <el-tag :type="studentStatusType(detail.status)" size="small" effect="plain" style="margin-left: 8px">
                {{ studentStatusText(detail.status) }}
              </el-tag>
            </div>
            <div class="text-muted">{{ detail.studentNo }} · {{ detail.className || '-' }}</div>
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="mb-16">
          <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(detail.gender) }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ detail.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ detail.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="政治面貌">{{ detail.politicalStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="院系">{{ detail.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ detail.majorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ detail.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="年级">{{ detail.grade || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入学日期">{{ detail.enrollmentDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="宿舍">{{ detail.dormitory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="监护人">{{ detail.guardianName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="监护人电话">{{ detail.guardianPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="家庭住址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">学业概况</el-divider>
        <div class="score-mini">
          <div class="sm-item">
            <div class="sm-val" :style="{ color: scoreColor(detail.avgPoint != null ? detail.avgPoint * 25 : null) }">
              {{ fixed(detail.avgPoint, 2) }}
            </div>
            <div class="sm-label">平均绩点</div>
          </div>
          <div class="sm-item">
            <div class="sm-val">{{ fixed(detail.earnedCredit, 1) }}</div>
            <div class="sm-label">已获学分</div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--aas-border);
  margin-bottom: 16px;
}
.dh-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
}
.dh-name {
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.score-mini {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.sm-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.sm-val {
  font-size: 24px;
  font-weight: 600;
}
.sm-label {
  font-size: 12px;
  color: var(--aas-text-secondary);
  margin-top: 4px;
}
:deep(.el-upload-dragger) {
  padding: 24px;
}
</style>
