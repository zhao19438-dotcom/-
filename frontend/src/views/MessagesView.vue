<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Promotion,
  ChatDotRound,
  Check,
  ArrowRight,
  Message,
  Notification,
  Warning,
  CircleClose
} from '@element-plus/icons-vue'
import { messageApi } from '../api/message'

const tab = ref('inbox')
const inbox = ref([])
const sent = ref([])
const notices = ref([])
const blacklist = ref([])
const unreadInbox = ref(0)
const loading = ref(false)
const loadError = ref(false)
let listRequest = 0

const dialog = reactive({ visible: false, receiveUserId: null, name: '', content: '', sending: false })
// 私信举报弹窗状态
const reportDialog = reactive({ visible: false, messageId: null, type: 0, reason: '', loading: false })

async function loadAll () {
  const request = ++listRequest
  const currentTab = tab.value
  loading.value = true
  loadError.value = false
  try {
    const api = { inbox: messageApi.inbox, sent: messageApi.sent, blacklist: messageApi.blacklistList, notice: messageApi.notifications }
    const result = await api[currentTab]()
    if (request !== listRequest) return
    if (currentTab === 'inbox') {
      inbox.value = result
      unreadInbox.value = result.filter(m => m.state === 0).length
    } else if (currentTab === 'sent') sent.value = result
    else if (currentTab === 'blacklist') blacklist.value = result
    else notices.value = result
  } catch { if (request === listRequest) loadError.value = true }
  finally { if (request === listRequest) loading.value = false }
}
async function markRead (id) {
  await messageApi.markRead(id)
  window.dispatchEvent(new Event('messages:changed'))
  await loadAll()
}
function openSend () {
  dialog.visible = true
  dialog.receiveUserId = null
  dialog.name = ''
  dialog.content = ''
}
async function send () {
  if (dialog.sending) return
  if (!dialog.receiveUserId) return ElMessage.warning('请填写接收者用户 ID')
  if (!dialog.content.trim()) return ElMessage.warning('请输入内容')
  dialog.sending = true
  try {
    await messageApi.send({ receiveUserId: Number(dialog.receiveUserId), content: dialog.content.trim() })
    dialog.visible = false
    ElMessage.success('消息发送成功')
    await loadAll()
  } finally { dialog.sending = false }
}

// 打开私信举报弹窗（只能举报收到的私信）
function openReport (m) {
  reportDialog.messageId = m.id
  reportDialog.type = 0
  reportDialog.reason = ''
  reportDialog.visible = true
}

async function submitReport () {
  if (reportDialog.type === 4 && !reportDialog.reason.trim()) {
    return ElMessage.warning('请填写具体违规原因')
  }
  reportDialog.loading = true
  try {
    await messageApi.reportMessage({
      messageId: reportDialog.messageId,
      type: reportDialog.type,
      reason: reportDialog.reason
    })
    reportDialog.visible = false
    ElMessage.success('举报已提交，管理员将尽快核查')
  } finally {
    reportDialog.loading = false
  }
}

// 会话聊天视图状态：与某用户的完整往来私信
const convDialog = reactive({
  visible: false,
  peerId: null,
  peerName: '',
  messages: [],
  content: '',
  loading: false,
  sending: false
})

// 打开与某用户的会话视图：自动加载完整往来并批量已读
// fromSent=true 时对方是 receiveUser（发件箱），否则对方是 postUser（收件箱）
async function openConversation (m, fromSent = false) {
  const peer = fromSent ? m.receiveUser : m.postUser
  if (!peer || !peer.id) return ElMessage.warning('无法打开与该用户的会话')
  convDialog.peerId = peer.id
  convDialog.peerName = peer.nickname || peer.username || '对方'
  convDialog.messages = []
  convDialog.content = ''
  convDialog.visible = true
  await loadConversation()
}

async function loadConversation () {
  convDialog.loading = true
  try {
    convDialog.messages = await messageApi.conversation(convDialog.peerId)
    window.dispatchEvent(new Event('messages:changed'))
    // 会话已自动批量已读，刷新未读角标
    if (tab.value === 'inbox') {
      inbox.value = await messageApi.inbox()
      unreadInbox.value = inbox.value.filter(m => m.state === 0).length
    }
  } finally {
    convDialog.loading = false
  }
}

// 会话内直接发送（走 /message/send，受一条未回复限制约束）
async function sendInConversation () {
  if (!convDialog.content.trim()) return ElMessage.warning('请输入内容')
  convDialog.sending = true
  try {
    await messageApi.send({ receiveUserId: convDialog.peerId, content: convDialog.content.trim() })
    convDialog.content = ''
    await loadConversation()
  } finally {
    convDialog.sending = false
  }
}

// 拉黑发私信的人：二次确认后加入黑名单
async function blockUser (m) {
  if (!m.postUser) return
  const name = m.postUser.nickname || m.postUser.username || '该用户'
  try {
    await ElMessageBox.confirm(`确定拉黑「${name}」吗？拉黑后对方将无法再给你发送私信。`, '拉黑确认', {
      confirmButtonText: '确定拉黑',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await messageApi.blacklistAdd(m.postUser.id)
  ElMessage.success(`已拉黑「${name}」`)
  await loadAll()
}

// 解除拉黑
async function unblockUser (b) {
  const name = b.blockedUser ? (b.blockedUser.nickname || b.blockedUser.username) : '该用户'
  await messageApi.blacklistRemove(b.blockedUserId)
  ElMessage.success(`已解除对「${name}」的拉黑`)
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div class="page messages-page">
    <header class="page-heading"><h1>消息中心</h1></header>
    <div class="card messages-card">
      <div class="messages-top between">
        <el-tabs v-model="tab" @tab-change="loadAll" class="msg-tabs">
          <el-tab-pane name="inbox">
            <template #label>
              <span class="tab-label-custom">
                收件箱
                <span v-if="unreadInbox" class="unread-pill">{{ unreadInbox }}</span>
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="已发送" name="sent" />
          <el-tab-pane label="系统通知" name="notice" />
          <el-tab-pane label="黑名单" name="blacklist" />
        </el-tabs>

        <el-button type="primary" :icon="Promotion" round @click="openSend">
          发私信
        </el-button>
      </div>

      <!-- 收件箱列表 -->
      <div v-if="loading" class="tab-flow"><el-skeleton :rows="6" animated /></div>
      <div v-else-if="loadError" class="empty-state" role="alert"><h3>消息加载失败</h3><button class="outline-button" @click="loadAll">重新加载</button></div>
      <div v-else-if="tab === 'inbox'" class="tab-flow">
        <div v-if="!inbox.length" class="empty-box">
          <el-empty description="收件箱暂无私信" :image-size="100" />
        </div>
        <div v-else class="msg-items">
          <div
            v-for="(m, index) in inbox"
            :key="m.id"
            class="msg-row anim-fade-up"
            :class="{ 'is-unread': m.state === 0 }"
            :style="{ animationDelay: Math.min(index * 40, 400) + 'ms' }"
          >
            <el-avatar :size="40" :src="m.postUser && m.postUser.avatarUrl || ''" class="m-avatar">
              {{ m.postUser ? (m.postUser.nickname || m.postUser.username || '?').slice(0, 1) : '系' }}
            </el-avatar>
            <div class="m-main">
              <div class="m-header">
                <span class="m-sender">
                  {{ m.postUser ? (m.postUser.nickname || m.postUser.username) : '系统提示' }}
                </span>
                <span v-if="m.state === 0" class="new-dot">NEW</span>
                <span class="m-time">{{ m.createTime }}</span>
              </div>
              <div class="m-text">{{ m.content }}</div>
              <div class="m-actions">
                <el-button v-if="m.state === 0" text size="small" :icon="Check" @click="markRead(m.id)">
                  标为已读
                </el-button>
                <el-button text size="small" :icon="ChatDotRound" @click="openConversation(m)">
                  查看会话
                </el-button>
                <template v-if="m.postUser">
                  <el-button text size="small" :icon="Warning" @click="openReport(m)">
                    举报
                  </el-button>
                  <el-button text size="small" :icon="CircleClose" @click="blockUser(m)">
                    拉黑
                  </el-button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 已发送列表 -->
      <div v-else-if="tab === 'sent'" class="tab-flow">
        <div v-if="!sent.length" class="empty-box">
          <el-empty description="还没有发送过私信" :image-size="100" />
        </div>
        <div v-else class="msg-items">
          <div
            v-for="(m, index) in sent"
            :key="m.id"
            class="msg-row anim-fade-up"
            :style="{ animationDelay: Math.min(index * 40, 400) + 'ms' }"
          >
            <el-avatar :size="40" class="m-avatar">我</el-avatar>
            <div class="m-main">
              <div class="m-header">
                <span class="m-sender">
                  发送给 <strong>{{ m.receiveUser ? (m.receiveUser.nickname || m.receiveUser.username) : '目标同学' }}</strong>
                </span>
                <span class="m-time">{{ m.createTime }}</span>
              </div>
              <div class="m-text">{{ m.content }}</div>
              <div class="m-actions">
                <el-button text size="small" :icon="ChatDotRound" @click="openConversation(m, true)">
                  查看会话
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 黑名单管理 -->
      <div v-else-if="tab === 'blacklist'" class="tab-flow">
        <div class="blacklist-tip">
          拉黑后对方将无法向你发送私信；你向对方发送私信也会被限制。解除拉黑后恢复正常。
        </div>
        <div v-if="!blacklist.length" class="empty-box">
          <el-empty description="黑名单为空，暂无被拉黑的用户" :image-size="100" />
        </div>
        <div v-else class="msg-items">
          <div
            v-for="(b, index) in blacklist"
            :key="b.id"
            class="msg-row anim-fade-up"
            :style="{ animationDelay: Math.min(index * 40, 400) + 'ms' }"
          >
            <el-avatar :size="40" :src="b.blockedUser && b.blockedUser.avatarUrl || ''" class="m-avatar">
              {{ b.blockedUser ? (b.blockedUser.nickname || b.blockedUser.username || '?').slice(0, 1) : '?' }}
            </el-avatar>
            <div class="m-main">
              <div class="m-header">
                <span class="m-sender">{{ b.blockedUser ? (b.blockedUser.nickname || b.blockedUser.username) : '未知用户' }}</span>
                <span class="blacklist-time">{{ b.createTime }}</span>
              </div>
              <div class="m-text">已被拉黑，无法向你发送私信</div>
              <div class="m-actions">
                <el-button text size="small" type="primary" @click="unblockUser(b)">
                  解除拉黑
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 系统通知列表 -->
      <div v-else class="tab-flow">
        <div v-if="!notices.length" class="empty-box">
          <el-empty description="暂无系统通知" :image-size="100" />
        </div>
        <div v-else class="msg-items">
          <div
            v-for="(n, index) in notices"
            :key="n.id"
            class="msg-row notice-row anim-fade-up"
            :style="{ animationDelay: Math.min(index * 40, 400) + 'ms' }"
          >
            <div class="notice-icon"><el-icon><Bell /></el-icon></div>
            <div class="m-main">
              <div class="m-header">
                <span class="m-sender">{{ n.title }}</span>
                <span class="m-time">{{ n.createTime }}</span>
              </div>
              <div class="m-text">{{ n.content }}</div>
              <div v-if="n.articleId" class="m-actions">
                <router-link :to="`/article/${n.articleId}`" class="notice-link">
                  查看相关帖子详情 &rarr;
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发送站内信对话框 -->
    <el-dialog v-model="dialog.visible" title="发送站内私信" width="460px">
      <el-form label-position="top">
        <el-form-item label="接收者用户 ID（可在对方个人主页URL中查看）" required>
          <el-input v-model="dialog.receiveUserId" placeholder="例如：2" />
        </el-form-item>
        <el-form-item label="私信正文" required>
          <el-input
            v-model="dialog.content"
            type="textarea"
            :rows="4"
            placeholder="输入你想对该同学说的话…"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.sending" @click="send">发送私信</el-button>
      </template>
    </el-dialog>

    <!-- 会话聊天对话框：与某用户的完整往来，气泡式 -->
    <el-dialog v-model="convDialog.visible" :title="`与 ${convDialog.peerName} 的会话`" width="520px">
      <div v-loading="convDialog.loading" class="conv-body">
        <div v-if="!convDialog.messages.length && !convDialog.loading" class="empty-box">
          <el-empty description="暂无私信往来" :image-size="80" />
        </div>
        <div
          v-for="m in convDialog.messages"
          :key="m.id"
          class="conv-row"
          :class="{ 'conv-mine': m.postUserId !== convDialog.peerId }"
        >
          <div class="conv-bubble">
            <div class="conv-content">{{ m.content }}</div>
            <div class="conv-time">{{ m.createTime }}</div>
          </div>
        </div>
      </div>
      <div class="conv-input-row">
        <el-input
          v-model="convDialog.content"
          type="textarea"
          :rows="2"
          maxlength="255"
          placeholder="输入回复内容（对方回复前你只能再发一条）…"
          @keyup.ctrl.enter="sendInConversation"
        />
        <el-button type="primary" :loading="convDialog.sending" :icon="Promotion" @click="sendInConversation">
          发送
        </el-button>
      </div>
    </el-dialog>

    <!-- 举报私信对话框 -->
    <el-dialog v-model="reportDialog.visible" title="举报私信" width="460px">
      <el-form label-position="top">
        <el-form-item label="违规类型" required>
          <el-radio-group v-model="reportDialog.type">
            <el-radio :label="0">垃圾广告</el-radio>
            <el-radio :label="1">色情低俗</el-radio>
            <el-radio :label="2">辱骂攻击</el-radio>
            <el-radio :label="3">违法违规</el-radio>
            <el-radio :label="4">其他说明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="reportDialog.reason"
            type="textarea"
            :rows="3"
            placeholder="可补充违规细节，帮助管理员更快核查…"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="reportDialog.loading" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.messages-page {
  max-width: var(--page-width);
  margin: 0 auto;
  padding: 0 0 32px;
}

/* 黑名单说明提示条 */
.blacklist-tip {
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  background: var(--teal-pale-2);
  color: var(--teal-dark);
  font-size: 13px;
}

.blacklist-time {
  margin-left: auto;
  color: var(--muted);
  font-size: 12px;
}

/* 会话聊天视图：气泡式 */
.conv-body {
  max-height: 380px;
  overflow-y: auto;
  padding: 4px 2px;
}

.conv-row {
  display: flex;
  margin-bottom: 10px;
}

.conv-mine {
  justify-content: flex-end;
}

.conv-bubble {
  max-width: 78%;
  padding: 8px 12px;
  border-radius: var(--radius);
  background: var(--teal-pale-2);
  color: var(--ink);
}

.conv-mine .conv-bubble {
  background: var(--teal);
  color: #fff;
}

.conv-content {
  font-size: 13.5px;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
}

.conv-time {
  margin-top: 4px;
  font-size: 11px;
  opacity: 0.65;
}

.conv-input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.messages-card {
  padding: 24px 28px;
  background: var(--card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}

.messages-top {
  border-bottom: 1px solid var(--line);
  padding-bottom: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 14px;
}

.tab-label-custom {
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-pill {
  font-size: 11px;
  background: var(--red);
  color: #fff;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 700;
}

.tab-flow {
  min-height: 280px;
}

.msg-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--card);
  transition: background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease);
}

.msg-row:hover {
  background: var(--paper);
  border-color: var(--line-strong);
  box-shadow: var(--shadow-xs);
}

/* 未读消息：左侧 3px 品牌色竖条提示 */
.msg-row.is-unread {
  background: var(--teal-pale-2);
  border-color: #bfe0da;
}

.msg-row.is-unread::before {
  content: "";
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--teal);
}

.m-avatar {
  border: 1px solid var(--line);
  flex-shrink: 0;
  background: var(--paper);
  color: var(--teal-dark);
}

.notice-icon {
  font-size: 20px;
  width: 40px;
  height: 40px;
  background: var(--teal-pale);
  color: var(--teal);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.m-main {
  flex: 1;
  min-width: 0;
}

.m-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.m-sender {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
}

.new-dot {
  font-size: 10px;
  background: var(--teal);
  color: #fff;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 700;
}

.m-time {
  font-size: 12px;
  color: var(--muted);
  margin-left: auto;
}

.m-text {
  font-size: 13.5px;
  color: var(--ink-light);
  line-height: 1.6;
  word-break: break-word;
}

.m-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.notice-link {
  font-size: 12.5px;
  color: var(--teal);
  font-weight: 600;
}

.notice-link:hover {
  color: var(--teal-dark);
  text-decoration: underline;
}

.empty-box {
  padding: 40px;
  text-align: center;
}

.reply-target-tip {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 12px;
}
@media (max-width: 600px) {
  .messages-card { padding: 18px 16px; }
  .messages-top { display: block; }
  .messages-top > .el-button { margin-top: 12px; }
  .msg-row { padding: 16px 0; border: 0; border-bottom: 1px solid var(--line); border-radius: 0; gap: 10px; }
  .m-main { min-width: 0; }
  .m-header { flex-wrap: wrap; gap: 6px; }
  .m-text { overflow-wrap: anywhere; }
  .m-time { font-size: 11px; }
  .m-actions { display: flex; flex-wrap: wrap; gap: 4px; }
  .m-actions :deep(.el-button) { margin: 0; }
}
</style>
