<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classApi, noticeApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { noticeTypeTag } from '@/utils/format'
import { NOTICE_SCOPE_OPTIONS, NOTICE_SCOPE_TEXT, NOTICE_TYPE_OPTIONS, ROLE_OPTIONS } from '@/utils/dict'

const userStore = useUserStore()

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
  onSizeChange
} = useTable(noticeApi.managePage, { keyword: '', noticeType: undefined, scope: undefined, status: undefined })

const classes = ref([])

async function loadOptions() {
  const res = await classApi.options()
  let all = res.data || []
  if (userStore.isHeadTeacher) {
    const mine = userStore.manageClassIds || []
    if (mine.length) all = all.filter((c) => mine.includes(c.id))
  }
  classes.value = all
}

/* ==================== 发布 / 编辑 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  title: '',
  noticeType: '通知',
  scope: 'ALL',
  classId: null,
  targetRole: null,
  content: '',
  status: 1
})

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 4, message: '标题至少 4 个字', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const scopeTip = computed(() => {
  if (form.scope === 'CLASS') {
    return userStore.isHeadTeacher
      ? '仅本班学生可见。班主任只能向自己管理的班级发布公告。'
      : '仅所选班级的学生与班主任可见。'
  }
  if (form.scope === 'ROLE') return '仅所选身份的用户可见，例如只发给「班主任」。'
  return '全校师生均可见。'
})

function openCreate() {
  dialogTitle.value = '发布公告'
  Object.assign(form, {
    id: null,
    title: '',
    noticeType: '通知',
    scope: userStore.isHeadTeacher ? 'CLASS' : 'ALL',
    classId: userStore.isHeadTeacher ? classes.value[0]?.id || null : null,
    targetRole: null,
    content: '',
    status: 1
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑公告'
  const res = await noticeApi.detail(row.id)
  Object.assign(form, { ...res.data })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  if (form.scope === 'CLASS' && !form.classId) {
    ElMessage.warning('请选择目标班级')
    return
  }
  if (form.scope === 'ROLE' && !form.targetRole) {
    ElMessage.warning('请选择目标身份')
    return
  }
  submitting.value = true
  try {
    const payload = {
      ...form,
      classId: form.scope === 'CLASS' ? form.classId : null,
      targetRole: form.scope === 'ROLE' ? form.targetRole : null
    }
    const res = payload.id ? await noticeApi.update(payload) : await noticeApi.create(payload)
    if (res.code === 200) {
      ElMessage.success(payload.id ? '修改成功' : '发布成功')
      dialogVisible.value = false
      userStore.refreshBadges()
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function changeStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${next === 1 ? '重新发布' : '下架'}公告「${row.title}」吗？`,
    '操作确认',
    { type: 'warning' }
  )
  const res = await noticeApi.changeStatus(row.id, next)
  if (res.code === 200) {
    ElMessage.success('操作成功')
    load()
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
  const res = await noticeApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    load()
  }
}

async function removeBatch() {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条公告吗？`, '批量删除', {
    type: 'warning'
  })
  const res = await noticeApi.removeBatch(selectedIds.value)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    selection.value = []
    load()
  }
}

const previewVisible = ref(false)
const preview = ref(null)

async function openPreview(row) {
  const res = await noticeApi.detail(row.id)
  preview.value = res.data
  previewVisible.value = true
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
            placeholder="公告标题"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="t in NOTICE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="query.scope" placeholder="全部" clearable style="width: 140px" @change="search">
            <el-option v-for="s in NOTICE_SCOPE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="0" />
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
          <el-icon><Plus /></el-icon>发布公告
        </el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeBatch">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 条公告</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="title" label="公告标题" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="openPreview(row)">{{ row.title }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="noticeTypeTag(row.noticeType)" size="small" effect="plain">
              {{ row.noticeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布范围" width="130" align="center">
          <template #default="{ row }">
            {{ NOTICE_SCOPE_TEXT[row.scope] || row.scope }}
            <span v-if="row.className" class="text-muted">·{{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="changeStatus(row)">
              {{ row.status === 1 ? '下架' : '发布' }}
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="80" show-word-limit />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="公告类型">
              <el-select v-model="form.noticeType" style="width: 100%">
                <el-option v-for="t in NOTICE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布范围">
              <el-select v-model="form.scope" style="width: 100%" :disabled="userStore.isHeadTeacher">
                <el-option v-for="s in NOTICE_SCOPE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="form.scope === 'CLASS'" label="目标班级" required>
          <el-select v-model="form.classId" filterable placeholder="选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.scope === 'ROLE'" label="目标身份" required>
          <el-select v-model="form.targetRole" placeholder="选择身份" style="width: 100%">
            <el-option v-for="r in ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="2000"
            show-word-limit
            placeholder="支持简单 HTML 标签，例如 &lt;p&gt;段落&lt;/p&gt;、&lt;b&gt;加粗&lt;/b&gt;"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">立即发布</el-radio>
            <el-radio :value="0">保存为下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon>
        <template #title>{{ scopeTip }}</template>
      </el-alert>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="preview?.title" width="640px">
      <template v-if="preview">
        <div class="preview-meta">
          <el-tag :type="noticeTypeTag(preview.noticeType)" size="small" effect="plain">
            {{ preview.noticeType }}
          </el-tag>
          <span class="text-muted">{{ preview.publisherName }} · {{ preview.publishTime }}</span>
        </div>
        <div class="preview-content" v-html="preview.content" />
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.preview-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--aas-border);
  margin-bottom: 14px;
}
.preview-content {
  line-height: 1.85;
  color: #334155;
  max-height: 55vh;
  overflow-y: auto;
}
.preview-content :deep(p) {
  margin: 0 0 10px;
}
</style>
