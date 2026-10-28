<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Edit,
  Document,
  Star,
  ChatDotRound,
  View,
  School,
  Postcard,
  Promotion
} from '@element-plus/icons-vue'
import { userApi } from '../api/user'
import { articleApi } from '../api/article'
import { messageApi } from '../api/message'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const profile = ref(null)
const articles = ref([])
const loading = ref(false)
const activeTab = ref('posts')
const collections = ref([])
const loadingCollect = ref(false)
const collectionPage = ref(1)
const collectionTotal = ref(0)
const collectionError = ref(false)
const collectionSize = 10

// 发私信对话框（他人主页）
const dmDialog = reactive({ visible: false, content: '', loading: false })

function openDm () {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再发送私信')
    return router.push('/login')
  }
  dmDialog.content = ''
  dmDialog.visible = true
}

async function submitDm () {
  if (!dmDialog.content.trim()) return ElMessage.warning('请输入私信内容')
  dmDialog.loading = true
  try {
    await messageApi.send({ receiveUserId: Number(targetId.value), content: dmDialog.content.trim() })
    dmDialog.visible = false
    ElMessage.success('私信已发送，等待对方回复')
  } finally {
    dmDialog.loading = false
  }
}

// 没有 id 时展示当前登录用户
const targetId = computed(() => route.params.id || (store.userInfo && store.userInfo.id) || '')
const isSelf = computed(() => store.userInfo && store.userInfo.id && String(store.userInfo.id) === String(targetId.value))

async function load () {
  if (!targetId.value) return
  loading.value = true
  try {
    profile.value = await userApi.info(targetId.value)
    articles.value = await articleApi.byUser(targetId.value)
  } finally {
    loading.value = false
  }
}

// 收藏列表仅本人可见：后端只提供当前登录用户的收藏接口。
async function loadCollections () {
  loadingCollect.value = true
  collectionError.value = false
  try {
    const res = await userApi.collections(collectionPage.value, collectionSize)
    collections.value = res.records || []
    collectionTotal.value = res.total || 0
  } catch {
    collectionError.value = true
  } finally {
    loadingCollect.value = false
  }
}
function onTabChange (tab) {
  if (tab === 'collect' && isSelf.value) loadCollections()
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div class="page profile-page" v-if="loading">
    <div class="card" style="padding: 30px;">
      <el-skeleton :rows="6" animated />
    </div>
  </div>

  <div class="page profile-page" v-else-if="profile">
    <!-- 用户个人主页 Hero 头部卡片 -->
    <div class="card profile-hero-card">
      <div class="profile-main">
        <div class="avatar-row">
          <el-avatar :size="80" :src="profile.avatarUrl || ''" class="profile-avatar">
            {{ (profile.nickname || profile.username || '?').slice(0, 1) }}
          </el-avatar>
          <div class="profile-btn-wrap" v-if="isSelf">
            <el-button :icon="Edit" round @click="router.push('/settings')">
              编辑个人资料
            </el-button>
          </div>
          <!-- 他人主页：提供发私信入口 -->
          <div class="profile-btn-wrap" v-else-if="store.isLogin">
            <el-button type="primary" :icon="Promotion" round @click="openDm">
              发私信
            </el-button>
          </div>
        </div>

        <div class="profile-details">
          <div class="name-title-row">
            <h2 class="user-fullname">{{ profile.nickname || profile.username }}</h2>
            <span class="user-handle">@{{ profile.username }}</span>
            <el-tag v-if="profile.isAdmin === 1" size="small" type="danger" effect="dark" round>
              管理员
            </el-tag>
          </div>

          <p v-if="profile.remark" class="user-bio">{{ profile.remark }}</p>

          <div class="user-tags">
            <span class="info-pill" v-if="profile.college">
              <el-icon><School /></el-icon> {{ profile.college }} · {{ profile.className || '校园同学' }}
            </span>
            <span class="info-pill" v-if="profile.studentNo && isSelf">
              <el-icon><Postcard /></el-icon> 学号：{{ profile.studentNo }}
            </span>
            <span class="info-pill" v-if="profile.realName && isSelf">
              真实姓名：{{ profile.realName }}
            </span>
          </div>

          <div class="user-stats-strip">
            <div class="strip-stat">
              <span class="s-val">{{ profile.articleCount || articles.length || 0 }}</span>
              <span class="s-lbl">发表帖子</span>
            </div>
            <div class="strip-sep"></div>
            <div class="strip-stat">
              <span class="s-val">{{ articles.reduce((acc, a) => acc + (a.likeCount || 0), 0) }}</span>
              <span class="s-lbl">累计获赞</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 帖子/收藏 Tab 切换区 -->
    <div class="card tabs-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange" class="profile-tabs">
        <el-tab-pane :label="isSelf ? '我的发帖' : 'TA 的帖子'" name="posts" />
        <el-tab-pane v-if="isSelf" label="我的收藏" name="collect" />
      </el-tabs>

      <!-- 帖子列表 -->
      <div v-if="activeTab === 'posts'">
        <div v-if="!articles.length" class="empty-state">
          <el-empty description="还没有发布过帖子" :image-size="100" />
        </div>
        <div v-else class="post-cards">
          <div
            v-for="(a, index) in articles"
            :key="a.id"
            class="p-card clickable anim-fade-up"
            :style="{ animationDelay: Math.min(index * 40, 400) + 'ms' }"
            @click="router.push(`/article/${a.id}`)"
          >
            <div class="p-title-row">
              <span v-if="a.topState === 1" class="tag-top">置顶</span>
              <span v-if="a.essenceState === 1" class="tag-essence">精华</span>
              <h4 class="p-title">{{ a.title }}</h4>
            </div>
            <div class="p-meta">
              <span>{{ a.createTime }}</span>
              <span class="p-counts">
                赞 {{ a.likeCount }} · 评 {{ a.replyCount }} · 浏览 {{ a.visitCount }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 收藏列表 -->
      <div v-if="activeTab === 'collect'">
        <div v-if="loadingCollect" style="padding: 20px;">
          <el-skeleton :rows="4" animated />
        </div>
        <div v-else-if="collectionError" class="empty-state"><p>收藏加载失败</p><button class="outline-button" @click="loadCollections">重新加载</button></div>
        <div v-else-if="!collections.length" class="empty-state">
          <el-empty description="还没有收藏任何帖子" :image-size="100" />
        </div>
        <div v-else class="post-cards">
          <div
            v-for="(a, index) in collections"
            :key="a.id"
            class="p-card clickable anim-fade-up"
            :style="{ animationDelay: Math.min(index * 40, 400) + 'ms' }"
            @click="router.push(`/article/${a.id}`)"
          >
            <div class="p-title-row">
              <span v-if="a.topState === 1" class="tag-top">置顶</span>
              <span v-if="a.essenceState === 1" class="tag-essence">精华</span>
              <h4 class="p-title">{{ a.title }}</h4>
            </div>
            <div class="p-meta">
              <span>{{ a.createTime }}</span>
              <span class="p-counts">
                赞 {{ a.likeCount }} · 评 {{ a.replyCount }} · 浏览 {{ a.visitCount }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="!loadingCollect && !collectionError && collectionTotal > collectionSize" class="profile-pagination">
          <el-pagination v-model:current-page="collectionPage" :total="collectionTotal" :page-size="collectionSize" :pager-count="5" layout="prev, pager, next" background @current-change="loadCollections" />
        </div>
      </div>
    </div>
  </div>

  <div class="page profile-page" v-else>
    <el-empty description="该用户不存在或已被注销" />
  </div>

  <!-- 发私信对话框 -->
  <el-dialog
    v-model="dmDialog.visible"
    :title="profile ? `发送私信给 ${profile.nickname || profile.username}` : '发送私信'"
    width="460px"
  >
    <el-input
      v-model="dmDialog.content"
      type="textarea"
      :rows="4"
      maxlength="255"
      show-word-limit
      placeholder="输入私信内容（对方回复前你只能发送一条）…"
    />
    <template #footer>
      <el-button @click="dmDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="dmDialog.loading" @click="submitDm">发送私信</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.profile-pagination { display: flex; justify-content: center; padding-top: 24px; }
.profile-page {
  max-width: var(--page-width);
  margin: 0 auto;
  padding: 8px 16px 32px;
}

.profile-hero-card {
  padding: 0;
  overflow: hidden;
  margin-bottom: 20px;
  background: var(--card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}

.profile-main {
  padding: 32px;
}

.avatar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.profile-avatar {
  border: 4px solid #fff;
  background: var(--teal);
  color: #fff;
  font-weight: 700;
  font-size: 28px;
}

.name-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.user-fullname {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  color: var(--ink);
  letter-spacing: -0.01em;
}

.user-handle {
  font-size: 13px;
  color: var(--muted);
}

.user-bio {
  font-size: 13.5px;
  color: var(--ink-light);
  margin: 6px 0 14px;
  line-height: 1.6;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.info-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  background: var(--paper);
  color: var(--ink-light);
  padding: 3px 12px;
  border-radius: 14px;
  border: 1px solid var(--line);
}

.user-stats-strip {
  display: inline-flex;
  align-items: center;
  gap: 20px;
  padding: 10px 20px;
  background: var(--paper);
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
}

/* 统计条：hover 微浮起 */
.strip-stat {
  display: flex;
  flex-direction: column;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  transition: transform var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease);
}

.strip-stat:hover {
  transform: translateY(-2px);
  background: var(--teal-pale-2);
}

.s-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--teal);
}

.s-lbl {
  font-size: 11px;
  color: var(--muted);
}

.strip-sep {
  width: 1px;
  height: 24px;
  background: var(--line);
}

.tabs-card {
  padding: 20px 28px;
  background: var(--card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}

.post-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.p-card {
  padding: 16px 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  transition: border-color var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease);
  background: var(--card);
}

.p-card:hover {
  border-color: var(--c-border-hover);
  box-shadow: var(--shadow-xs);
  transform: translateY(-1px);
}

.p-card:hover .p-title {
  color: var(--teal);
}

.p-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.p-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
  transition: color var(--dur-fast) var(--ease);
}

.p-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--muted);
}

.empty-state {
  padding: 30px;
  text-align: center;
}
@media (max-width: 600px) {
  .profile-page { padding: 0; }
  .profile-main, .tabs-card { padding: 24px 18px; }
  .name-title-row { align-items: baseline; }
  .user-fullname { font-size: 24px; overflow-wrap: anywhere; }
  .p-meta { flex-wrap: wrap; gap: 8px; }
  .p-card { padding: 16px 0; border: 0; border-bottom: 1px solid var(--line); border-radius: 0; }
}
</style>
