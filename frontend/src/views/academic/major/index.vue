<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deptApi, majorApi } from '@/api'
import { useTable } from '@/composables/useTable'

const { loading, list, total, query, load, search, reset, onPageChange, onSizeChange, refreshAfterRemove } =
  useTable(majorApi.page, { keyword: '', deptId: undefined, status: undefined })

const depts = ref([])
const deptMap = computed(() => {
  const m = {}
  depts.value.forEach((d) => {
    m[d.id] = d.deptName
  })
  return m
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  majorCode: '',
  majorName: '',
  deptId: null,
  degree: '工学学士',
  duration: 4,
  description: '',
  status: 1
})

const rules = {
  majorCode: [{ required: true, message: '请输入专业编码', trigger: 'blur' }],
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属院系', trigger: 'change' }]
}

async function loadDepts() {
  const res = await deptApi.options()
  depts.value = res.data || []
}

function openCreate() {
  dialogTitle.value = '新增专业'
  Object.assign(form, {
    id: null,
    majorCode: '',
    majorName: '',
    deptId: query.deptId || null,
    degree: '工学学士',
    duration: 4,
    description: '',
    status: 1
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑专业'
  const res = await majorApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await majorApi.update({ ...form }) : await majorApi.create({ ...form })
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
  await ElMessageBox.confirm(`确认删除专业「${row.majorName}」吗？`, '删除确认', { type: 'warning' })
  const res = await majorApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
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
            placeholder="专业编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="所属院系">
          <el-select v-model="query.deptId" placeholder="全部院系" clearable style="width: 180px" @change="search">
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
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
          <el-icon><Plus /></el-icon>新增专业
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 个专业</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="majorCode" label="专业编码" width="110" />
        <el-table-column prop="majorName" label="专业名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="所属院系" min-width="140">
          <template #default="{ row }">{{ row.deptName || deptMap[row.deptId] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="degree" label="授予学位" width="120" />
        <el-table-column prop="duration" label="学制" width="80" align="center">
          <template #default="{ row }">{{ row.duration }} 年</template>
        </el-table-column>
        <el-table-column prop="description" label="专业简介" min-width="200" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="专业编码" prop="majorCode">
          <el-input v-model="form.majorCode" placeholder="如 CS01" maxlength="20" />
        </el-form-item>
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="form.majorName" placeholder="如 计算机科学与技术" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属院系" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择院系" style="width: 100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授予学位">
          <el-input v-model="form.degree" placeholder="如 工学学士" maxlength="30" />
        </el-form-item>
        <el-form-item label="学制">
          <el-input-number v-model="form.duration" :min="1" :max="8" /> <span class="text-muted">年</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="专业简介">
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
