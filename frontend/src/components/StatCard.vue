<script setup>
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '-' },
  /** 图标背景色，可直接传渐变色 */
  color: { type: String, default: 'linear-gradient(135deg,#3b82f6,#2563eb)' },
  /** 单位后缀 */
  suffix: { type: String, default: '' },
  /** 底部补充说明 */
  tip: { type: String, default: '' }
})
</script>

<template>
  <div class="stat-card">
    <div class="stat-icon" :style="{ background: color }">
      <slot name="icon" />
    </div>
    <div class="stat-body">
      <div class="stat-value">
        {{ value }}<span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
      </div>
      <div class="stat-label">{{ label }}</div>
      <div v-if="tip" class="stat-tip">{{ tip }}</div>
    </div>
  </div>
</template>

<style scoped>
.stat-suffix {
  font-size: 13px;
  font-weight: 400;
  color: var(--aas-text-secondary);
  margin-left: 3px;
}
.stat-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
</style>
