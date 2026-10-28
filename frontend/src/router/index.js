import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { title: '账号登录' } },
  { path: '/forgot', name: 'forgot', component: () => import('../views/ForgotPasswordView.vue'), meta: { title: '找回密码' } },
  { path: '/forgot-password', redirect: '/forgot' },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { title: '注册账号' } },
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '交流广场' } },
  { path: '/article/:id', name: 'detail', component: () => import('../views/DetailView.vue'), meta: { title: '帖子详情' } },
  { path: '/post', name: 'post', component: () => import('../views/PostView.vue'), meta: { requiresAuth: true, title: '发布新帖' } },
  { path: '/article/:id/edit', redirect: (to) => ({ path: '/post', query: { id: to.params.id } }) },
  { path: '/profile/:id?', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true, title: '个人主页' } },
  { path: '/collections', name: 'collections', component: () => import('../views/CollectionsView.vue'), meta: { requiresAuth: true, title: '我的收藏' } },
  { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue'), meta: { requiresAuth: true, title: '账号设置' } },
  { path: '/messages', name: 'messages', component: () => import('../views/MessagesView.vue'), meta: { requiresAuth: true, title: '站内私信' } },
  { path: '/admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true, title: '管理控制台' } },
  { path: '/:pathMatch(.*)*', name: 'notfound', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior () {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  if (!store.loaded) {
    await store.load()
  }
  if (to.meta.requiresAuth) {
    if (!store.isLogin) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAdmin && !store.isAdmin) {
      return { path: '/' }
    }
  }
  return true
})

export default router
