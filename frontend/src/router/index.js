import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { LOGIN_PATH, HOME_PATH } from '@/utils/loginRoles'
import Layout from '@/layout/index.vue'

const viewModules = import.meta.glob('../views/**/*.vue')

const LoginView = () => import('@/views/login/index.vue')

export const constantRoutes = [
  {
    path: LOGIN_PATH,
    name: 'Login',
    component: LoginView,
    meta: { title: '登录' }
  },
  // 系统只有一个登录入口 /login。
  // 旧版遗留的 /login/student、/login/headteacher、/login/academic、/login/admin
  // （以及任何未定义的登录子路径）统一回落到 /login，老书签/老收藏不会失效。
  {
    path: `${LOGIN_PATH}/:rest(.*)`,
    redirect: LOGIN_PATH
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无访问权限' }
  }
]

/** 是否为登录页（含已废弃的 /login/xxx 子路径） */
export function isLoginPath(path) {
  return path === LOGIN_PATH || path.startsWith(`${LOGIN_PATH}/`)
}

export const layoutRoute = {
  path: '/',
  name: 'Layout',
  component: Layout,
  redirect: HOME_PATH,
  children: []
}

const router = createRouter({
  history: createWebHistory(),
  routes: [...constantRoutes, layoutRoute],
  scrollBehavior: () => ({ top: 0 })
})

let dynamicAdded = false
/** 记录已动态注册的路由名，便于退出登录 / 切换账号时彻底移除 */
let addedRouteNames = []

/** 由后端菜单生成路由 */
function buildRoutes(menus) {
  const routes = []
  const walk = (list) => {
    (list || []).forEach((menu) => {
      if (menu.menuType === 'C' && menu.path && menu.component) {
        const key = `../views/${menu.component}.vue`
        const loader = viewModules[key]
        routes.push({
          path: menu.path,
          name: `Menu_${menu.id}`,
          component: loader || (() => import('@/views/error/404.vue')),
          meta: {
            title: menu.menuName,
            icon: menu.icon,
            perm: menu.perms,
            menuId: menu.id
          }
        })
      }
      if (menu.children && menu.children.length) {
        walk(menu.children)
      }
    })
  }
  walk(menus)
  return routes
}

export function addDynamicRoutes(menus) {
  if (dynamicAdded) return
  const routes = buildRoutes(menus)
  routes.forEach((route) => {
    if (!router.hasRoute(route.name)) {
      router.addRoute('Layout', route)
      addedRouteNames.push(route.name)
    }
  })
  if (!router.hasRoute('NotFound')) {
    router.addRoute({
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
      meta: { title: '页面不存在' }
    })
    addedRouteNames.push('NotFound')
  }
  dynamicAdded = true
}

/** 退出登录 / 切换账号时移除全部动态路由，避免上一角色的菜单残留 */
export function resetDynamicRoutes() {
  addedRouteNames.forEach((name) => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  addedRouteNames = []
  dynamicAdded = false
}

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (isLoginPath(to.path)) {
    if (userStore.token && userStore.loaded) {
      return next('/')
    }
    return next()
  }

  if (!userStore.token) {
    return next(`${LOGIN_PATH}?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  if (!userStore.loaded) {
    try {
      const info = await userStore.fetchInfo()
      addDynamicRoutes(info.menus)
      return next({ ...to, replace: true })
    } catch (e) {
      // 信息拉取失败（token 失效等）：清登录态，回到唯一登录入口
      userStore.reset()
      resetDynamicRoutes()
      return next(LOGIN_PATH)
    }
  }

  if (to.meta?.perm && !userStore.hasPerm(to.meta.perm)) {
    return next('/403')
  }

  return next()
})

router.afterEach((to) => {
  const base = import.meta.env.VITE_APP_TITLE || '教务管理系统'
  document.title = to.meta?.title ? `${to.meta.title} - ${base}` : base
})

export default router
