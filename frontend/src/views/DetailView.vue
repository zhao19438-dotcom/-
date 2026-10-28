<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  View,
  Edit,
  Delete,
  Warning,
  Share,
  Close,
  Message,
  Pointer,
  Star,
  StarFilled,
  Bell,
  Search,
  ChatDotRound,
  CaretTop
} from '@element-plus/icons-vue'
import { articleApi, replyApi } from '../api/article'
import { messageApi } from '../api/message'
import { renderMarkdown } from '../utils/markdown'
import { useUserStore } from '../stores/user'
import { displayName, formatTime } from '../utils/format'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const articleId = computed(() => route.params.id)

const article = ref(null)
const loading = ref(true)
const loadError = ref(false)
const repliesError = ref(false)
let detailRequest = 0
const replies = ref([])
const replyText = ref('')
const replyTo = ref(null)
const posting = ref(false)

const html = computed(() => article.value ? renderMarkdown(article.value.content) : '')

// 解析失物招领等扩展字段
const extInfo = computed(() => {
  const a = article.value
  if (!a || a.bizType !== 'lostfound' || !a.extJson) return null
  try {
    const obj = JSON.parse(a.extJson)
    return obj && typeof obj === 'object' ? obj : null
  } catch (e) {
    return null
  }
})
const lostKind = computed(() => extInfo.value?.lostType || extInfo.value?.type || 'lost')

// 加载帖子详情与回帖列表
async function load () {
  const request = ++detailRequest
  const id = articleId.value
  loading.value = true
  loadError.value = false
  article.value = null
  replies.value = []
  replyTo.value = null
  try {
    const result = await articleApi.detail(id)
    if (request !== detailRequest) return
    article.value = result
    await loadReplies()
  } catch {
    if (request === detailRequest) loadError.value = true
  } finally {
    if (request === detailRequest) loading.value = false
  }
}

// 加载回帖列表
async function loadReplies () {
  const id = articleId.value
  repliesError.value = false
  try {
    const result = await replyApi.list(id)
    if (id !== articleId.value) return
    replies.value = result
    // 回复多时先只渲染前若干条，避免一次渲染过长列表
    visibleReplyCount.value = REPLY_PAGE_SIZE
  } catch (e) {
    if (id === articleId.value) repliesError.value = true
  }
}

// 回复客户端分页：接口一次返回完整楼中楼结构，前端分批渲染
const REPLY_PAGE_SIZE = 10
const visibleReplyCount = ref(REPLY_PAGE_SIZE)
const visibleReplies = computed(() => replies.value.slice(0, visibleReplyCount.value))
const hasMoreReplies = computed(() => replies.value.length > visibleReplyCount.value)

function loadMoreReplies () {
  visibleReplyCount.value += REPLY_PAGE_SIZE
}

// 回复点赞 / 取消点赞（幂等，后端以点赞记录是否存在为准）
async function toggleReplyLike (r) {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再点赞')
    return router.push('/login')
  }
  try {
    if (r.liked) {
      await replyApi.thumbsDown(r.id)
      r.liked = false
      r.likeCount = Math.max(0, (r.likeCount || 0) - 1)
    } else {
      await replyApi.thumbsUp(r.id)
      r.liked = true
      r.likeCount = (r.likeCount || 0) + 1
    }
  } catch (e) {
    // 失败提示由全局拦截器统一处理
  }
}

// 点赞 / 取消点赞
async function toggleLike () {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再点赞')
    return router.push('/login')
  }
  if (article.value.liked) {
    const r = await articleApi.thumbsDown(articleId.value)
    article.value.liked = false
    article.value.likeCount = (r && r.likeCount != null) ? r.likeCount : Math.max(0, article.value.likeCount - 1)
  } else {
    const r = await articleApi.thumbsUp(articleId.value)
    article.value.liked = true
    article.value.likeCount = (r && r.likeCount != null) ? r.likeCount : article.value.likeCount + 1
  }
}

// 收藏 / 取消收藏
async function toggleCollect () {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再收藏')
    return router.push('/login')
  }
  if (article.value.collected) {
    await articleApi.uncollect(articleId.value)
    article.value.collected = false
    ElMessage.success('已取消收藏')
  } else {
    await articleApi.collect(articleId.value)
    article.value.collected = true
    ElMessage.success('已加入我的收藏')
  }
}

// 复制链接分享
function shareArticle () {
  const url = window.location.href
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('已复制帖子链接到剪贴板')
    }).catch(() => {
      ElMessage.info(`帖子链接：${url}`)
    })
  } else {
    ElMessage.info(`帖子链接：${url}`)
  }
}

// 滚动定位到回帖框
function scrollToReply () {
  const el = document.getElementById('reply-input-box')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.focus()
  }
}

// 设置回复目标
function setReplyTo (reply) {
  replyTo.value = {
    replyId: reply.id,
    name: reply.user ? (reply.user.nickname || reply.user.username) : '同学'
  }
  scrollToReply()
}

// 提交回帖
async function submitReply () {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再发表回帖')
    return router.push('/login')
  }
  if (!replyText.value.trim()) {
    return ElMessage.warning('回帖内容不能为空')
  }
  posting.value = true
  try {
    const data = { articleId: articleId.value, content: replyText.value.trim() }
    if (replyTo.value) data.replyId = replyTo.value.replyId
    await replyApi.create(data)
    replyText.value = ''
    replyTo.value = null
    await loadReplies()
    ElMessage.success('回帖发表成功')
  } finally {
    posting.value = false
  }
}

// 私信交流弹窗状态（userId 指定收件人，默认帖子作者）
const msgDialog = reactive({
  visible: false,
  userId: null,
  name: '',
  content: '',
  loading: false
})

function openSendMsg () {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再发送私信')
    return router.push('/login')
  }
  if (store.userInfo && article.value?.user && store.userInfo.id === article.value.user.id) {
    return ElMessage.info('不能给自己发送站内信')
  }
  msgDialog.userId = article.value.user.id
  msgDialog.name = article.value.user.nickname || article.value.user.username || ''
  msgDialog.content = ''
  msgDialog.visible = true
}

// 从交流区头像发起私信：点回复者头像 → 菜单 → 发私信
function openSendMsgTo (user) {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再发送私信')
    return router.push('/login')
  }
  if (!user || !user.id) return ElMessage.error('找不到该用户')
  if (store.userInfo && store.userInfo.id === user.id) {
    return ElMessage.info('不能给自己发送站内信')
  }
  msgDialog.userId = user.id
  msgDialog.name = user.nickname || user.username || ''
  msgDialog.content = ''
  msgDialog.visible = true
}

// 回复者头像点击菜单分发：profile=查看主页，dm=发私信
function onAvatarCommand (cmd, user) {
  if (cmd === 'profile' && user) {
    router.push(`/profile/${user.id}`)
  } else if (cmd === 'dm') {
    openSendMsgTo(user)
  }
}

async function submitSendMsg () {
  if (!msgDialog.content.trim()) return ElMessage.warning('请输入私信内容')
  if (!msgDialog.userId) return ElMessage.error('找不到收件人')
  msgDialog.loading = true
  try {
    await messageApi.send({
      receiveUserId: msgDialog.userId,
      content: msgDialog.content.trim()
    })
    // 后端规则：对方回复之前只能发送一条私信；发送成功后弹窗关闭
    ElMessage.success('私信已发送，等待对方回复')
    msgDialog.visible = false
  } finally {
    msgDialog.loading = false
  }
}

// 举报弹窗状态
const reportDialog = reactive({ visible: false, type: 0, reason: '', loading: false })
function openReport () {
  if (!store.isLogin) {
    ElMessage.warning('请先登录后再提交举报')
    return router.push('/login')
  }
  reportDialog.type = 0
  reportDialog.reason = ''
  reportDialog.visible = true
}

async function submitReport () {
  if (reportDialog.type === 4 && !reportDialog.reason.trim()) {
    return ElMessage.warning('请填写具体违规说明')
  }
  reportDialog.loading = true
  try {
    await articleApi.report({
      articleId: Number(articleId.value),
      type: reportDialog.type,
      reason: reportDialog.reason.trim() || undefined
    })
    ElMessage.success('违规举报已提交，管理员将核查处理')
    reportDialog.visible = false
  } finally {
    reportDialog.loading = false
  }
}

// 删除帖子
async function delArticle () {
  await ElMessageBox.confirm('确认删除这篇帖子？删除后不可恢复。', '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await articleApi.delete(articleId.value)
  ElMessage.success('帖子已删除')
  router.push('/')
}

watch(articleId, load, { immediate: true })
</script>

<template>
  <div v-if="loading && !article" class="detail-page-container">
    <el-skeleton :rows="10" animated />
  </div>
  <div v-else-if="article" class="detail-page-container">
    <!-- 面包屑导航与返回按钮 -->
    <div class="detail-top-nav anim-fade-up">
      <router-link class="back-link-btn" :to="{ path: '/', query: { boardId: article.board?.id } }">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回讨论列表</span>
      </router-link>

      <div class="breadcrumb-trail">
        <router-link to="/">交流广场</router-link>
        <i>/</i>
        <span v-if="article.board" class="board-crumb" @click="router.push(`/?boardId=${article.board.id}`)">
          {{ article.board.name }}
        </span>
        <i>/</i>
        <span class="active-crumb">详情</span>
      </div>
    </div>

    <!-- 帖子主体卡片 -->
    <article class="article-sheet anim-fade-up" style="animation-delay: 80ms;">
      <!-- 头部：标题与标签 -->
      <header class="sheet-header">
        <div class="sheet-badges">
          <span v-if="article.topState === 1" class="tag-pin">置顶</span>
          <span v-if="article.essenceState === 1" class="tag-essence">精华</span>
          <span v-if="article.bizType === 'lostfound'" class="tag-lostfound">失物招领</span>
          <span v-if="article.board" class="tag-board">{{ article.board.name }}</span>
        </div>

        <h1 class="sheet-title">{{ article.title }}</h1>

        <!-- 作者与互动元信息 -->
        <div class="sheet-meta-bar">
          <div class="author-block">
            <UserAvatar :user="article.user" size="medium" />
            <div class="author-details">
              <div class="author-title-row">
                <strong>{{ displayName(article.user) }}</strong>
                <span v-if="article.user?.isAdmin" class="badge-admin">管理员</span>
                <small class="author-tag">{{ article.user?.college || '在校同学' }}</small>
              </div>
              <div class="time-read-row">
                <span>发布于 {{ formatTime(article.createTime) || '刚刚' }}</span>
                <span class="dot-sep">·</span>
                <span><el-icon><View /></el-icon> {{ article.visitCount || 0 }} 浏览</span>
                <span class="dot-sep">·</span>
                <span><el-icon><ChatDotRound /></el-icon> {{ replies.length }} 评论</span>
              </div>
            </div>
          </div>

          <div class="sheet-actions-right">
            <button
              v-if="store.isLogin && store.userInfo?.id !== article.userId"
              class="outline-button"
              @click="openSendMsg"
            >
              <el-icon><Message /></el-icon>
              <span>私信作者</span>
            </button>

            <el-dropdown trigger="click">
              <button class="outline-button icon-only-btn" title="更多操作">
                <span>···</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="article.own" :icon="Edit" @click="router.push(`/post?id=${articleId}`)">编辑讨论</el-dropdown-item>
                  <el-dropdown-item v-if="article.own" :icon="Delete" @click="delArticle">删除讨论</el-dropdown-item>
                  <el-dropdown-item :icon="Share" @click="shareArticle">复制链接</el-dropdown-item>
                  <el-dropdown-item :icon="Warning" @click="openReport">违规举报</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </header>

      <!-- 失物招领专用卡片展示 -->
      <div v-if="extInfo" class="lostfound-box">
        <div class="lf-header">
          <el-icon>
            <Bell v-if="lostKind === 'found'" />
            <Search v-else />
          </el-icon>
          <span>{{ lostKind === 'found' ? '拾得物品登记卡片' : '寻物求助登记卡片' }}</span>
        </div>
        <div class="lf-grid">
          <div class="lf-item"><span class="lf-label">物品名称：</span><span class="lf-val">{{ extInfo.itemName }}</span></div>
          <div class="lf-item"><span class="lf-label">地点：</span><span class="lf-val">{{ extInfo.location }}</span></div>
          <div class="lf-item"><span class="lf-label">时间：</span><span class="lf-val">{{ extInfo.itemTime }}</span></div>
          <div class="lf-item"><span class="lf-label">联系方式：</span><span class="lf-val highlight">{{ extInfo.contact }}</span></div>
        </div>
      </div>

      <!-- Markdown 正文渲染 -->
      <div class="article-markdown-body markdown-body" v-html="html"></div>

      <!-- 底部互动操作栏 -->
      <div class="detail-actions-bar">
        <button
          class="interaction-btn"
          :class="{ active: article.liked }"
          @click="toggleLike"
        >
          <!-- key 切换触发重挂载，已赞时播放 like-pop 弹跳动画 -->
          <el-icon :key="'like-' + article.liked" :class="{ 'pop-anim-icon': article.liked }">
            <Pointer />
          </el-icon>
          <span>{{ article.liked ? '已点赞' : '点赞' }}</span>
          <b>({{ article.likeCount || 0 }})</b>
        </button>

        <button
          class="interaction-btn"
          :class="{ active: article.collected }"
          @click="toggleCollect"
        >
          <el-icon :key="'collect-' + article.collected" :class="{ 'pop-anim-icon': article.collected }">
            <StarFilled v-if="article.collected" />
            <Star v-else />
          </el-icon>
          <span>{{ article.collected ? '已收藏' : '收藏' }}</span>
        </button>

        <button class="interaction-btn" @click="shareArticle">
          <el-icon><Share /></el-icon>
          <span>分享</span>
        </button>

        <button class="interaction-btn report-btn-link" @click="openReport">
          <el-icon><Warning /></el-icon>
          <span>举报违规</span>
        </button>
      </div>

      <!-- 回帖评论区 -->
      <section class="replies-section">
        <div class="replies-title-row">
          <h3>大家的回应 <span>{{ replies.length }}</span></h3>
        </div>

        <!-- 评论发布卡片 -->
        <div class="composer-card">
          <div v-if="replyTo" class="reply-target-tag">
            <span>正在回复 <strong>@{{ replyTo.name }}</strong></span>
            <el-icon class="cancel-tag-btn" @click="replyTo = null"><Close /></el-icon>
          </div>

          <el-input
            id="reply-input-box"
            v-model="replyText"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论…"
            maxlength="500"
            show-word-limit
            class="composer-textarea"
          />

          <div class="composer-submit-bar">
            <span></span>
            <button
              class="primary-button"
              :disabled="posting"
              @click="submitReply"
            >
              {{ posting ? '发送中...' : '发表评论' }}
            </button>
          </div>
        </div>

        <!-- 回帖列表 -->
        <div v-if="repliesError" class="empty-replies-tip" role="alert">
          <p>评论加载失败</p><button class="outline-button" @click="loadReplies">重新加载</button>
        </div>
        <div v-else-if="!replies.length" class="empty-replies-tip">
          <p>还没有评论，来发表第一条吧。</p>
        </div>

        <ul v-else class="replies-list">
          <li
            v-for="(r, idx) in visibleReplies"
            :key="r.id"
            class="reply-item-row anim-fade-up"
            :style="{ animationDelay: Math.min(idx * 40, 400) + 'ms' }"
          >
            <el-dropdown trigger="click" @command="(cmd) => onAvatarCommand(cmd, r.user)">
              <span class="avatar-clickable" title="点击查看操作">
                <UserAvatar :user="r.user" />
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">查看主页</el-dropdown-item>
                  <el-dropdown-item
                    command="dm"
                    :disabled="!store.isLogin || (store.userInfo && r.user && store.userInfo.id === r.user.id)"
                  >
                    发私信
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <div class="reply-content-box">
              <div class="reply-user-line">
                <span class="r-author-nick">{{ displayName(r.user) }}</span>
                <span v-if="r.user?.id === article.userId" class="r-lz-badge">楼主</span>
                <span class="r-floor-number">#{{ idx + 1 }} 楼</span>
                <span class="r-time-text">{{ formatTime(r.createTime) || '刚刚' }}</span>
              </div>

              <div class="r-message-body">
                <span v-if="r.replyUser" class="r-quote-pill">@{{ r.replyUser.nickname || r.replyUser.username }}</span>
                <span class="r-text-content">{{ r.content }}</span>
              </div>

              <div class="reply-actions-row">
                <button class="sub-reply-btn" @click="setReplyTo(r)">
                  <el-icon><ChatDotRound /></el-icon> 回复
                </button>
                <button
                  class="sub-reply-btn"
                  :class="{ 'liked-btn': r.liked }"
                  @click="toggleReplyLike(r)"
                >
                  <el-icon><CaretTop /></el-icon> {{ r.liked ? '已赞' : '点赞' }} {{ r.likeCount || 0 }}
                </button>
              </div>
            </div>
          </li>
        </ul>

        <!-- 回复分批加载：避免长列表一次渲染 -->
        <div v-if="hasMoreReplies" class="load-more-replies">
          <button class="sub-reply-btn load-more-btn" @click="loadMoreReplies">
            加载更多回复（剩余 {{ replies.length - visibleReplyCount }} 条）
          </button>
        </div>
      </section>
    </article>

    <!-- 私信弹窗 -->
    <el-dialog v-model="msgDialog.visible" :title="msgDialog.name ? `发送私信给 ${msgDialog.name}` : '发送站内私信'" width="460px" append-to-body>
      <div class="dm-rule-tip">
        <el-icon><Bell /></el-icon>
        会话规则：对方回复之前只能发送一条私信
      </div>
      <el-input
        v-model="msgDialog.content"
        type="textarea"
        :rows="4"
        placeholder="请输入私信内容…"
        maxlength="300"
        show-word-limit
      />
      <template #footer>
        <button class="outline-button" @click="msgDialog.visible = false">取消</button>
        <button class="primary-button" style="margin-left: 8px;" :disabled="msgDialog.loading" @click="submitSendMsg">
          {{ msgDialog.loading ? '发送中...' : '发送私信' }}
        </button>
      </template>
    </el-dialog>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportDialog.visible" title="违规内容举报" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="举报违规类型">
          <el-radio-group v-model="reportDialog.type">
            <el-radio :label="0">垃圾广告</el-radio>
            <el-radio :label="1">色情低俗</el-radio>
            <el-radio :label="2">辱骂攻击</el-radio>
            <el-radio :label="3">违法违规</el-radio>
            <el-radio :label="4">其他说明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="reportDialog.type === 4" label="补充具体违规原因">
          <el-input
            v-model="reportDialog.reason"
            type="textarea"
            :rows="3"
            placeholder="请详细描述违规细节…"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="outline-button" @click="reportDialog.visible = false">取消</button>
        <button class="primary-button" style="margin-left: 8px;" :disabled="reportDialog.loading" @click="submitReport">
          {{ reportDialog.loading ? '提交中...' : '提交举报' }}
        </button>
      </template>
    </el-dialog>
  </div>
  <div v-else class="detail-page-container empty-state" role="alert">
    <h3>{{ loadError ? '暂时无法查看这篇讨论' : '讨论不存在' }}</h3>
    <p>内容可能已被移除，或暂时无法连接。</p>
    <button class="outline-button" @click="load">重试</button>
    <router-link to="/" class="primary-button">返回论坛</router-link>
  </div>
</template>

<style scoped>
.detail-page-container {
  max-width: 880px;
  margin: 0 auto;
}

.detail-top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.back-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  color: var(--ink-soft);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  font-size: 11.5px;
  font-weight: 600;
  transition: color var(--dur) var(--ease), border-color var(--dur) var(--ease), background var(--dur) var(--ease), transform var(--dur-fast) var(--ease);
}
.back-link-btn:hover {
  color: var(--teal);
  border-color: var(--c-border-hover);
  background: var(--teal-pale-2);
}
/* 按压反馈 */
.back-link-btn:active {
  transform: scale(0.96);
}
.breadcrumb-trail {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--muted);
}
.breadcrumb-trail a:hover, .board-crumb:hover {
  color: var(--teal);
  cursor: pointer;
}
.active-crumb {
  color: var(--ink);
  font-weight: 700;
}
.dot-sep, .breadcrumb-trail i {
  color: #cbd5e1;
  font-style: normal;
}

/* 主卡片 */
.article-sheet {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 40px 48px;
  box-shadow: var(--shadow-xs);
}

.sheet-header {
  border-bottom: 1px solid var(--line);
  padding-bottom: 24px;
}
.sheet-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}
.sheet-title {
  margin: 0 0 18px;
  color: var(--ink);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.sheet-meta-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.author-block {
  display: flex;
  align-items: center;
  gap: 12px;
}
.author-details {
  display: grid;
  gap: 3px;
}
.author-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.author-title-row strong {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}
.badge-admin {
  padding: 1px 6px;
  color: #fff;
  background: var(--orange);
  border-radius: 4px;
  font-size: 9.5px;
  font-weight: 700;
}
.author-tag {
  color: var(--muted);
  font-size: 11px;
}
.time-read-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--muted);
  font-size: 12px;
  flex-wrap: wrap;
}
.time-read-row .el-icon {
  font-size: 12px;
}

.sheet-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.icon-only-btn {
  padding: 0 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

/* 正文 */
.article-markdown-body {
  padding: 32px 0;
  min-height: 120px;
  font-size: 16px;
  line-height: 1.95;
  overflow-wrap: anywhere;
}

/* 底部交互栏 */
.detail-actions-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.interaction-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  color: var(--ink-soft);
  background: #f8fafc;
  border: 1px solid var(--line);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  transition: color var(--dur) var(--ease), border-color var(--dur) var(--ease), background var(--dur) var(--ease), transform var(--dur-fast) var(--ease);
}
.interaction-btn:hover {
  color: var(--teal);
  border-color: var(--c-border-hover);
  background: var(--teal-pale-2);
}
/* 按压反馈 */
.interaction-btn:active {
  transform: scale(0.95);
}
.interaction-btn.active {
  color: var(--teal-dark);
  background: var(--teal-pale);
  border-color: var(--c-border-hover);
  font-weight: 700;
}
/* 点赞/收藏成功瞬间的缩放弹跳动画（引用全局 like-pop 关键帧） */
.pop-anim-icon {
  animation: like-pop 0.45s var(--ease-spring);
}
.interaction-btn b {
  font-weight: 700;
}
.report-btn-link {
  margin-left: auto;
  color: var(--muted);
  background: transparent;
  border-color: transparent;
}
.report-btn-link:hover {
  color: #b65c3d;
  background: #fff5f0;
}

/* 回帖区 */
.replies-section {
  padding-top: 36px;
}
.replies-title-row h3 {
  margin: 0 0 18px;
  color: var(--ink);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}
.replies-title-row span {
  color: var(--teal);
  font-size: 14px;
}

.composer-card {
  padding: 16px 20px;
  background: #f8fafc;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
}
.reply-target-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px;
  margin-bottom: 10px;
  color: var(--teal);
  background: var(--teal-pale);
  border-radius: 6px;
  font-size: 11px;
}
.cancel-tag-btn {
  cursor: pointer;
  font-size: 12px;
}
.composer-submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.composer-submit-bar small {
  color: var(--muted);
  font-size: 11px;
}

.empty-replies-tip {
  padding: 36px 0;
  text-align: center;
  color: var(--muted);
  font-size: 12.5px;
}

@media (max-width: 768px) {
  .article-sheet {
    padding: 20px 18px;
  }
  .sheet-title {
    font-size: 20px;
  }
  .sheet-meta-bar {
    flex-wrap: wrap;
    align-items: flex-start;
  }
  .author-block {
    flex: 1 1 100%;
  }
  .sheet-actions-right {
    margin-top: 10px;
    margin-left: auto;
  }
  .lf-grid {
    grid-template-columns: 1fr;
  }
  .detail-actions-bar { flex-wrap: wrap; gap: 8px; }
  .interaction-btn { padding: 8px 12px; border-radius: 7px; }
  .report-btn-link { margin-left: 0; }
  .detail-top-nav { flex-wrap: wrap; gap: 12px; }
  .author-title-row { flex-wrap: wrap; }
}

/* 回复区头像可点击：带悬停反馈，暗示可操作 */
.avatar-clickable {
  display: inline-flex;
  cursor: pointer;
  border-radius: 50%;
  transition: box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}
.avatar-clickable:hover {
  box-shadow: 0 0 0 2px var(--teal-pale-2);
  transform: scale(1.06);
}

/* 私信弹窗内的会话规则提示条 */
.dm-rule-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: var(--teal-pale-2);
  color: var(--teal-dark);
  font-size: 12.5px;
}

/* 回复加载更多 */
.load-more-replies {
  display: flex;
  justify-content: center;
  padding: 14px 0 4px;
}

.load-more-btn {
  font-size: 13px;
  padding: 6px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
}

.load-more-btn:hover {
  border-color: var(--teal);
}
</style>
