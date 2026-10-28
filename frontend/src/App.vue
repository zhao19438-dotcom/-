<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Search,
  Bell,
  Top,
  User,
  Setting,
  Collection,
  SwitchButton,
  Management,
  Compass,
  ChatDotRound,
  ChatLineRound,
  Menu,
  ArrowDown,
  Plus
} from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'
import { userApi } from './api/user'
import { messageApi } from './api/message'
import { useBoardStore } from './stores/boards'
import { displayName } from './utils/format'
import AppNavList from './components/AppNavList.vue'
import UserAvatar from './components/UserAvatar.vue'

const UNREAD_POLL_MS = 30000

const store = useUserStore()
const boardStore = useBoardStore()
const BOARD_ANNOUNCEMENT = computed(() => boardStore.announcement ? String(boardStore.announcement.id) : '')
const BOARD_LOST_FOUND = computed(() => boardStore.lostFound ? String(boardStore.lostFound.id) : '')
const router = useRouter()
const route = useRoute()

const keyword = ref('')
const unread = ref(0)
const showTop = ref(false)
const mobileMenu = ref(false)
let unreadTimer = null

function onScroll () {
  showTop.value = window.scrollY > 300
}
function onAuthExpired () {
  store.clear()
  unread.value = 0
}
function backToTop () {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function skipToMain () {
  const el = document.getElementById('main-content')
  if (el) el.focus()
}

const user = computed(() => store.userInfo)
const isLogin = computed(() => store.isLogin)
const isAdmin = computed(() => store.isAdmin)

const currentTitle = computed(() => {
  if (route.path === '/' && route.query.boardId === BOARD_LOST_FOUND.value) return '失物招领'
  if (route.path === '/' && route.query.boardId === BOARD_ANNOUNCEMENT.value) return '官方公告'
  if (route.path === '/' && route.query.keyword) return '搜索结果'
  if (route.path === '/post' && route.query.id) return '编辑帖子'
  return route.meta.title || '交流广场'
})

const isAuthPage = computed(() => route.name === 'login' || route.name === 'register' || route.name === 'forgot')
const isAdminPage = computed(() => route.path.startsWith('/admin'))

const exploreNav = computed(() => [
  { label: '交流广场', icon: Compass, to: '/', active: () => route.path === '/' && !route.query.boardId },
  ...(BOARD_LOST_FOUND.value ? [{ label: '失物招领', icon: ChatLineRound, to: `/?boardId=${BOARD_LOST_FOUND.value}`, active: () => route.query.boardId === BOARD_LOST_FOUND.value }] : []),
  ...(BOARD_ANNOUNCEMENT.value ? [{ label: '官方公告', icon: Bell, to: `/?boardId=${BOARD_ANNOUNCEMENT.value}`, active: () => route.query.boardId === BOARD_ANNOUNCEMENT.value }] : [])
])
const mineNav = computed(() => [
  { label: '个人主页', icon: User, to: '/profile', active: () => route.path.startsWith('/profile') },
  { label: '我的收藏', icon: Collection, to: '/collections', active: () => route.path === '/collections' },
  { label: '站内私信', icon: ChatDotRound, to: '/messages', active: () => route.path === '/messages', badge: unread.value },
  { label: '账号设置', icon: Setting, to: '/settings', active: () => route.path === '/settings' }
])
const adminNav = computed(() => [
  { label: '管理控制台', icon: Management, to: '/admin', active: () => route.path === '/admin' }
])

function navTo (to) {
  mobileMenu.value = false
  router.push(to)
}

async function loadUnread () {
  if (!store.isLogin) {
    unread.value = 0
    return
  }
  try {
    unread.value = await messageApi.unreadCount()
  } catch (e) {
    unread.value = 0
  }
}

function goSearch () {
  const q = keyword.value.trim()
  if (!q) return
  router.push({ path: '/', query: { boardId: route.path === '/' ? route.query.boardId : undefined, keyword: q } })
}

function logout () {
  userApi.logout().finally(() => {
    store.clear()
    router.push('/login')
  })
}

function onCommand (cmd) {
  if (cmd === 'logout') logout()
  else if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'collections') router.push('/collections')
  else if (cmd === 'settings') router.push('/settings')
  else if (cmd === 'admin') router.push('/admin')
  else if (cmd === 'login') router.push('/login')
}

watch(() => route.query.keyword, (val) => {
  keyword.value = val ? String(val) : ''
}, { immediate: true })

watch(() => currentTitle.value, (title) => {
  document.title = `${title} · 校园论坛`
}, { immediate: true })

watch(() => store.isLogin, loadUnread)
watch(() => route.fullPath, () => {
  loadUnread()
  mobileMenu.value = false
})

onMounted(() => {
  refreshBoards()
  window.addEventListener('boards:changed', refreshBoards)
  loadUnread()
  unreadTimer = setInterval(loadUnread, UNREAD_POLL_MS)
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('auth:expired', onAuthExpired)
  window.addEventListener('messages:changed', loadUnread)
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('boards:changed', refreshBoards)
  if (unreadTimer) clearInterval(unreadTimer)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('auth:expired', onAuthExpired)
  window.removeEventListener('messages:changed', loadUnread)
})
async function refreshBoards () {
  try { await boardStore.load(true) } catch { /* 请求错误由统一拦截器显示。 */ }
}
</script>

<template>
  <router-view v-if="isAuthPage" v-slot="{ Component }">
    <transition name="fade-slide" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>

  <!-- 管理员控制台：采用专用独立工作台布局（左侧导航 + 顶部工具区 + 主面板） -->
  <router-view v-else-if="isAdminPage" v-slot="{ Component }">
    <component :is="Component" />
  </router-view>

  <!-- 学生端页面：顶部导航栏 + 1200px 居中内容区 -->
  <div v-else class="student-shell">
    <button class="skip-link" type="button" @click="skipToMain">跳到主要内容</button>

    <!-- 顶部常驻导航栏 -->
    <header class="site-navbar">
      <div class="site-navbar-inner">
        <!-- 左侧：品牌与频道导航 -->
        <div class="site-navbar-left">
          <button class="icon-button mobile-menu-btn" type="button" title="打开导航菜单" @click="mobileMenu = true">
            <el-icon><Menu /></el-icon>
          </button>

          <router-link class="nav-brand" to="/" aria-label="校园论坛首页">
            <div class="brand-mark">
              <span></span><span></span><span></span>
            </div>
            <strong>校园论坛</strong>
          </router-link>

          <nav class="desktop-nav-links">
            <router-link
              to="/"
              class="nav-link-item"
              :class="{ active: route.path === '/' && !route.query.boardId }"
            >
              交流广场
            </router-link>
            <router-link
              v-if="BOARD_LOST_FOUND"
              :to="`/?boardId=${BOARD_LOST_FOUND}`"
              class="nav-link-item"
              :class="{ active: route.query.boardId === BOARD_LOST_FOUND }"
            >
              失物招领
            </router-link>
            <router-link
              v-if="BOARD_ANNOUNCEMENT"
              :to="`/?boardId=${BOARD_ANNOUNCEMENT}`"
              class="nav-link-item"
              :class="{ active: route.query.boardId === BOARD_ANNOUNCEMENT }"
            >
              官方公告
            </router-link>
            <router-link
              v-if="isAdmin"
              to="/admin"
              class="nav-link-item nav-admin-link"
            >
              <el-icon><Management /></el-icon>
              <span>管理后台</span>
            </router-link>
          </nav>
        </div>

        <!-- 中部：搜索框 -->
        <div class="site-navbar-center">
          <el-input
            v-model="keyword"
            placeholder="搜索讨论或资料…"
            aria-label="搜索讨论"
            size="small"
            clearable
            :prefix-icon="Search"
            @keyup.enter="goSearch"
            @clear="router.push({ path: '/', query: { ...route.query, keyword: undefined, page: undefined } })"
          />
        </div>

        <!-- 右侧：发帖、消息与用户菜单 -->
        <div class="site-navbar-right">
          <button
            v-if="isLogin"
            class="topbar-post-btn"
            type="button"
            @click="router.push('/post')"
          >
            <el-icon><Plus /></el-icon>
            <span>发帖</span>
          </button>

          <button
            v-if="isLogin"
            class="icon-button notice-button"
            type="button"
            title="查看私信与通知"
            @click="router.push('/messages')"
          >
            <el-icon><Bell /></el-icon>
            <span v-if="unread > 0" class="unread-dot"></span>
          </button>

          <el-dropdown v-if="isLogin" trigger="click" @command="onCommand">
            <div class="user-chip">
              <UserAvatar :user="user" size="small" fallback="学" />
              <div class="user-chip-copy">
                <strong>{{ user ? displayName(user) : '用户' }}</strong>
                <small>{{ isAdmin ? '管理员' : (user?.college || '在校学生') }}</small>
              </div>
              <el-icon class="chevron"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人主页</el-dropdown-item>
                <el-dropdown-item command="collections" :icon="Collection">我的收藏</el-dropdown-item>
                <el-dropdown-item command="settings" :icon="Setting">账号设置</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin" :icon="Management">管理控制台</el-dropdown-item>
                <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <div v-else class="auth-btn-group">
            <button class="nav-btn-link" type="button" @click="router.push('/login')">登录</button>
            <button class="nav-btn-primary" type="button" @click="router.push('/register')">注册</button>
          </div>
        </div>
      </div>
    </header>

    <!-- 移动端侧栏抽屉 -->
    <el-drawer
      v-model="mobileMenu"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="mobile-nav-drawer"
    >
      <div class="drawer-inner">
        <div class="brand" @click="navTo('/')">
          <div class="brand-mark">
            <span></span><span></span><span></span>
          </div>
          <div>
            <strong>校园论坛</strong>
            <small>Campus Forum</small>
          </div>
        </div>

        <div class="sidebar-section-label">探索社区</div>
        <AppNavList :items="exploreNav" @navigate="navTo" />

        <template v-if="isLogin">
          <div class="sidebar-section-label side-label-spaced">我的参与</div>
          <AppNavList :items="mineNav" @navigate="navTo" />
        </template>

        <div v-if="isAdmin" class="sidebar-admin-block">
          <div class="sidebar-section-label">管理空间</div>
          <AppNavList :items="adminNav" @navigate="navTo" />
        </div>

        <div v-if="!isLogin" class="drawer-login-tip">
          <p>登录后即可发帖、私信与收藏</p>
          <button class="primary-button" style="width: 100%;" type="button" @click="navTo('/login')">立即登录</button>
        </div>
      </div>
    </el-drawer>

    <!-- 主内容区：1200px 居中 -->
    <main id="main-content" class="student-main-content" tabindex="-1">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <div :key="route.path + (route.query.id || '')" class="route-page"><component :is="Component" /></div>
        </transition>
      </router-view>
    </main>

    <!-- 返回顶部浮动按钮 -->
    <transition name="fade">
      <el-button
        v-if="showTop"
        class="floating-backtop"
        circle
        type="primary"
        :icon="Top"
        title="返回顶部"
        @click="backToTop"
      />
    </transition>
  </div>
</template>

<style scoped>
.drawer-inner {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}
.drawer-inner .brand {
  padding: 4px 10px 24px;
}
.drawer-login-tip {
  margin-top: auto;
  padding: 14px 4px 0;
}
.drawer-login-tip p {
  margin: 0 0 10px;
  color: var(--muted);
  font-size: 12px;
}
</style>
