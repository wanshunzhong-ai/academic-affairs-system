<template>
  <div class="error-page">
    <div class="error-code">404</div>
    <h2>抱歉，您访问的页面不存在</h2>
    <p class="text-muted">页面可能已被移除、重命名，或者您输入的地址有误。</p>
    <div class="error-actions">
      <el-button type="primary" @click="$router.replace('/dashboard')">返回工作台</el-button>
      <el-button @click="$router.back()">返 回</el-button>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'NotFoundPage' })
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
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
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
