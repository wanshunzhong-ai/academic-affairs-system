<template>
  <div class="layout" :class="{ collapsed: appStore.sidebarCollapsed }">
    <!-- 侧边栏 -->
    <aside class="layout-sidebar">
      <div class="logo">
        <span class="logo-icon">🎓</span>
        <transition name="fade">
          <span v-show="!appStore.sidebarCollapsed" class="logo-text">教务管理系统</span>
        </transition>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="#1e293b"
          text-color="#cbd5e1"
          active-text-color="#ffffff"
          unique-opened
          router
        >
          <template v-for="menu in userStore.menus" :key="menu.id">
            <!-- 目录 -->
            <el-sub-menu v-if="menu.children && menu.children.length" :index="String(menu.id)">
              <template #title>
                <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
                <span>{{ menu.menuName }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.id"
                :index="child.path"
              >
                <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
                <template #title>
                  <span>{{ child.menuName }}</span>
                  <el-badge
                    v-if="badgeOf(child.perms) > 0"
                    :value="badgeOf(child.perms)"
                    class="menu-badge"
                  />
                </template>
              </el-menu-item>
            </el-sub-menu>
            <!-- 单级菜单 -->
            <el-menu-item v-else :index="menu.path">
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <template #title>
                <span>{{ menu.menuName }}</span>
                <el-badge
                  v-if="badgeOf(menu.perms) > 0"
                  :value="badgeOf(menu.perms)"
                  class="menu-badge"
                />
              </template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </aside>

    <!-- 主区域 -->
    <div class="layout-main">
      <header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, idx) in breadcrumbs" :key="idx">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag :type="roleTagType" effect="dark" size="large" class="role-tag">
            {{ userStore.roleName }}
          </el-tag>
          <el-tooltip content="当前学期" placement="bottom">
            <span class="semester-text">{{ appStore.currentSemesterName || '未设置学期' }}</span>
          </el-tooltip>

          <el-badge :value="userStore.badges.noticeUnread" :hidden="!userStore.badges.noticeUnread">
            <el-icon class="header-icon" @click="goNotice"><Bell /></el-icon>
          </el-badge>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><UserFilled /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <el-scrollbar class="layout-content">
        <div class="app-container">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <keep-alive :max="10">
                <component :is="Component" :key="route.fullPath" />
              </keep-alive>
            </transition>
          </router-view>
        </div>
      </el-scrollbar>
    </div>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="440px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { authApi } from '@/api'
import { resetDynamicRoutes } from '@/router'
import { LOGIN_PATH, roleTagTypeOf } from '@/utils/loginRoles'

defineOptions({ name: 'Layout' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 退出登录 / 改密后回到唯一登录入口 */
function loginEntryPath() {
  return LOGIN_PATH
}
const appStore = useAppStore()

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  return matched.map((r) => r.meta.title)
})

/** 顶部身份标签配色，直接取自身份配置（loginRoles.js），避免两处维护 */
const roleTagType = computed(() => roleTagTypeOf(userStore.roleCode))

const avatarText = computed(() => {
  const name = userStore.realName || ''
  return name.length > 1 ? name.slice(-2) : name
})

function badgeOf(perms) {
  if (perms === 'leave:approve') return userStore.badges.leavePending
  if (perms === 'notice:list') return userStore.badges.noticeUnread
  return 0
}

function goNotice() {
  const target = userStore.hasPerm('notice:manage') ? '/notice/manage' : '/notice/list'
  router.push(target)
}

/* ---------------- 修改密码 ---------------- */
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度 6-32 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submitPassword() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    const entry = loginEntryPath()
    await userStore.logout()
    resetDynamicRoutes()
    router.push(entry)
  } finally {
    pwdLoading.value = false
  }
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    pwdForm.value = { oldPassword: '', newPassword: '', confirm: '' }
    pwdVisible.value = true
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        const entry = loginEntryPath()
        await userStore.logout()
        resetDynamicRoutes()
        router.push(entry)
        ElMessage.success('已安全退出')
      })
      .catch(() => {})
  }
}

onMounted(() => {
  appStore.loadSemesters().catch(() => {})
  userStore.refreshBadges()
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---------- 侧边栏 ---------- */
.layout-sidebar {
  width: var(--aas-sidebar-w);
  background: var(--aas-sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  flex-shrink: 0;
}

.layout.collapsed .layout-sidebar {
  width: var(--aas-sidebar-w-collapsed);
}

.logo {
  height: var(--aas-header-h);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  flex-shrink: 0;
  overflow: hidden;
}

.logo-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.logo-text {
  white-space: nowrap;
}

.menu-scroll {
  flex: 1;
  overflow-x: hidden;
}

.layout-sidebar :deep(.el-menu) {
  border-right: none;
}

.layout-sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2563eb, #1d4ed8) !important;
  border-right: 3px solid #60a5fa;
}

.layout-sidebar :deep(.el-menu-item:hover),
.layout-sidebar :deep(.el-sub-menu__title:hover) {
  background-color: var(--aas-sidebar-bg-light) !important;
}

.layout.collapsed .logo {
  justify-content: center;
  padding: 0;
}

.menu-badge {
  margin-left: 6px;
  vertical-align: middle;
}

/* ---------- 主区域 ---------- */
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.layout-header {
  height: var(--aas-header-h);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.collapse-btn {
  font-size: 19px;
  cursor: pointer;
  color: #475569;
}

.collapse-btn:hover {
  color: var(--aas-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-shrink: 0;
}

.role-tag {
  font-weight: 500;
}

.semester-text {
  font-size: 13px;
  color: var(--aas-text-secondary);
  white-space: nowrap;
}

.header-icon {
  font-size: 19px;
  cursor: pointer;
  color: #475569;
}

.header-icon:hover {
  color: var(--aas-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: var(--aas-primary);
  font-size: 13px;
}

.user-name {
  font-size: 14px;
  color: var(--aas-text);
}

.layout-content {
  flex: 1;
  background: var(--aas-bg);
  overflow-x: hidden;
}

/* ---------- 过渡 ---------- */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .semester-text {
    display: none;
  }
  .user-name {
    display: none;
  }
}
</style>
