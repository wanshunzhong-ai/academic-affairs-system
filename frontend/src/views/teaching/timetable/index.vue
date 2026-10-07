<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { classApi, offeringApi, semesterApi, studentApi, teacherApi } from '@/api'
import { useUserStore } from '@/store/user'
import TimetableGrid from '@/components/TimetableGrid.vue'
import { fixed } from '@/utils/format'

const userStore = useUserStore()

const isStudent = computed(() => userStore.isStudent)
const isHeadTeacher = computed(() => userStore.isHeadTeacher)
/** 可自由切换查看对象：管理员 / 教务处 / 班主任 */
const canSwitch = computed(() => userStore.isAdmin || userStore.isAcademic || userStore.isHeadTeacher)

const semesters = ref([])
const semesterId = ref(null)
const loading = ref(false)
const offers = ref([])

/** 查询对象类型：class | teacher | student */
const scopeType = ref('class')
const classes = ref([])
const teachers = ref([])
const students = ref([])
const targetId = ref(null)

const semesterName = computed(
  () => semesters.value.find((s) => s.id === semesterId.value)?.semesterName || '-'
)

/** 汇总统计 */
const summary = computed(() => {
  const totalCredit = offers.value.reduce((sum, o) => sum + Number(o.credit || 0), 0)
  const names = new Set(offers.value.map((o) => o.courseName))
  const hours = offers.value.reduce((sum, o) => sum + Number(o.hours || 0), 0)
  return { courseCount: names.size, totalCredit: fixed(totalCredit, 1), hours }
})

async function loadSemesters() {
  const res = await semesterApi.list()
  semesters.value = res.data || []
  const cur = semesters.value.find((i) => i.isCurrent === 1) || semesters.value[0]
  semesterId.value = cur?.id || null
}

async function loadOptions() {
  if (isStudent.value) return
  const tasks = [classApi.options(), teacherApi.options(), studentApi.options(''), classApi.my()]
  const [c, t, s, my] = await Promise.all(tasks)
  classes.value = c.data || []
  teachers.value = t.data || []
  students.value = s.data || []

  if (isHeadTeacher.value) {
    // 班主任默认看自己的班
    scopeType.value = 'class'
    const mine = my.data || []
    targetId.value = mine[0]?.id || null
  }
}

async function loadTimetable() {
  if (!semesterId.value) {
    offers.value = []
    return
  }
  loading.value = true
  try {
    // 学生只看自己的课表
    if (isStudent.value) {
      const res = await offeringApi.myTimetable(semesterId.value)
      offers.value = res.data || []
      return
    }
    if (!targetId.value) {
      offers.value = []
      return
    }
    if (scopeType.value === 'class') {
      const res = await offeringApi.classTimetable(targetId.value, semesterId.value)
      offers.value = res.data || []
    } else if (scopeType.value === 'teacher') {
      const res = await offeringApi.teacherTimetable(targetId.value, semesterId.value)
      offers.value = res.data || []
    } else {
      const res = await offeringApi.studentTimetable(targetId.value, semesterId.value)
      offers.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

watch(semesterId, loadTimetable)
watch([scopeType, targetId], loadTimetable)

const courseColumns = [
  { prop: 'courseName', label: '课程名称', minWidth: 170 },
  { prop: 'offeringCode', label: '开课代码', width: 110 },
  { prop: 'teacherName', label: '授课教师', width: 100 },
  { prop: 'className', label: '上课班级', minWidth: 170 },
  { prop: 'credit', label: '学分', width: 80, align: 'center' },
  { prop: 'scheduleText', label: '上课时间地点', minWidth: 230 }
]

onMounted(async () => {
  await loadSemesters()
  await loadOptions()
  await loadTimetable()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline @submit.prevent style="margin-bottom: 0">
          <el-form-item label="学期">
            <el-select v-model="semesterId" placeholder="选择学期" style="width: 220px">
              <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
            </el-select>
          </el-form-item>

          <template v-if="canSwitch">
            <el-form-item label="查看对象">
              <el-radio-group v-model="scopeType">
                <el-radio-button value="class">班级</el-radio-button>
                <el-radio-button value="teacher">教师</el-radio-button>
                <el-radio-button value="student">学生</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-select
                v-if="scopeType === 'class'"
                v-model="targetId"
                placeholder="选择班级"
                filterable
                style="width: 230px"
              >
                <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
              </el-select>
              <el-select
                v-else-if="scopeType === 'teacher'"
                v-model="targetId"
                placeholder="选择教师"
                filterable
                style="width: 230px"
              >
                <el-option v-for="t in teachers" :key="t.id" :label="`${t.name}（${t.teacherNo}）`" :value="t.id" />
              </el-select>
              <el-select
                v-else
                v-model="targetId"
                placeholder="选择学生"
                filterable
                style="width: 230px"
              >
                <el-option
                  v-for="s in students"
                  :key="s.id"
                  :label="`${s.name}（${s.studentNo}）`"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </template>
        </el-form>
      </div>

      <el-alert v-if="isStudent" type="info" :closable="false" show-icon class="mb-16">
        <template #title>以下为你在「{{ semesterName }}」的个人课表。</template>
      </el-alert>
      <el-alert
        v-else-if="canSwitch && !targetId"
        type="warning"
        :closable="false"
        show-icon
        class="mb-16"
      >
        <template #title>请先选择要查看的班级 / 教师 / 学生。</template>
      </el-alert>

      <div v-loading="loading" class="timetable-wrap">
        <el-empty v-if="!offers.length" description="暂无课表数据" :image-size="100" />
        <TimetableGrid v-else :data="offers" />
      </div>

      <div class="tt-summary">
        <span>课程门数：<b>{{ summary.courseCount }}</b></span>
        <span>总学分：<b>{{ summary.totalCredit }}</b></span>
        <span>总学时：<b>{{ summary.hours }}</b></span>
      </div>
    </el-card>

    <el-card v-if="offers.length" shadow="never" class="mt-16">
      <template #header><div class="card-head"><span>课程明细</span></div></template>
      <el-table :data="offers" stripe border size="small">
        <el-table-column
          v-for="col in courseColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'left'"
          show-overflow-tooltip
        />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.timetable-wrap {
  overflow-x: auto;
}
.tt-summary {
  display: flex;
  gap: 28px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--aas-border);
  font-size: 13px;
  color: var(--aas-text-secondary);
}
.tt-summary b {
  color: var(--aas-primary);
  font-size: 15px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
