<script setup>
import { computed } from 'vue'
import { User, CollectionTag, Document, Warning, Plus, Refresh, Lock } from '@element-plus/icons-vue'
const props = defineProps({ workspace: { type: Object, required: true } })
const { summary, pendingReports, refreshPanel, refreshSensitive, onTabChange, openBoard } = props.workspace
const metrics = computed(() => [
  { label: '注册用户', value: summary.value.users || 0, icon: User, tab: 'users' },
  { label: '讨论版块', value: summary.value.boards || 0, icon: CollectionTag, tab: 'boards' },
  { label: '论坛帖子', value: summary.value.articles || 0, icon: Document, tab: 'articles' },
  { label: '待处理举报', value: pendingReports.value, icon: Warning, tab: 'reports' }
])
</script>

<template>
  <section class="panel-section">
    <div class="panel-title-row"><h1 class="section-title">概览统计</h1><el-button :icon="Refresh" @click="refreshPanel">刷新数据</el-button></div>
    <div class="stat-cards-grid">
      <button v-for="metric in metrics" :key="metric.tab" class="stat-card card" @click="onTabChange(metric.tab)">
        <span class="sc-icon-wrap"><el-icon><component :is="metric.icon" /></el-icon></span>
        <span class="sc-info"><span class="sc-val">{{ metric.value }}</span><span class="sc-label">{{ metric.label }}</span></span>
      </button>
    </div>
    <div class="card quick-actions-card">
      <h2 class="dash-subtitle">快捷操作</h2>
      <div class="qa-grid">
        <button class="qa-item" @click="openBoard(null)"><el-icon class="qa-icon"><Plus /></el-icon>新增讨论版块</button>
        <button class="qa-item" @click="onTabChange('sensitive')"><el-icon class="qa-icon"><Lock /></el-icon>管理敏感词</button>
        <button class="qa-item" @click="onTabChange('reports')"><el-icon class="qa-icon"><Warning /></el-icon>处理举报<span v-if="pendingReports">（{{ pendingReports }}）</span></button>
        <button class="qa-item" @click="refreshSensitive"><el-icon class="qa-icon"><Refresh /></el-icon>更新敏感词库</button>
      </div>
    </div>
  </section>
</template>
