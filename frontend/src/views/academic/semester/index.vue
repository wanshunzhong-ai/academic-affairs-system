<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { semesterApi } from '@/api'
import { useTable } from '@/composables/useTable'

const { loading, list, load, search, query } = useTable(semesterApi.list, { keyword: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  semesterName: '',
  schoolYear: '',
  term: 1,
  startDate: '',
  endDate: '',
  selectStart: '',
  selectEnd: '',
  isCurrent: 0,
  status: 1
})

const rules = {
  semesterName: [{ required: true, message: '请输入学期名称', trigger: 'blur' }],
  schoolYear: [{ required: true, message: '请输入学年，如 2026-2027', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增学期'
  const y = new Date().getFullYear()
  Object.assign(form, {
    id: null,
    semesterName: '',
    schoolYear: `${y}-${y + 1}`,
    term: 1,
    startDate: '',
    endDate: '',
    selectStart: '',
    selectEnd: '',
    isCurrent: 0,
    status: 1
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑学期'
  const res = await semesterApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await semesterApi.update({ ...form }) : await semesterApi.create({ ...form })
    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function setCurrent(row) {
  await ElMessageBox.confirm(
    `确认将「${row.semesterName}」设为当前学期吗？全校排课、选课、成绩录入都将以此学期为准。`,
    '设置当前学期',
    { type: 'warning' }
  )
  const res = await semesterApi.setCurrent(row.id)
  if (res.code === 200) {
    ElMessage.success('已设为当前学期')
    load()
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除学期「${row.semesterName}」吗？`, '删除确认', { type: 'warning' })
  const res = await semesterApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    load()
  }
}

onMounted(load)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form class="search-bar" inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="学期名称 / 学年"
            clearable
            style="width: 220px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="query.keyword = ''; search()">
            <el-icon><RefreshLeft /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增学期
        </el-button>
        <div class="toolbar-right text-muted">共 {{ list.length }} 个学期</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="semesterName" label="学期名称" min-width="200" />
        <el-table-column prop="schoolYear" label="学年" width="120" align="center" />
        <el-table-column label="学期" width="90" align="center">
          <template #default="{ row }">第 {{ row.term }} 学期</template>
        </el-table-column>
        <el-table-column label="起止日期" width="210" align="center">
          <template #default="{ row }">{{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="选课开放时间" min-width="250">
          <template #default="{ row }">
            <span class="text-muted">{{ row.selectStart || '-' }} ~ {{ row.selectEnd || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前学期" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isCurrent === 1" type="success" size="small" effect="dark">当前</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.isCurrent !== 1" link type="success" @click="setCurrent(row)">设为当前</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.isCurrent === 1" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="学期名称" prop="semesterName">
          <el-input v-model="form.semesterName" placeholder="如 2026-2027学年第一学期" maxlength="50" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="学年" prop="schoolYear">
              <el-input v-model="form.schoolYear" placeholder="如 2026-2027" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="学期">
              <el-select v-model="form.term" style="width: 100%">
                <el-option :value="1" label="第一学期" />
                <el-option :value="2" label="第二学期" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始日期" label-width="110px">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" label-width="110px">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="选课开始时间">
          <el-date-picker
            v-model="form.selectStart"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选课开放时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="选课结束时间">
          <el-date-picker
            v-model="form.selectEnd"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选课截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
