<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'

const props = defineProps({
  /** ECharts option */
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '320px' },
  loading: { type: Boolean, default: false },
  /** 是否显示空态 */
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' }
})

const el = ref(null)
let chart = null

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option || {}, true)
}

function resize() {
  if (chart) chart.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(
  () => props.option,
  async () => {
    await nextTick()
    render()
  },
  { deep: true }
)

watch(
  () => props.loading,
  (v) => {
    if (!chart) return
    if (v) chart.showLoading({ text: '加载中', color: '#2563eb', textColor: '#64748b', maskColor: 'rgba(255,255,255,0.7)' })
    else chart.hideLoading()
  }
)

defineExpose({ resize })
</script>

<template>
  <div class="chart-box" :style="{ height }">
    <el-empty v-if="empty" :description="emptyText" :image-size="80" />
    <div v-else ref="el" class="chart-canvas" />
  </div>
</template>

<style scoped>
.chart-box {
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chart-canvas {
  width: 100%;
  height: 100%;
}
</style>
