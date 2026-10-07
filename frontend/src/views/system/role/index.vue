<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { menuApi, roleApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { DATA_SCOPE_TEXT } from '@/utils/dict'

const { loading, list, load, query, search } = useTable(roleApi.list, { keyword: '' })

const filtered = computed(() => {
  const k = (query.keyword || '').trim()
  if (!k) return list.value
  return list.value.filter((r) => (r.roleName || '').includes(k) || (r.roleCode || '').includes(k))
})

/** 内置角色：不允许删除/停用 */
const BUILT_IN = ['STUDENT', 'HEAD_TEACHER', 'ACADEMIC', 'ADMIN']

const DATA_SCOPE_OPTIONS = [
  { label: '仅本人数据', value: 'SELF' },
  { label: '本班数据', value: 'CLASS' },
  { label: '全部数据', value: 'ALL' }
]

/* ==================== 新增 / 编辑 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  dataScope: 'SELF',
  sort: 1,
  status: 1,
  description: ''
})

const rules = {
  roleCode: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[A-Z_]{3,30}$/, message: '角色标识为 3-30 位大写字母或下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增角色'
  Object.assign(form, {
    id: null,
    roleCode: '',
    roleName: '',
    dataScope: 'SELF',
    sort: list.value.length + 1,
    status: 1,
    description: ''
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑角色'
  const res = await roleApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = form.id ? await roleApi.update({ ...form }) : await roleApi.create({ ...form })
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
  if (BUILT_IN.includes(row.roleCode)) {
    ElMessage.warning('内置角色不允许删除')
    return
  }
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '删除确认', { type: 'warning' })
  const res = await roleApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    load()
  }
}

async function changeStatus(row) {
  if (BUILT_IN.includes(row.roleCode)) {
    ElMessage.warning('内置角色不允许停用')
    return
  }
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${next === 1 ? '启用' : '停用'}角色「${row.roleName}」吗？`, '操作确认', {
    type: 'warning'
  })
  const res = await roleApi.changeStatus(row.id, next)
  if (res.code === 200) {
    ElMessage.success('操作成功')
    load()
  }
}

/* ==================== 分配菜单权限 ==================== */
const menuVisible = ref(false)
const menuTarget = ref(null)
const menuTree = ref([])
const checkedKeys = ref([])
const savingMenus = ref(false)
const menuTreeRef = ref()

async function loadMenuTree() {
  const res = await menuApi.tree()
  menuTree.value = res.data || []
}

async function openMenuAssign(row) {
  menuTarget.value = row
  await loadMenuTree()
  const res = await roleApi.menuIds(row.id)
  checkedKeys.value = res.data || []
  menuVisible.value = true
  await nextTick()
  // 仅设置叶子节点为选中，父节点由组件自动推导半选状态
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys(checkedKeys.value, false)
  }
}

function getCheckedKeys() {
  if (!menuTreeRef.value) return checkedKeys.value
  const checked = menuTreeRef.value.getCheckedKeys()
  const half = menuTreeRef.value.getHalfCheckedKeys()
  return [...half, ...checked]
}

function toggleAll(checked) {
  if (!menuTreeRef.value) return
  const collect = (nodes) => {
    nodes.forEach((n) => {
      menuTreeRef.value.setChecked(n.id, checked, false)
      if (n.children?.length) collect(n.children)
    })
  }
  collect(menuTree.value)
}

async function submitMenus() {
  savingMenus.value = true
  try {
    const ids = getCheckedKeys()
    const res = await roleApi.assignMenus(menuTarget.value.id, ids)
    if (res.code === 200) {
      ElMessage.success('权限已更新，相关用户重新登录后生效')
      menuVisible.value = false
    }
  } finally {
    savingMenus.value = false
  }
}

const menuTypeTag = (t) => (t === 'M' ? 'warning' : t === 'C' ? 'primary' : 'info')
const menuTypeText = (t) => (t === 'M' ? '目录' : t === 'C' ? '菜单' : '按钮')

onMounted(load)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form class="search-bar" inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="角色名称 / 标识"
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
          <el-icon><Plus /></el-icon>新增角色
        </el-button>
        <div class="toolbar-right text-muted">共 {{ filtered.length }} 个角色</div>
      </div>

      <el-table v-loading="loading" :data="filtered" stripe border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="140">
          <template #default="{ row }">
            {{ row.roleName }}
            <el-tag v-if="BUILT_IN.includes(row.roleCode)" size="small" type="info" effect="plain" style="margin-left: 4px">
              内置
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleCode" label="角色标识" width="150" />
        <el-table-column label="数据范围" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.dataScope === 'ALL' ? 'danger' : row.dataScope === 'CLASS' ? 'warning' : 'info'" effect="plain">
              {{ DATA_SCOPE_TEXT[row.dataScope] || row.dataScope }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="角色说明" min-width="280" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openMenuAssign(row)">分配权限</el-button>
            <el-button link type="warning" @click="changeStatus(row)">启停</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="如 教务员" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色标识" prop="roleCode">
              <el-input v-model="form.roleCode" placeholder="如 ACADEMIC_ASSIST" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option v-for="s in DATA_SCOPE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <div class="text-muted" style="font-size: 12px; margin-top: 6px">
            决定该角色能看到哪些数据：仅本人 / 本班 / 全部
          </div>
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
        <el-form-item label="角色说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuVisible" title="分配菜单权限" width="620px" destroy-on-close top="6vh">
      <el-alert type="info" :closable="false" show-icon class="mb-16">
        <template #title>
          为角色「{{ menuTarget?.roleName }}」配置可访问的菜单与操作权限。已勾选的父级目录会自动包含。
        </template>
      </el-alert>

      <div class="toolbar">
        <el-button size="small" @click="toggleAll(true)">全选</el-button>
        <el-button size="small" @click="toggleAll(false)">全不选</el-button>
        <div class="toolbar-right text-muted">已选 {{ getCheckedKeys().length }} 项</div>
      </div>

      <div class="menu-tree-wrap">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'menuName', children: 'children' }"
        >
          <template #default="{ data }">
            <span class="menu-node">
              <el-tag :type="menuTypeTag(data.menuType)" size="small" effect="plain">
                {{ menuTypeText(data.menuType) }}
              </el-tag>
              <span class="mn-name">{{ data.menuName }}</span>
              <span v-if="data.perms" class="mn-perms">{{ data.perms }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <template #footer>
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMenus" @click="submitMenus">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.menu-tree-wrap {
  max-height: 52vh;
  overflow-y: auto;
  border: 1px solid var(--aas-border);
  border-radius: 8px;
  padding: 10px;
}
.menu-node {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mn-name {
  font-size: 13px;
}
.mn-perms {
  font-size: 11px;
  color: #94a3b8;
  font-family: Consolas, Monaco, monospace;
}
.toolbar-right {
  display: flex;
  align-items: center;
}
</style>
