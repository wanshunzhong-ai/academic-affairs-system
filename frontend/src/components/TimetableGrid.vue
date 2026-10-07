<script setup>
import { computed } from 'vue'
import { WEEK_DAY_OPTIONS, SECTION_TIME } from '@/utils/dict'

const props = defineProps({
  /** 开课列表，每项需带 schedules 数组 */
  data: { type: Array, default: () => [] },
  /** 显示到第几节 */
  maxSection: { type: Number, default: 10 }
})

/** 颜色池：按课程名散列取色，保证同一门课颜色稳定 */
const PALETTE = [
  { bg: '#eff6ff', border: '#2563eb', name: '#1e3a8a' },
  { bg: '#ecfdf5', border: '#059669', name: '#065f46' },
  { bg: '#fef3c7', border: '#d97706', name: '#92400e' },
  { bg: '#fce7f3', border: '#db2777', name: '#9d174d' },
  { bg: '#ede9fe', border: '#7c3aed', name: '#5b21b6' },
  { bg: '#e0f2fe', border: '#0284c7', name: '#075985' },
  { bg: '#fee2e2', border: '#dc2626', name: '#991b1b' },
  { bg: '#f0fdf4', border: '#16a34a', name: '#166534' }
]

function colorOf(name) {
  if (!name) return PALETTE[0]
  let hash = 0
  for (let i = 0; i < name.length; i += 1) hash = (hash * 31 + name.charCodeAt(i)) % 9973
  return PALETTE[hash % PALETTE.length]
}

/** 每天一张行表：rows[节次] = { blocks, span } | null（null 表示被上一行的 rowspan 覆盖） */
const gridByDay = computed(() => {
  const result = {}
  WEEK_DAY_OPTIONS.forEach(({ value: day }) => {
    const rows = new Array(props.maxSection + 1).fill(null)
    const starts = {}

    ;(props.data || []).forEach((offering) => {
      ;(offering.schedules || []).forEach((s) => {
        if (s.weekDay !== day) return
        const st = Number(s.startSection)
        if (!st || st > props.maxSection) return
        const en = Math.min(Number(s.endSection) || st, props.maxSection)
        if (!starts[st]) starts[st] = []
        starts[st].push({
          offering,
          schedule: s,
          color: colorOf(offering.courseName),
          span: Math.max(1, en - st + 1),
          weekText: s.startWeek && s.endWeek ? `第${s.startWeek}-${s.endWeek}周` : ''
        })
      })
    })

    const covered = new Set()
    Object.keys(starts)
      .map(Number)
      .sort((a, b) => a - b)
      .forEach((st) => {
        if (covered.has(st)) return
        const blocks = starts[st]
        const span = Math.max(...blocks.map((b) => b.span))
        rows[st] = { blocks, span }
        for (let k = 1; k < span; k += 1) covered.add(st + k)
      })

    result[day] = rows
  })
  return result
})

const dayOptions = WEEK_DAY_OPTIONS
const sections = computed(() => Array.from({ length: props.maxSection }, (_, i) => i + 1))

/** 某天的某节 -> 单元格数据（null 代表跳过） */
function cellOf(day, section) {
  return gridByDay.value[day]?.[section] || null
}

/** 统计某天是否有课 */
function hasCourseOn(day) {
  return gridByDay.value[day]?.some((r) => r) || false
}
</script>

<template>
  <table class="timetable-table">
    <thead>
      <tr>
        <th class="section-head">节次</th>
        <th v-for="d in dayOptions" :key="d.value" :class="{ 'day-empty': !hasCourseOn(d.value) }">
          {{ d.label }}
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="sec in sections" :key="sec">
        <td class="section-cell">
          <div class="sec-no">第{{ sec }}节</div>
          <div class="sec-time">{{ SECTION_TIME[sec] || '' }}</div>
        </td>
        <template v-for="d in dayOptions" :key="d.value">
          <td
            v-if="cellOf(d.value, sec)"
            :rowspan="cellOf(d.value, sec).span"
            class="has-course"
          >
            <div
              v-for="(b, i) in cellOf(d.value, sec).blocks"
              :key="i"
              class="course-block"
              :style="{ background: b.color.bg, borderLeftColor: b.color.border }"
            >
              <div class="cb-name" :style="{ color: b.color.name }">{{ b.offering.courseName }}</div>
              <div class="cb-info">{{ b.offering.teacherName || '-' }}</div>
              <div class="cb-info">{{ b.schedule.classroomName || '-' }}</div>
              <div v-if="b.weekText" class="cb-info cb-week">{{ b.weekText }}</div>
            </div>
          </td>
        </template>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.section-head {
  width: 92px;
}
.sec-no {
  font-size: 12px;
  font-weight: 600;
}
.sec-time {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 2px;
}
td.has-course {
  background: #fbfdff;
}
.cb-week {
  color: #94a3b8;
}
th.day-empty {
  color: #94a3b8;
}
</style>
