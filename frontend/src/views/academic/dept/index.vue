<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deptApi } from '@/api'
import { useTable } from '@/composables/useTable'

const { loading, list, total, query, load, search, reset, onPageChange, onSizeChange, refreshAfterRemove } =
  useTable(deptApi.page, { keyword: '', status: undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  deptCode: '',
  deptName: '',
  dean: '',
  phone: '',
  description: '',
  sort: 1,
  status: 1
})

const rules = {
  deptCode: [{ required: true, message: '请输入院系编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入院系名称', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增院系'
  Object.assign(form, {
    id: null,
    deptCode: '',
    deptName: '',
    dean: '',
    phone: '',
    description: '',
    sort: (list.value.length || 0) + 1,
    status: 1
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑院系'
  const res = await deptApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await deptApi.update({ ...form }) : await deptApi.create({ ...form })
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
  await ElMessageBox.confirm(`确认删除院系「${row.deptName}」吗？`, '删除确认', { type: 'warning' })
  const res = await deptApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
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
            placeholder="院系编码 / 名称 / 负责人"
            clearable
            style="width: 220px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
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
          <el-icon><Plus /></el-icon>新增院系
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 个院系</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="deptCode" label="院系编码" width="110" />
        <el-table-column prop="deptName" label="院系名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="dean" label="负责人" width="110" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="description" label="简介" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
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
        <el-form-item label="院系编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="如 CS、EI" maxlength="20" />
        </el-form-item>
        <el-form-item label="院系名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="如 计算机学院" maxlength="50" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.dean" placeholder="院长 / 负责人姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="院系简介">
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
