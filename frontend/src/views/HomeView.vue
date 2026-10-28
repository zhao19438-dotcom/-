<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, ArrowRight, Document, Refresh } from '@element-plus/icons-vue'
import { articleApi, boardApi } from '../api/article'
import { useUserStore } from '../stores/user'
import { displayName } from '../utils/format'
import UserAvatar from '../components/UserAvatar.vue'
import PostPreview from '../components/PostPreview.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const list = ref([])
const boards = ref([])
const hotList = ref([])
const total = ref(0)
const loading = ref(true)
const error = ref(false)
const boardsError = ref(false)
const size = 12
let requestId = 0
const boardId = computed(() => String(route.query.boardId || ''))
const keyword = computed(() => String(route.query.keyword || '').trim())
const page = computed(() => Math.max(1, Math.floor(Number(route.query.page) || 1)))
const order = computed(() => route.query.order === 'hot' ? 'hot' : 'latest')
const currentBoard = computed(() => boards.value.find(b => String(b.id) === boardId.value))
const title = computed(() => keyword.value ? '搜索结果' : currentBoard.value?.name || '交流广场')

function updateQuery (values) {
  const query = { ...route.query, ...values }
  Object.keys(query).forEach(key => { if (query[key] === '' || query[key] == null || query[key] === 1) delete query[key] })
  router.push({ path: '/', query })
}
function pickBoard (id) { updateQuery({ boardId: id, page: 1 }) }
function publish () {
  router.push(store.isLogin ? '/post' : { path: '/login', query: { redirect: '/post' } })
}
async function loadBoards () {
  boardsError.value = false
  try { boards.value = await boardApi.available() } catch { boardsError.value = true }
}
async function load () {
  const id = ++requestId
  loading.value = true
  error.value = false
  try {
    const params = { page: page.value, size, orderBy: order.value, boardId: boardId.value || undefined }
    const res = keyword.value
      ? await articleApi.search({ ...params, keyword: keyword.value })
      : await articleApi.list(params)
    if (id !== requestId) return
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    if (id === requestId) error.value = true
  } finally {
    if (id === requestId) loading.value = false
  }
}
onMounted(async () => {
  loadBoards()
  try {
    const res = await articleApi.list({ page: 1, size: 5, orderBy: 'hot' })
    hotList.value = res.records || []
  } catch { hotList.value = [] }
})
watch(() => [boardId.value, keyword.value, page.value, order.value], load, { immediate: true })
</script>

<template>
  <div class="community-page">
    <header class="community-heading">
      <div>
        <h1>{{ title }}</h1>
        <p v-if="keyword">“{{ keyword }}”<span v-if="!loading && !error"> · {{ total }} 条结果</span></p>
        <p v-else-if="!loading && !error">{{ total }} 篇讨论</p>
        <p v-else>校园论坛</p>
      </div>
      <button class="primary-button" type="button" @click="publish"><el-icon><Plus /></el-icon>发布讨论</button>
    </header>

    <div class="community-layout">
      <section class="feed-surface" aria-label="讨论列表" :aria-busy="loading">
        <nav class="feed-categories" aria-label="讨论版块">
          <button :class="{ selected: !boardId }" :aria-pressed="!boardId" @click="pickBoard('')">全部</button>
          <button v-for="b in boards" :key="b.id" :class="{ selected: boardId === String(b.id) }" :aria-pressed="boardId === String(b.id)" @click="pickBoard(String(b.id))">{{ b.name }}</button>
          <button v-if="boardsError" @click="loadBoards">重新加载版块</button>
        </nav>
        <div class="feed-toolbar">
          <div class="feed-sort" aria-label="排序">
            <button :class="{ selected: order === 'latest' }" :aria-pressed="order === 'latest'" @click="updateQuery({ order: '', page: 1 })">最新发布</button>
            <button :class="{ selected: order === 'hot' }" :aria-pressed="order === 'hot'" @click="updateQuery({ order: 'hot', page: 1 })">热门讨论</button>
          </div>
          <button v-if="keyword" class="text-button" @click="updateQuery({ keyword: '', page: 1 })">清除搜索</button>
        </div>
        <div v-if="loading" class="feed-placeholder"><el-skeleton :rows="8" animated /></div>
        <div v-else-if="error" class="feed-placeholder empty-state" role="alert">
          <el-icon class="state-icon"><Refresh /></el-icon>
          <h3>讨论加载失败</h3><p>请检查网络后重试。</p>
          <button class="outline-button" @click="load">重新加载</button>
        </div>
        <div v-else-if="!list.length" class="feed-placeholder empty-state">
          <el-icon class="state-icon"><Document /></el-icon>
          <h3>{{ keyword ? '没有找到相关讨论' : '还没有讨论' }}</h3>
          <p>{{ keyword ? '试试其他关键词，或切换版块。' : '发布一篇讨论，和同学交流。' }}</p>
          <button v-if="keyword" class="outline-button" @click="updateQuery({ keyword: '', page: 1 })">清除搜索</button>
          <button v-else class="primary-button" @click="publish">发布讨论</button>
        </div>
        <template v-else><PostPreview v-for="item in list" :key="item.id" :article="item" /></template>
        <div v-if="!loading && !error && total > size" class="feed-pagination">
          <el-pagination :current-page="page" :page-size="size" :total="total" :pager-count="5" layout="prev, pager, next" background @current-change="p => updateQuery({ page: p })" />
        </div>
      </section>

      <aside class="community-aside">
        <section v-if="store.isLogin" class="aside-surface account-panel">
          <router-link to="/profile" class="account-identity"><UserAvatar :user="store.userInfo" size="medium" /><div><strong>{{ displayName(store.userInfo) }}</strong><small>{{ store.isAdmin ? '管理员' : store.userInfo?.college || '社区成员' }}</small></div></router-link>
          <div class="account-links"><router-link to="/profile">我的帖子<ArrowRight /></router-link><router-link to="/collections">我的收藏<ArrowRight /></router-link><router-link to="/messages">消息中心<ArrowRight /></router-link></div>
        </section>
        <section v-if="hotList.length" class="aside-surface trending-panel">
          <h2>热门讨论<span>HOT</span></h2>
          <ol><li v-for="(item, i) in hotList" :key="item.id"><router-link :to="`/article/${item.id}`"><span class="trend-index">{{ String(i + 1).padStart(2, '0') }}</span><div><h3>{{ item.title }}</h3><small>{{ item.replyCount || 0 }} 条回复 · {{ item.visitCount || 0 }} 次浏览</small></div></router-link></li></ol>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.community-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin: 7px 0 28px; }
.community-heading h1 { margin: 0; font-size: 30px; letter-spacing: -.8px; font-weight: 700; }
.community-heading p { margin: 7px 0 0; color: var(--muted); font-size: 14px; overflow-wrap: anywhere; }
.community-layout { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 28px; align-items: start; }
.feed-surface, .aside-surface { background: var(--card); border: 1px solid var(--line); border-radius: 12px; overflow: hidden; }
.feed-categories { display: flex; flex-wrap: wrap; gap: 4px; padding: 18px 22px; border-bottom: 1px solid var(--line); }
.feed-categories button { padding: 7px 10px; background: transparent; color: var(--ink-soft); font-size: 13px; border-radius: 6px; transition: background 150ms; }
.feed-categories button:hover, .feed-categories .selected { background: var(--teal-pale); color: var(--teal); }
.feed-categories .selected { font-weight: 650; }
.feed-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 0 28px; border-bottom: 1px solid var(--line); }
.feed-sort { display: flex; gap: 24px; }
.feed-sort button { position: relative; padding: 17px 0; background: transparent; color: var(--muted); font-size: 13px; }
.feed-sort .selected { color: var(--ink); font-weight: 650; }
.feed-sort .selected::after { content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 2px; background: var(--teal); }
.text-button { background: transparent; color: var(--teal); font-size: 12px; }
.feed-placeholder { padding: 36px 28px; min-height: 300px; border: 0; }
.state-icon { font-size: 32px; color: var(--teal); }
.feed-pagination { padding: 24px 16px; display: flex; justify-content: center; border-top: 1px solid var(--line); }
.community-aside { display: grid; gap: 20px; }
.account-panel { padding: 24px; }
.account-identity { display: flex; align-items: center; gap: 12px; }
.account-identity strong { display: block; font-size: 15px; overflow-wrap: anywhere; }
.account-identity small { display: block; color: var(--muted); margin-top: 3px; font-size: 12px; }
.account-links { display: grid; margin-top: 20px; border-top: 1px solid var(--line); padding-top: 12px; }
.account-links a { display: flex; justify-content: space-between; align-items: center; padding: 9px 0; color: var(--ink-soft); font-size: 13px; }
.account-links a:hover { color: var(--teal); }
.account-links svg { width: 13px; }
.trending-panel { padding: 24px 22px 8px; }
.trending-panel h2 { display: flex; justify-content: space-between; align-items: center; margin: 0 0 8px; font-size: 15px; font-weight: 650; }
.trending-panel h2 > span { font-size: 10px; color: var(--teal); letter-spacing: 1px; background: var(--teal-pale); padding: 3px 7px; border-radius: 4px; }
.trending-panel ol { list-style: none; padding: 0; margin: 0; }
.trending-panel li + li { border-top: 1px solid var(--line); }
.trending-panel li a { display: flex; gap: 12px; padding: 17px 0; }
.trend-index { font-size: 16px; font-weight: 600; font-variant-numeric: tabular-nums; color: #a7b9b4; line-height: 1.6; }
.trending-panel li:first-child .trend-index { color: var(--teal); }
.trending-panel h3 { margin: 0 0 7px; font-size: 13px; line-height: 1.7; font-weight: 500; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.trending-panel a:hover h3 { color: var(--teal); }
.trending-panel small { color: var(--muted); font-size: 11px; }
@media (max-width: 1000px) { .community-layout { grid-template-columns: minmax(0, 1fr) 250px; gap: 20px; } }
@media (max-width: 820px) { .community-layout { grid-template-columns: 1fr; } .community-aside { display: none; } }
@media (max-width: 600px) {
  .community-heading { margin: 0 0 22px; gap: 12px; }
  .community-heading h1 { font-size: 25px; }
  .community-heading .primary-button { font-size: 13px; padding: 10px 13px; }
  .feed-categories { flex-wrap: nowrap; overflow: auto; padding: 14px 12px; scrollbar-width: thin; }
  .feed-categories button { flex-shrink: 0; }
  .feed-toolbar { padding: 0 17px; }
}
</style>
