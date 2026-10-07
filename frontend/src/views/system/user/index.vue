<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roleApi, userApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { genderText } from '@/utils/format'
import { GENDER_OPTIONS, USER_TYPE_OPTIONS, USER_TYPE_TEXT } from '@/utils/dict'

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
} = useTable(userApi.page, { keyword: '', userType: undefined, roleCode: undefined, status: undefined })

const roles = ref([])

async function loadRoles() {
  const res = await roleApi.options()
  roles.value = res.data || []
}

const roleMap = computed(() => Object.fromEntries(roles.value.map((r) => [r.roleCode, r.roleName])))

/* ==================== 新增 / 编辑 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null,
  username: '',
  realName: '',
  userType: 'ADMIN',
  gender: 1,
  phone: '',
  email: '',
  status: 1,
  remark: '',
  roleIds: []
})
const form = reactive(emptyForm())

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,30}$/, message: '账号为 3-30 位字母、数字或下划线', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请至少分配一个角色', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增用户'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑用户'
  const res = await userApi.detail(row.id)
  Object.assign(form, emptyForm(), res.data, { roleIds: res.data.roleIds || [] })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    if (!payload.id) delete payload.id
    const res = payload.id ? await userApi.update(payload) : await userApi.create(payload)
    if (res.code === 200) {
      ElMessage.success(payload.id ? '修改成功' : '新增成功（初始密码 123456）')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.realName}（${row.username}）」吗？`, '删除确认', {
    type: 'warning'
  })
  const res = await userApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    load()
  }
}

async function removeBatch() {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个用户吗？`, '批量删除', {
    type: 'warning'
  })
  const res = await userApi.removeBatch(selectedIds.value)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    selection.value = []
    load()
  }
}

async function resetPassword(row) {
  await ElMessageBox.confirm(
    `确认将「${row.realName}」的密码重置为 123456 吗？重置后该用户需使用新密码登录。`,
    '重置密码',
    { type: 'warning' }
  )
  const res = await userApi.resetPassword(row.id, '123456')
  if (res.code === 200) {
    ElMessage.success('密码已重置为 123456')
  }
}

async function changeStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${next === 1 ? '启用' : '停用'}用户「${row.realName}」吗？`,
    '操作确认',
    { type: 'warning' }
  )
  const res = await userApi.changeStatus(row.id, next)
  if (res.code === 200) {
    ElMessage.success('操作成功')
    load()
  }
}

/* ==================== 分配角色 ==================== */
const roleVisible = ref(false)
const roleTarget = ref(null)
const roleIds = ref([])
const assigning = ref(false)

async function openAssignRole(row) {
  const res = await userApi.detail(row.id)
  roleTarget.value = res.data
  roleIds.value = res.data.roleIds || []
  roleVisible.value = true
}

async function submitAssignRole() {
  if (!roleIds.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  assigning.value = true
  try {
    const res = await userApi.assignRoles(roleTarget.value.id, roleIds.value)
    if (res.code === 200) {
      ElMessage.success('角色已更新')
      roleVisible.value = false
      load()
    }
  } finally {
    assigning.value = false
  }
}

onMounted(async () => {
  await loadRoles()
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
            placeholder="账号 / 姓名 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="query.userType" placeholder="全部" clearable style="width: 130px" @change="search">
            <el-option v-for="t in USER_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleCode" placeholder="全部" clearable style="width: 150px" @change="search">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.roleCode" />
          </el-select>
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
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeBatch">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 个用户</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="用户类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ USER_TYPE_TEXT[row.userType] || row.userType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag
              v-for="(code, i) in row.roleCodes || []"
              :key="code"
              :type="code === 'ADMIN' ? 'danger' : code === 'ACADEMIC' ? 'warning' : code === 'HEAD_TEACHER' ? 'success' : 'primary'"
              size="small"
              effect="plain"
              style="margin-right: 4px"
            >
              {{ (row.roleNames || [])[i] || roleMap[code] || code }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="165" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openAssignRole(row)">分配角色</el-button>
            <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="changeStatus(row)">
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" placeholder="字母/数字/下划线" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="用户类型">
              <el-select v-model="form.userType" style="width: 100%">
                <el-option v-for="t in USER_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio v-for="g in GENDER_OPTIONS" :key="g.value" :value="g.value">{{ g.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="分配角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.roleName}（${r.roleCode}）`" :value="r.id" />
          </el-select>
        </el-form-item>
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
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="120" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配角色" width="480px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="mb-16">
        <template #title>
          为用户「{{ roleTarget?.realName }}（{{ roleTarget?.username }}）」设置角色。角色决定了可访问的菜单与数据范围。
        </template>
      </el-alert>
      <el-checkbox-group v-model="roleIds">
        <div v-for="r in roles" :key="r.id" class="role-item">
          <el-checkbox :value="r.id">
            <span>{{ r.roleName }}</span>
            <span class="text-muted" style="margin-left: 8px; font-size: 12px">{{ r.description }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssignRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-item {
  padding: 6px 0;
  border-bottom: 1px dashed var(--aas-border);
}
.role-item:last-child {
  border-bottom: none;
}
</style>
