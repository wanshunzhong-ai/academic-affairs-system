<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { noticeApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { noticeTypeTag } from '@/utils/format'
import { NOTICE_TYPE_OPTIONS } from '@/utils/dict'

const userStore = useUserStore()

const { loading, list, total, query, load, search, reset, onPageChange, onSizeChange } = useTable(
  noticeApi.page,
  { keyword: '', noticeType: undefined }
)

const viewMode = ref('list')

const detailVisible = ref(false)
const current = ref(null)

async function openDetail(row) {
  const res = await noticeApi.detail(row.id)
  current.value = res.data
  detailVisible.value = true
  if (!row.readFlag) {
    try {
      await noticeApi.markRead(row.id)
      row.readFlag = true
      userStore.refreshBadges()
    } catch {
      /* 忽略 */
    }
  }
}

async function markAllRead() {
  const unread = list.value.filter((i) => !i.readFlag)
  if (!unread.length) {
    ElMessage.info('当前页没有未读公告')
    return
  }
  await Promise.all(unread.map((i) => noticeApi.markRead(i.id).catch(() => null)))
  ElMessage.success('已全部标记为已读')
  userStore.refreshBadges()
  load()
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
            placeholder="公告标题"
            clearable
            style="width: 240px"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" placeholder="全部" clearable style="width: 130px" @change="search">
            <el-option v-for="t in NOTICE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
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
        <el-button plain @click="markAllRead">
          <el-icon><Select /></el-icon>本页全部已读
        </el-button>
        <div class="toolbar-right">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="list">列表</el-radio-button>
            <el-radio-button value="card">卡片</el-radio-button>
          </el-radio-group>
          <span class="text-muted" style="margin-left: 12px">共 {{ total }} 条公告</span>
        </div>
      </div>

      <!-- 列表视图 -->
      <template v-if="viewMode === 'list'">
        <el-table v-loading="loading" :data="list" stripe border>
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-badge v-if="!row.readFlag" is-dot type="danger">
                <el-tag type="warning" size="small" effect="plain">未读</el-tag>
              </el-badge>
              <el-tag v-else type="info" size="small" effect="plain">已读</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="公告标题" min-width="280" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link :type="row.readFlag ? 'info' : 'primary'" @click="openDetail(row)">
                {{ row.title }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="noticeTypeTag(row.noticeType)" size="small" effect="plain">
                {{ row.noticeType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布人" width="110" />
          <el-table-column prop="publisherRole" label="发布单位" width="120" />
          <el-table-column prop="publishTime" label="发布时间" width="165" />
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 卡片视图 -->
      <template v-else>
        <div v-loading="loading" class="notice-grid">
          <div
            v-for="item in list"
            :key="item.id"
            class="notice-card"
            :class="{ unread: !item.readFlag }"
            @click="openDetail(item)"
          >
            <div class="nc-head">
              <el-tag :type="noticeTypeTag(item.noticeType)" size="small" effect="plain">
                {{ item.noticeType }}
              </el-tag>
              <el-tag v-if="!item.readFlag" type="danger" size="small" effect="dark">未读</el-tag>
            </div>
            <div class="nc-title">{{ item.title }}</div>
            <div class="nc-foot">
              <span>{{ item.publisherName }}</span>
              <span>{{ item.publishTime }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" description="暂无公告" :image-size="100" />
      </template>

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

    <el-dialog v-model="detailVisible" :title="current?.title" width="680px" top="6vh">
      <template v-if="current">
        <div class="preview-meta">
          <el-tag :type="noticeTypeTag(current.noticeType)" size="small" effect="plain">
            {{ current.noticeType }}
          </el-tag>
          <span class="text-muted">
            {{ current.publisherName }}（{{ current.publisherRole }}） · {{ current.publishTime }}
          </span>
        </div>
        <div class="preview-content" v-html="current.content" />
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}
.notice-card {
  border: 1px solid var(--aas-border);
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.18s;
  background: #fff;
}
.notice-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}
.notice-card.unread {
  border-left: 3px solid #2563eb;
  background: #f8fbff;
}
.nc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.nc-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--aas-text);
  line-height: 1.5;
  min-height: 45px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.nc-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--aas-text-secondary);
  margin-top: 12px;
}
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
  max-height: 58vh;
  overflow-y: auto;
}
.preview-content :deep(p) {
  margin: 0 0 10px;
}
.toolbar-right {
  display: flex;
  align-items: center;
}
</style>
