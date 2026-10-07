<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logApi } from '@/api'
import { useTable } from '@/composables/useTable'

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
} = useTable(logApi.page, {
  keyword: '',
  module: '',
  status: undefined,
  startDate: undefined,
  endDate: undefined
})

const modules = ref([])
const dateRange = ref([])
const cleanDays = ref(30)

async function loadModules() {
  const res = await logApi.modules()
  modules.value = res.data || []
}

function onDateRangeChange(val) {
  query.startDate = val?.[0] || undefined
  query.endDate = val?.[1] || undefined
  search()
}

async function removeBatch() {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条日志吗？`, '删除确认', {
    type: 'warning'
  })
  const res = await logApi.removeBatch(selectedIds.value)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    selection.value = []
    load()
  }
}

async function clearAll() {
  await ElMessageBox.confirm(
    '确认清空全部操作日志吗？该操作不可恢复。',
    '危险操作',
    { type: 'warning', confirmButtonText: '确认清空' }
  )
  const res = await logApi.clear()
  if (res.code === 200) {
    ElMessage.success('已清空')
    load()
  }
}

async function cleanBefore() {
  await ElMessageBox.confirm(
    `确认清理 ${cleanDays.value} 天前的操作日志吗？`,
    '清理历史日志',
    { type: 'warning' }
  )
  const res = await logApi.clean(cleanDays.value)
  if (res.code === 200) {
    ElMessage.success(`已清理 ${res.data ?? 0} 条日志`)
    load()
    loadModules()
  }
}

/* ==================== 详情 ==================== */
const detailVisible = ref(false)
const detail = ref(null)

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function methodTag(m) {
  if (!m) return 'info'
  const up = m.toUpperCase()
  if (up === 'GET') return 'primary'
  if (up === 'POST') return 'success'
  if (up === 'PUT') return 'warning'
  if (up === 'DELETE') return 'danger'
  return 'info'
}

onMounted(() => {
  loadModules()
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
            placeholder="账号 / 姓名 / 操作内容 / 接口"
            clearable
            style="width: 220px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 160px" @change="search">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 240px"
            @change="onDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="dateRange = []; reset()">
            <el-icon><RefreshLeft /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeBatch">
          <el-icon><Delete /></el-icon>删除选中
        </el-button>
        <el-button type="warning" plain @click="cleanBefore">
          <el-icon><Timer /></el-icon>清理历史日志
        </el-button>
        <el-input-number v-model="cleanDays" :min="1" :max="3650" size="small" style="width: 110px" />
        <span class="text-muted" style="font-size: 12px">天前</span>
        <el-button type="danger" plain @click="clearAll">
          <el-icon><DeleteFilled /></el-icon>清空全部
        </el-button>
        <div class="toolbar-right text-muted">共 {{ total }} 条日志</div>
      </div>

      <el-table v-loading="loading" :data="list" stripe border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="createTime" label="操作时间" width="165" />
        <el-table-column prop="realName" label="操作人" width="105">
          <template #default="{ row }">
            {{ row.realName || '-' }}
            <div class="text-muted" style="font-size: 11px">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.roleName" size="small" effect="plain">{{ row.roleName }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="业务模块" width="120" />
        <el-table-column prop="operation" label="操作内容" min-width="150" show-overflow-tooltip />
        <el-table-column label="请求方式" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="methodTag(row.method)" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUri" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="130" show-overflow-tooltip />
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.costTime > 1000 }">{{ row.costTime }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailVisible" title="操作日志详情" size="620px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="操作时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ detail.realName }}（{{ detail.username }}）· {{ detail.roleName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="业务模块">{{ detail.module }}</el-descriptions-item>
          <el-descriptions-item label="操作内容">{{ detail.operation }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">{{ detail.method }}</el-descriptions-item>
          <el-descriptions-item label="请求地址">{{ detail.requestUri }}</el-descriptions-item>
          <el-descriptions-item label="IP 地址">{{ detail.ip }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
          <el-descriptions-item label="执行结果">
            <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
              {{ detail.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.errorMsg" label="错误信息">
            <span class="text-danger">{{ detail.errorMsg }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="请求参数">
            <pre class="param-pre">{{ detail.requestParam || '无' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.param-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
  color: #475569;
}
.toolbar-right {
  display: flex;
  align-items: center;
}
</style>
