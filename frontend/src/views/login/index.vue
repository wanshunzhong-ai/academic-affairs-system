<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-circle c1"></div>
      <div class="bg-circle c2"></div>
      <div class="bg-circle c3"></div>
    </div>

    <div class="login-wrapper">
      <!-- 左侧介绍 -->
      <div class="login-intro">
        <div class="intro-logo">🎓</div>
        <h1>教务管理系统</h1>
        <p class="intro-sub">Academic Affairs Management System</p>
        <p class="intro-desc">
          全系统只有<strong>一个登录入口</strong>。用账号登录后，系统自动识别身份，
          进入对应的后台 —— 工作台、菜单、可见数据各不相同。
        </p>

        <ul class="role-list">
          <li v-for="r in LOGIN_ROLES" :key="r.key">
            <span class="role-dot" :style="{ background: r.accent }"></span>
            <span class="role-name">{{ r.label }}</span>
            <span class="role-slogan">{{ r.slogan }}</span>
          </li>
        </ul>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="login-card">
        <h2 class="card-title">账号登录</h2>
        <p class="card-sub">学生填学号，班主任填教师工号，教务处 / 管理员填各自账号</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 演示账号一键填充：由后端配置 aas.demo-accounts 控制，默认不展示 -->
        <template v-if="showDemoAccounts">
          <el-divider><span class="divider-text">演示账号（点击填充）</span></el-divider>
          <div class="demo-accounts">
            <div
              v-for="r in LOGIN_ROLES"
              :key="r.key"
              class="demo-item"
              @click="fillAccount(r)"
            >
              <el-tag :type="r.tagType" effect="plain" size="small">{{ r.label }}</el-tag>
              <span class="demo-username">{{ r.demoUsername }}</span>
            </div>
          </div>
        </template>

        <p class="login-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>登录后系统根据账号身份自动进入对应后台，无需选择身份</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { authApi } from '@/api'
import { addDynamicRoutes, resetDynamicRoutes } from '@/router'
import { LOGIN_ROLES, HOME_PATH, roleLabelOf } from '@/utils/loginRoles'

defineOptions({ name: 'LoginPage' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

/** 是否展示演示账号一键填充（后端 aas.demo-accounts 决定，默认关闭） */
const showDemoAccounts = ref(false)

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fillAccount(role) {
  form.username = role.demoUsername
  form.password = '123456'
}

async function loadLoginConfig() {
  try {
    const res = await authApi.loginConfig()
    showDemoAccounts.value = res.data?.demoAccounts === true
  } catch {
    // 拿不到配置就按「不展示演示账号」处理，与默认配置一致
    showDemoAccounts.value = false
  }
}

onMounted(loadLoginConfig)

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const username = form.username.trim()
    await userStore.login({ username, password: form.password })

    // 登录成功后按账号身份装载菜单与权限，进入该身份的后台
    resetDynamicRoutes()
    addDynamicRoutes(userStore.menus)
    appStore.loadSemesters().catch(() => {})

    const label = roleLabelOf(userStore.roleCode) || userStore.roleName
    ElMessage.success(`欢迎回来，${userStore.realName}${label ? '（' + label + '）' : ''}`)

    const redirect = route.query.redirect
    await router.replace(redirect ? decodeURIComponent(redirect) : HOME_PATH)
  } catch (e) {
    /* 校验失败 / 请求错误：提示已在拦截器中处理 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #1e40af 100%);
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(96, 165, 250, 0.12);
  animation: float 14s ease-in-out infinite;
}

.c1 {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -100px;
}

.c2 {
  width: 300px;
  height: 300px;
  bottom: -90px;
  right: -60px;
  animation-delay: 3s;
}

.c3 {
  width: 180px;
  height: 180px;
  top: 45%;
  left: 12%;
  animation-delay: 6s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(24px, -24px) scale(1.08); }
}

.login-wrapper {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 70px;
  padding: 0 40px;
  max-width: 1080px;
  width: 100%;
}

/* ---------- 左侧介绍 ---------- */
.login-intro {
  flex: 1;
  color: #fff;
  min-width: 0;
}

.intro-logo {
  font-size: 54px;
  margin-bottom: 12px;
}

.login-intro h1 {
  font-size: 38px;
  margin: 0 0 8px;
  letter-spacing: 2px;
  font-weight: 600;
}

.intro-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
  margin: 0 0 18px;
}

.intro-desc {
  font-size: 14px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.78);
  margin: 0 0 24px;
  max-width: 430px;
}

.intro-desc strong {
  color: #93c5fd;
  font-weight: 600;
}

.role-list {
  list-style: none;
  padding: 20px 0 0;
  margin: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
  max-width: 430px;
}

.role-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  padding: 7px 0;
  line-height: 1.6;
}

.role-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.role-name {
  font-weight: 600;
  min-width: 48px;
  color: #fff;
}

.role-slogan {
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

/* ---------- 右侧卡片 ---------- */
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 14px;
  padding: 34px 34px 26px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.28);
  flex-shrink: 0;
}

.card-title {
  font-size: 22px;
  margin: 0 0 6px;
  color: #1e293b;
}

.card-sub {
  font-size: 13px;
  color: #94a3b8;
  margin: 0 0 24px;
  line-height: 1.6;
}

.login-btn {
  width: 100%;
  letter-spacing: 2px;
  font-size: 15px;
  height: 42px;
}

.divider-text {
  font-size: 12px;
  color: #94a3b8;
}

.demo-accounts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.demo-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.16s;
}

.demo-item:hover {
  border-color: #2563eb;
  background: #f8fafc;
}

.demo-username {
  font-size: 12.5px;
  color: #475569;
  font-family: Consolas, Monaco, monospace;
}

.login-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  margin: 18px 0 0;
  line-height: 1.6;
}

.login-tip .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .login-intro {
    display: none;
  }
  .login-wrapper {
    justify-content: center;
    padding: 0 20px;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
