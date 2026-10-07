<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { menuApi } from '@/api'

const loading = ref(false)
const tree = ref([])
const keyword = ref('')

const ICON_OPTIONS = [
  'Odometer', 'School', 'User', 'Avatar', 'Postcard', 'OfficeBuilding', 'Grid', 'Collection',
  'Calendar', 'Location', 'Reading', 'Notebook', 'Tickets', 'Clock', 'ShoppingCart', 'List',
  'DataLine', 'EditPen', 'TrendCharts', 'CircleCheck', 'PieChart', 'Finished', 'Stamp', 'Document',
  'Bell', 'Promotion', 'ChatDotSquare', 'Setting', 'Key', 'Menu', 'UserFilled', 'Suitcase'
]

/** 过滤后的树（保留匹配节点的祖先链） */
const filteredTree = computed(() => {
  const k = keyword.value.trim()
  if (!k) return tree.value
  const filter = (nodes) =>
    nodes
      .map((n) => {
        const children = n.children?.length ? filter(n.children) : []
        const hit =
          (n.menuName || '').includes(k) ||
          (n.perms || '').includes(k) ||
          (n.path || '').includes(k)
        if (hit || children.length) return { ...n, children }
        return null
      })
      .filter(Boolean)
  return filter(tree.value)
})

async function load() {
  loading.value = true
  try {
    const res = await menuApi.tree()
    tree.value = res.data || []
  } finally {
    loading.value = false
  }
}

/* ==================== 表单 ==================== */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null,
  parentId: 0,
  menuName: '',
  menuType: 'C',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 1,
  visible: 1,
  status: 1
})
const form = reactive(emptyForm())

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

/** 上级菜单可选项（M 目录 / C 菜单，不能选按钮） */
const parentOptions = computed(() => {
  const list = [{ id: 0, menuName: '顶级菜单（根目录）' }]
  const walk = (nodes, depth) => {
    nodes.forEach((n) => {
      if (n.menuType !== 'F' && n.id !== form.id) {
        list.push({ id: n.id, menuName: `${'　'.repeat(depth)}${n.menuName}` })
      }
      if (n.children?.length) walk(n.children, depth + 1)
    })
  }
  walk(tree.value, 0)
  return list
})

const isButton = computed(() => form.menuType === 'F')

function openCreate(parentId = 0) {
  dialogTitle.value = '新增菜单'
  Object.assign(form, emptyForm(), { parentId })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑菜单'
  Object.assign(form, emptyForm(), {
    ...row,
    children: undefined,
    parentId: row.parentId ?? 0
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    if (payload.menuType === 'F') {
      payload.path = null
      payload.component = null
      payload.icon = null
    }
    if (!payload.id) delete payload.id
    const res = payload.id ? await menuApi.update(payload) : await menuApi.create(payload)
    if (res.code === 200) {
      ElMessage.success(payload.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确认删除菜单「${row.menuName}」吗？其下级菜单与角色关联也会一并清除。`,
    '删除确认',
    { type: 'warning' }
  )
  const res = await menuApi.remove(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    load()
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
            v-model="keyword"
            placeholder="菜单名称 / 权限标识 / 路由"
            clearable
            style="width: 260px"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="openCreate(0)">
          <el-icon><Plus /></el-icon>新增顶级菜单
        </el-button>
        <div class="toolbar-right text-muted">
          菜单为动态路由来源，新增后需为角色「分配权限」并重新登录方可生效
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredTree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="220">
          <template #default="{ row }">
            <el-icon v-if="row.icon" style="margin-right: 6px; vertical-align: -2px">
              <component :is="row.icon" />
            </el-icon>
            <span>{{ row.menuName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType)" size="small" effect="plain">
              {{ menuTypeText(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="190" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" min-width="150">
          <template #default="{ row }">
            <code v-if="row.perms" class="perms-code">{{ row.perms }}</code>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="可见" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.visible === 1 ? '是' : '隐' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="75" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 'F'" link type="success" @click="openCreate(row.id)">
              新增子项
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" filterable style="width: 100%">
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.menuName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="如 学生管理" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType">
                <el-radio-button value="M">目录</el-radio-button>
                <el-radio-button value="C">菜单</el-radio-button>
                <el-radio-button value="F">按钮</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="!isButton">
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="路由地址">
                <el-input v-model="form.path" placeholder="如 /academic/student" maxlength="120" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="菜单图标">
                <el-select v-model="form.icon" filterable clearable placeholder="选择图标" style="width: 100%">
                  <el-option v-for="i in ICON_OPTIONS" :key="i" :label="i" :value="i">
                    <el-icon style="margin-right: 6px; vertical-align: -2px"><component :is="i" /></el-icon>
                    {{ i }}
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item v-if="form.menuType === 'C'" label="组件路径">
            <el-input v-model="form.component" placeholder="相对 src/views，如 academic/student/index" maxlength="150" />
          </el-form-item>
        </template>

        <el-form-item label="权限标识">
          <el-input v-model="form.perms" placeholder="如 student:list" maxlength="80" />
          <div class="text-muted" style="font-size: 12px; margin-top: 6px">
            用于后端接口校验与前端按钮级权限控制，格式建议 <code>模块:操作</code>
          </div>
        </el-form-item>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否显示">
              <el-radio-group v-model="form.visible">
                <el-radio :value="1">显示</el-radio>
                <el-radio :value="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.perms-code {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  color: #475569;
}
.toolbar-right {
  display: flex;
  align-items: center;
  font-size: 12.5px;
}
</style>
