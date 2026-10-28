<script setup>
import { computed } from 'vue'
import { DataLine, User, CollectionTag, Document, Warning, Tickets, Lock, ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { useAdminWorkspace } from '../composables/useAdminWorkspace'
import { useUserStore } from '../stores/user'
import { displayName } from '../utils/format'
import UserAvatar from '../components/UserAvatar.vue'
import AdminOverview from '../components/admin/AdminOverview.vue'
import AdminUsers from '../components/admin/AdminUsers.vue'
import AdminBoards from '../components/admin/AdminBoards.vue'
import AdminArticles from '../components/admin/AdminArticles.vue'
import AdminSensitive from '../components/admin/AdminSensitive.vue'
import AdminReports from '../components/admin/AdminReports.vue'
import AdminAudit from '../components/admin/AdminAudit.vue'
import AdminDialogs from '../components/admin/AdminDialogs.vue'

const workspace = useAdminWorkspace()
const store = useUserStore()
const { tab, onTabChange, pendingReports, loading, loadError, busy, refreshPanel } = workspace
const panels = { summary: AdminOverview, users: AdminUsers, boards: AdminBoards, articles: AdminArticles, sensitive: AdminSensitive, reports: AdminReports, audit: AdminAudit }
const navigation = [
  { id: 'summary', label: '概览统计', icon: DataLine },
  { id: 'users', label: '用户管理', icon: User },
  { id: 'boards', label: '版块管理', icon: CollectionTag },
  { id: 'articles', label: '帖子管理', icon: Document },
  { id: 'sensitive', label: '敏感词管理', icon: Lock },
  { id: 'reports', label: '举报处理', icon: Warning },
  { id: 'audit', label: '审计日志', icon: Tickets }
]
const title = computed(() => navigation.find(item => item.id === tab.value)?.label)
</script>

<template>
  <div class="admin-container">
    <aside class="admin-rail">
      <router-link to="/" class="admin-brand"><span class="brand-mark"><span></span><span></span><span></span></span><strong>校园论坛</strong></router-link>
      <span class="admin-nav-label">管理工作台</span>
      <nav class="admin-navigation" aria-label="后台管理">
        <button v-for="item in navigation" :key="item.id" :class="{ selected: tab === item.id }" :aria-current="tab === item.id ? 'page' : undefined" @click="onTabChange(item.id)">
          <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
          <span v-if="item.id === 'reports' && pendingReports" class="pending-count">{{ pendingReports }}</span>
        </button>
      </nav>
      <router-link to="/" class="admin-back"><el-icon><ArrowLeft /></el-icon>返回论坛</router-link>
    </aside>
    <div class="admin-workarea">
      <header class="admin-header">
        <div class="admin-location">管理后台<span>/</span><strong>{{ title }}</strong></div>
        <div class="admin-account"><UserAvatar :user="store.userInfo" /><span>{{ displayName(store.userInfo) }}</span></div>
      </header>
      <main class="admin-main-panel" :aria-busy="loading || busy">
        <div v-if="loadError" class="admin-error" role="alert"><span>数据加载失败，请重试。</span><el-button :icon="Refresh" @click="refreshPanel">重新加载</el-button></div>
        <div v-loading="loading || busy"><component :is="panels[tab]" :workspace="workspace" /></div>
      </main>
    </div>
    <AdminDialogs :workspace="workspace" />
  </div>
</template>

<style src="../styles/admin.css"></style>
