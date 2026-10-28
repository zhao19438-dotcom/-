<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '../api/user'
import { articleApi } from '../api/article'
import PostPreview from '../components/PostPreview.vue'

const loading = ref(true)
const error = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const removing = ref(null)
const size = 10
async function load () {
  loading.value = true
  error.value = false
  try {
    const res = await userApi.collections(page.value, size)
    list.value = res.records || []
    total.value = res.total || 0
  } catch { error.value = true } finally { loading.value = false }
}
async function remove (article) {
  if (removing.value !== null) return
  try { await ElMessageBox.confirm(`取消收藏「${article.title}」？`, '取消收藏', { type: 'warning' }) } catch { return }
  removing.value = article.id
  try {
    await articleApi.uncollect(article.id)
    if (list.value.length === 1 && page.value > 1) page.value--
    await load()
    ElMessage.success('已取消收藏')
  } catch { /* Shared request handler displays API errors. */ } finally { removing.value = null }
}
onMounted(load)
</script>

<template>
  <div class="collections-page">
    <header class="page-heading"><div><h1>我的收藏</h1><p v-if="!loading && !error">{{ total }} 篇讨论</p></div><router-link to="/" class="outline-button">浏览讨论</router-link></header>
    <section class="collection-surface" :aria-busy="loading">
      <div v-if="loading" class="collection-placeholder"><el-skeleton :rows="7" animated /></div>
      <div v-else-if="error" class="empty-state" role="alert"><h3>收藏加载失败</h3><button class="outline-button" @click="load">重新加载</button></div>
      <div v-else-if="!list.length" class="empty-state"><h3>还没有收藏</h3><p>喜欢的讨论，可以收藏后再读。</p><router-link to="/" class="primary-button">浏览讨论</router-link></div>
      <template v-else><div v-for="article in list" :key="article.id" class="collection-item">
        <PostPreview :article="article" />
        <div class="collection-actions"><el-button link :loading="removing === article.id" :disabled="removing !== null" @click="remove(article)">取消收藏</el-button></div>
      </div></template>
      <div v-if="!loading && !error && total > size" class="collection-pagination"><el-pagination v-model:current-page="page" :page-size="size" :total="total" :pager-count="5" layout="prev, pager, next" background @current-change="load" /></div>
    </section>
  </div>
</template>

<style scoped>
.collections-page { max-width: var(--page-width); margin: auto; }
.collection-surface { background: white; border: 1px solid var(--line); border-radius: 12px; overflow: hidden; }
.collection-item + .collection-item { border-top: 1px solid var(--line); }
.collection-actions { display: flex; justify-content: flex-end; padding: 0 28px 18px; }
.collection-placeholder { padding: 30px; }
.collection-pagination { display: flex; justify-content: center; padding: 24px; border-top: 1px solid var(--line); }
.collection-surface .empty-state { border: 0; min-height: 280px; }
</style>
