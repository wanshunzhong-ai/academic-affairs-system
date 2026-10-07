<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi, studentApi, teacherApi } from '@/api'
import { useUserStore } from '@/store/user'
import { fixed, genderText, scoreColor, studentStatusText, studentStatusType } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const info = ref({})
const student = ref(null)
const teacher = ref(null)
const summary = ref({})

const isStudent = computed(() => (info.value.roleCode || userStore.roleCode) === 'STUDENT')
const isTeacher = computed(() =>
  ['HEAD_TEACHER', 'TEACHER'].includes(info.value.roleCode || userStore.roleCode)
)

async function load() {
  loading.value = true
  try {
    const res = await authApi.getInfo()
    info.value = res.data || {}
    if (isStudent.value) {
      const [s, sum] = await Promise.all([
        studentApi.my().catch(() => ({ data: null })),
        studentApi.mySummary().catch(() => ({ data: {} }))
      ])
      student.value = s.data
      summary.value = sum.data || {}
    } else if (isTeacher.value) {
      const t = await teacherApi.my().catch(() => ({ data: null }))
      teacher.value = t.data
    }
  } finally {
    loading.value = false
  }
}

/* ==================== 修改密码 ==================== */
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function openChangePwd() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdVisible.value = true
}

async function submitPwd() {
  await pwdFormRef.value.validate()
  pwdSubmitting.value = true
  try {
    const res = await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      pwdVisible.value = false
      setTimeout(() => {
        userStore.logout().then(() => window.location.reload())
      }, 800)
    }
  } finally {
    pwdSubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <el-card v-loading="loading" shadow="never" class="mb-16">
          <div class="profile-head">
            <div class="ph-avatar">{{ (info.realName || '?').slice(0, 1) }}</div>
            <div class="ph-name">{{ info.realName }}</div>
            <el-tag effect="dark" color="#2563eb" style="border: none">{{ info.roleName }}</el-tag>
            <div class="ph-sub">{{ info.username }}</div>
          </div>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="登录账号">{{ info.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ info.realName }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ info.roleName }}（{{ info.roleCode }}）</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ info.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ info.email || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="info.studentNo" label="学号">{{ info.studentNo }}</el-descriptions-item>
            <el-descriptions-item v-if="info.className" label="班级">{{ info.className }}</el-descriptions-item>
            <el-descriptions-item v-if="info.teacherNo" label="工号">{{ info.teacherNo }}</el-descriptions-item>
            <el-descriptions-item v-if="info.deptName" label="所属院系">{{ info.deptName }}</el-descriptions-item>
            <el-descriptions-item v-if="info.manageClassNames?.length" label="带班班级">
              {{ info.manageClassNames.join('、') }}
            </el-descriptions-item>
            <el-descriptions-item v-if="info.teachingCount != null" label="授课门数">
              {{ info.teachingCount }} 门
            </el-descriptions-item>
            <el-descriptions-item label="当前学期">{{ info.currentSemester || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="pwd-btn">
            <el-button type="primary" plain style="width: 100%" @click="openChangePwd">
              <el-icon><Lock /></el-icon>修改登录密码
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <!-- 学生档案 -->
        <el-card v-if="isStudent && student" v-loading="loading" shadow="never" class="mb-16">
          <template #header>
            <div class="card-head">
              <span>我的学籍信息</span>
              <el-tag :type="studentStatusType(student.status)" size="small" effect="plain">
                {{ studentStatusText(student.status) }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="学号">{{ student.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ student.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText(student.gender) }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ student.birthDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="院系">{{ student.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ student.majorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ student.className || '-' }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ student.grade || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入学日期">{{ student.enrollmentDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="政治面貌">{{ student.politicalStatus || '-' }}</el-descriptions-item>
            <el-descriptions-item label="宿舍">{{ student.dormitory || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ student.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="监护人">{{ student.guardianName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="监护人电话">{{ student.guardianPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="家庭住址" :span="2">{{ student.address || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 学业概况 -->
        <el-card v-if="isStudent" v-loading="loading" shadow="never" class="mb-16">
          <template #header><div class="card-head"><span>学业概况</span></div></template>
          <div class="mini-grid">
            <div class="mg-item">
              <div class="mg-val">{{ summary.courseCount ?? 0 }}</div>
              <div class="mg-label">已修课程（门）</div>
            </div>
            <div class="mg-item">
              <div class="mg-val" :style="{ color: scoreColor(summary.avgScore) }">
                {{ fixed(summary.avgScore, 2) }}
              </div>
              <div class="mg-label">平均成绩</div>
            </div>
            <div class="mg-item">
              <div class="mg-val" style="color: #7c3aed">{{ fixed(summary.avgPoint, 2) }}</div>
              <div class="mg-label">平均绩点</div>
            </div>
            <div class="mg-item">
              <div class="mg-val" style="color: #0ea5e9">{{ fixed(summary.earnedCredit, 1) }}</div>
              <div class="mg-label">已获学分</div>
            </div>
            <div class="mg-item">
              <div class="mg-val" style="color: #16a34a">{{ fixed(summary.maxScore, 1) }}</div>
              <div class="mg-label">最高分</div>
            </div>
            <div class="mg-item">
              <div class="mg-val text-danger">{{ summary.failCount ?? 0 }}</div>
              <div class="mg-label">不及格课程</div>
            </div>
          </div>
        </el-card>

        <!-- 教师档案 -->
        <el-card v-if="isTeacher && teacher" v-loading="loading" shadow="never" class="mb-16">
          <template #header><div class="card-head"><span>我的教师档案</span></div></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="工号">{{ teacher.teacherNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ teacher.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText(teacher.gender) }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ teacher.birthDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属院系">{{ teacher.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ teacher.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ teacher.education || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ teacher.hireDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ teacher.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ teacher.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否班主任">
              {{ teacher.isHeadTeacher === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="带班班级">{{ teacher.headClassNames || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 权限说明 -->
        <el-card v-loading="loading" shadow="never">
          <template #header>
            <div class="card-head">
              <span>我的权限（{{ (info.permissions || []).length }} 项）</span>
            </div>
          </template>
          <div class="perm-wrap">
            <el-tag
              v-for="p in info.permissions || []"
              :key="p"
              size="small"
              effect="plain"
              type="info"
              class="perm-tag"
            >
              {{ p }}
            </el-tag>
            <el-empty v-if="!(info.permissions || []).length" description="暂无权限标识" :image-size="70" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改登录密码" width="480px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="96px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位字符" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            @keyup.enter="submitPwd"
          />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>修改成功后需要重新登录。</template>
      </el-alert>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-head {
  text-align: center;
  padding: 8px 0 20px;
  border-bottom: 1px solid var(--aas-border);
  margin-bottom: 16px;
}
.ph-avatar {
  width: 68px;
  height: 68px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ph-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.ph-sub {
  font-size: 12.5px;
  color: var(--aas-text-secondary);
  margin-top: 8px;
}
.pwd-btn {
  margin-top: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mini-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 12px;
}
.mg-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}
.mg-val {
  font-size: 21px;
  font-weight: 600;
}
.mg-label {
  font-size: 12px;
  color: var(--aas-text-secondary);
  margin-top: 4px;
}
.perm-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.perm-tag {
  font-family: Consolas, Monaco, monospace;
}
</style>
