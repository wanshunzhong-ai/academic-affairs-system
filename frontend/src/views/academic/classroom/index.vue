<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classroomApi } from '@/api'
import { useTable } from '@/composables/useTable'

const ROOM_TYPES = ['普通教室', '多媒体', '机房', '实验室', '阶梯教室', '语音室']

const { loading, list, total, query, load, search, reset, onPageChange, onSizeChange, refreshAfterRemove } =
  useTable(classroomApi.page, { keyword: '', building: '', roomType: undefined, status: undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  roomCode: '',
  roomName: '',
  building: '',
  capacity: 60,
  roomType: '多媒体',
  status: 1
})

const rules = {
  roomCode: [{ required: true, message: '请输入教室编码', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入教室名称', trigger: 'blur' }]
}

const buildings = ref([])

function refreshBuildings() {
  const set = new Set(list.value.map((i) => i.building).filter(Boolean))
  buildings.value = Array.from(set)
}

function openCreate() {
  dialogTitle.value = '新增教室'
  Object.assign(form, {
    id: null,
    roomCode: '',
    roomName: '',
    building: '',
    capacity: 60,
    roomType: '多媒体',
    status: 1
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑教室'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await classroomApi.update({ ...form }) : await classroomApi.create({ ...form })
    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      load().then(refreshBuildings)
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除教室「${row.roomName}」吗？`, '删除确认', { type: 'warning' })
  const res = await classroomApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    refreshAfterRemove()
  }
}

async function doSearch() {
  await search()
  refreshBuildings()
}

onMounted(async () => {
  await load()
  refreshBuildings()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form class="search-bar" inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="教室编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="doSearch"
            @clear="doSearch"
          />
        </el-form-item>
        <el-form-item label="教学楼">
          <el-select v-model="query.building" placeholder="全部" clearable style="width: 160px" @change="doSearch">
            <el-option v-for="b in buildings" :key="b" :label="b" :value="b" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.roomType" placeholder="全部" clearable style="width: 130px" @change="doSearch">
            <el-option v-for="t in ROOM_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="reset()">
            <el-icon><RefreshLeft /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增教室
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 间教室</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="roomCode" label="教室编码" width="110" />
        <el-table-column prop="roomName" label="教室名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="building" label="教学楼" width="140" />
        <el-table-column prop="roomType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.roomType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="容量" width="90" align="center">
          <template #default="{ row }">{{ row.capacity }} 人</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '可用' : '停用' }}
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="教室编码" prop="roomCode">
          <el-input v-model="form.roomCode" placeholder="如 R001" maxlength="20" />
        </el-form-item>
        <el-form-item label="教室名称" prop="roomName">
          <el-input v-model="form.roomName" placeholder="如 第一教学楼-A01" maxlength="50" />
        </el-form-item>
        <el-form-item label="教学楼">
          <el-input v-model="form.building" placeholder="如 第一教学楼" maxlength="30" />
        </el-form-item>
        <el-form-item label="教室类型">
          <el-select v-model="form.roomType" style="width: 100%">
            <el-option v-for="t in ROOM_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="1" :max="1000" /> <span class="text-muted">人</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">可用</el-radio>
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
