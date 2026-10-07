<template>
  <div class="error-page">
    <div class="error-code">403</div>
    <h2>抱歉，您没有访问该页面的权限</h2>
    <p class="text-muted">当前身份：{{ userStore.roleName || '未登录' }}。如需访问，请联系系统管理员分配权限。</p>
    <div class="error-actions">
      <el-button type="primary" @click="$router.replace('/dashboard')">返回工作台</el-button>
      <el-button @click="$router.back()">返 回</el-button>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

defineOptions({ name: 'ForbiddenPage' })

const userStore = useUserStore()
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  text-align: center;
  padding: 20px;
}

.error-code {
  font-size: 110px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 12px;
}

.error-page h2 {
  color: #334155;
  font-size: 20px;
  margin: 0 0 10px;
}

.error-actions {
  margin-top: 22px;
  display: flex;
  gap: 12px;
}
</style>
