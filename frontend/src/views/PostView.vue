<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Picture } from '@element-plus/icons-vue'
import { articleApi, boardApi } from '../api/article'
import { renderMarkdown } from '../utils/markdown'
import { useUserStore } from '../stores/user'
import { createPostDraft, postSnapshot } from '../utils/postDraft'

const route = useRoute()
const router = useRouter()
const editId = route.params.id || route.query.id || ''
const isEdit = !!editId

const form = reactive({
  title: '',
  boardId: '',
  content: '',
  extJson: ''
})

// 失物招领扩展信息
const lfForm = reactive({
  lostType: 'found', // found = 拾得招领, lost = 寻物求助
  itemName: '',
  location: '',
  itemTime: '',
  contact: ''
})

const boards = ref([])
const submitting = ref(false)
const showPreview = ref(false)
const initializing = ref(true)
const initError = ref(false)

// ---------- 图片上传 ----------
const imageInputRef = ref(null)
const textareaRef = ref(null)
const uploadingImage = ref(false)

function pickImage () {
  imageInputRef.value && imageInputRef.value.click()
}

// 把文本插入正文输入框的光标位置
function insertAtCursor (text) {
  const el = textareaRef.value && textareaRef.value.$el
    ? textareaRef.value.$el.querySelector('textarea')
    : null
  if (!el) {
    form.content += text
    return
  }
  const start = el.selectionStart ?? form.content.length
  const end = el.selectionEnd ?? form.content.length
  form.content = form.content.slice(0, start) + text + form.content.slice(end)
}

async function onImageChange (event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    return ElMessage.warning('图片文件不能超过 5MB')
  }
  uploadingImage.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const data = await articleApi.uploadImage(fd)
    if (data && data.url) {
      insertAtCursor(`\n![图片](${data.url})\n`)
      ElMessage.success('图片上传成功，已插入正文')
    }
  } catch (e) {
    // 失败提示由全局拦截器统一处理
  } finally {
    uploadingImage.value = false
  }
}

// 草稿按用户和帖子隔离，离开或关闭前同步保存未提交修改。
const draft = createPostDraft(localStorage, useUserStore().userInfo.id, editId || 'new')
const baseline = ref('')
const dirty = computed(() => draftRestored.value && postSnapshot(form, lfForm) !== baseline.value)
const draftRestored = ref(false)
let draftTimer = null
let justPublished = false

function saveDraft () {
  if (!draftRestored.value || justPublished) return
  try {
    draft.write({
      title: form.title,
      boardId: form.boardId,
      content: form.content,
      lf: { ...lfForm }
    })
  } catch (e) { /* 存储不可用时静默失败 */ }
}

function clearDraft () {
  draft.clear()
}

// 表单任意字段变化后 600ms 落一次草稿
watch(form, () => {
  if (!draftRestored.value || justPublished) return
  clearTimeout(draftTimer)
  draftTimer = setTimeout(saveDraft, 600)
}, { deep: true })
watch(lfForm, () => {
  if (!draftRestored.value || justPublished) return
  clearTimeout(draftTimer)
  draftTimer = setTimeout(saveDraft, 600)
}, { deep: true })

// 关闭/刷新页面前，有未发布内容时提示
function beforeUnload (e) {
  if (dirty.value && !justPublished) {
    saveDraft()
    e.preventDefault()
    e.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
  clearTimeout(draftTimer)
  if (dirty.value && !justPublished) saveDraft()
})

// 站内路由离开时，有未发布内容先确认
onBeforeRouteLeave(async () => {
  if (justPublished || !dirty.value) return true
  if (submitting.value) return false
  try {
    await ElMessageBox.confirm('有尚未保存的修改，离开后将保留草稿，下次进入本页可恢复。确定离开吗？', '离开确认', {
      confirmButtonText: '离开并保留草稿',
      cancelButtonText: '继续编辑',
      type: 'warning'
    })
    saveDraft()
    return true
  } catch (e) {
    return false
  }
})

const previewHtml = computed(() => renderMarkdown(form.content || ''))

// 是否选定了“失物招领”版块
const isLostFoundBoard = computed(() => {
  const b = boards.value.find(item => String(item.id) === String(form.boardId))
  return b?.bizType === 'lostfound'
})

async function loadBoards () {
  try {
    boards.value = await boardApi.available()
    if (!form.boardId && boards.value.length) {
      const defaultB = boards.value.find(b => b.bizType === 'forum')
        || boards.value.find(b => b.bizType !== 'announcement')
      form.boardId = defaultB?.id || ''
    }
  } catch (e) {
    boards.value = []
    throw e
  }
}

async function loadEdit () {
  const a = await articleApi.detail(editId)
  form.title = a.title || ''
  form.boardId = a.boardId || ''
  form.content = a.content || ''
  form.extJson = a.extJson || ''
  if (a.board && !boards.value.some(b => String(b.id) === String(a.boardId))) {
    boards.value.push({ ...a.board, state: 1 })
  }
  if (a.extJson) {
    try {
      const obj = JSON.parse(a.extJson)
      if (obj && typeof obj === 'object') {
        lfForm.lostType = obj.lostType || 'found'
        lfForm.itemName = obj.itemName || ''
        lfForm.location = obj.location || ''
        lfForm.itemTime = obj.itemTime || ''
        lfForm.contact = obj.contact || ''
      }
    } catch (e) {}
  }
}

async function onSubmit () {
  if (submitting.value || uploadingImage.value) return
  if (!form.boardId) return ElMessage.warning('请选择所属讨论版块')
  if (!form.title.trim()) return ElMessage.warning('请输入帖子标题')
  if (!form.content.trim()) return ElMessage.warning('请输入帖子正文内容')

  // 如果是在失物招领版块且填写了信息，序列化为 JSON 扩展字段
  if (isLostFoundBoard.value) {
    if (!lfForm.contact.trim()) return ElMessage.warning('请填写联系方式')
    form.extJson = JSON.stringify({
      ...JSON.parse(form.extJson || '{}'),
      lostType: lfForm.lostType,
      itemName: lfForm.itemName.trim(),
      location: lfForm.location.trim(),
      itemTime: lfForm.itemTime.trim(),
      contact: lfForm.contact.trim()
    })
  } else if (!isLostFoundBoard.value) {
    form.extJson = ''
  }

  submitting.value = true
  try {
    if (isEdit) {
      await articleApi.modify({ id: editId, ...form })
      justPublished = true
      clearTimeout(draftTimer)
      clearDraft()
      ElMessage.success('帖子更新成功')
      router.push(`/article/${editId}`)
    } else {
      await articleApi.create(form)
      justPublished = true
      clearTimeout(draftTimer)
      clearDraft()
      ElMessage.success('帖子发布成功')
      router.push('/')
    }
  } finally {
    submitting.value = false
  }
}

async function restoreDraft () {
  try {
    const saved = draft.read()
    if (saved && (saved.title || saved.content)) {
      form.title = saved.title || ''
      form.boardId = saved.boardId || ''
      form.content = saved.content || ''
      if (saved.lf && typeof saved.lf === 'object') {
        Object.assign(lfForm, saved.lf)
      }
      ElMessage.info('已为你恢复上次未发布的草稿')
    }
  } catch (e) { /* 草稿损坏时忽略 */ }
}

async function initialize () {
  initializing.value = true
  initError.value = false
  try {
    await loadBoards()
    if (isEdit) await loadEdit()
    baseline.value = postSnapshot(form, lfForm)
    await restoreDraft()
    draftRestored.value = true
  } catch {
    initError.value = true
  } finally {
    initializing.value = false
  }
}
onMounted(initialize)
</script>

<template>
  <div class="forum-desktop-post-page">
    <!-- 顶部返回导航 -->
    <div class="post-nav-bar anim-fade-up">
      <button class="back-link-btn" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
    </div>

    <!-- 发帖卡片 -->
    <div v-if="initError" class="empty-state" role="alert"><h3>编辑内容加载失败</h3><button class="outline-button" @click="initialize">重新加载</button></div>
    <div v-else v-loading="initializing" class="post-card anim-fade-up" style="animation-delay: 80ms;">
      <div class="post-card-header">
        <h2 class="post-card-title">{{ isEdit ? '编辑帖子' : '发布帖子' }}</h2>
      </div>

      <el-form label-position="top" class="post-form" :disabled="initializing || submitting">
        <!-- 版块选择 -->
        <el-form-item label="所属版块" required>
          <el-select v-model="form.boardId" placeholder="请选择版块" size="large" class="board-selector">
            <el-option
              v-for="b in boards"
              :key="b.id"
              :label="b.name"
              :value="b.id"
              :disabled="b.state === 1"
            />
          </el-select>
        </el-form-item>

        <!-- 帖子标题 -->
        <el-form-item label="帖子标题" required>
          <el-input
            v-model="form.title"
            placeholder="请输入标题 (1-60字)"
            maxlength="60"
            show-word-limit
            size="large"
            clearable
          />
        </el-form-item>

        <!-- 失物招领专属表单 -->
        <div v-if="isLostFoundBoard" class="lostfound-form-wrap">
          <div class="lf-form-title">失物招领登记信息</div>
          <div class="lf-form-grid">
            <el-form-item label="招领类型">
              <el-radio-group v-model="lfForm.lostType" size="small">
                <el-radio-button label="found">拾得招领</el-radio-button>
                <el-radio-button label="lost">寻物求助</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="物品名称">
              <el-input v-model="lfForm.itemName" placeholder="如：黑色双肩包、校园一卡通" />
            </el-form-item>
            <el-form-item label="地点">
              <el-input v-model="lfForm.location" placeholder="如：图书馆二楼借阅区、三食堂" />
            </el-form-item>
            <el-form-item label="联系方式">
              <el-input v-model="lfForm.contact" placeholder="如：微信号、QQ或电话" />
            </el-form-item>
            <el-form-item label="发生时间"><el-input v-model="lfForm.itemTime" placeholder="例如：今天下午三点" /></el-form-item>
          </div>
        </div>

        <!-- 正文内容 -->
        <el-form-item label="正文内容" required>
          <div class="editor-toolbar">
            <div class="toolbar-left">
              <button
                type="button"
                class="image-upload-btn"
                :disabled="uploadingImage"
                @click="pickImage"
              >
                <el-icon><Picture /></el-icon>
                {{ uploadingImage ? '上传中…' : '插入图片' }}
              </button>
              <input
                ref="imageInputRef"
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                style="display: none;"
                @change="onImageChange"
              />
              <span class="toolbar-tip">支持 Markdown 与图片</span>
            </div>
            <div class="toolbar-mode-btns">
              <button
                type="button"
                :class="['mode-tab', { active: !showPreview }]"
                @click="showPreview = false"
              >
                编辑内容
              </button>
              <button
                type="button"
                :class="['mode-tab', { active: showPreview }]"
                @click="showPreview = true"
              >
                实时预览
              </button>
            </div>
          </div>

          <!-- 编辑/预览切换过渡 -->
          <Transition name="editor-switch" mode="out-in">
            <el-input
              v-if="!showPreview"
              key="editor"
              ref="textareaRef"
              v-model="form.content"
              type="textarea"
              :rows="14"
              placeholder="详细阐述你的讨论观点、技术分析或求助细节…"
              class="content-editor-input"
            />

            <!-- 预览模式（markdown-body 提供完整排版样式） -->
            <div v-else key="preview" class="markdown-preview-box markdown-body" v-html="previewHtml"></div>
          </Transition>
        </el-form-item>

        <!-- 提交与取消按钮 -->
        <div class="post-actions-row">
          <el-button size="large" class="cancel-action-btn" @click="router.back()">
            取消
          </el-button>
          <el-button
            type="primary"
            size="large"
            class="submit-action-btn"
            :loading="submitting"
            @click="onSubmit"
          >
            {{ isEdit ? '保存' : '发布' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.forum-desktop-post-page {
  max-width: var(--read-max-width);
  margin: 0 auto;
  padding: 8px 16px 32px;
}

.post-nav-bar {
  margin-bottom: 16px;
}

.back-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: var(--muted);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}

.back-link-btn:hover {
  color: var(--teal);
  background: var(--teal-pale);
}

/* 按压反馈 */
.back-link-btn:active {
  transform: scale(0.96);
}

.post-card {
  background: var(--card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  padding: 32px 36px;
  box-shadow: var(--shadow-sm);
}

.post-card-header {
  border-bottom: 1px solid var(--line);
  padding-bottom: 18px;
  margin-bottom: 24px;
}

.post-card-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 650;
  color: var(--ink);
  letter-spacing: -0.01em;
}

.post-card-sub {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
}

.board-selector {
  width: 100%;
  max-width: 320px;
}

/* 失物招领表单（左侧 teal 边条，hover 轻微提亮边框） */
.lostfound-form-wrap {
  background: var(--teal-pale-2);
  border: 1px solid #cce8e2;
  border-left: 4px solid var(--teal);
  border-radius: var(--radius-md);
  padding: 18px 20px;
  margin-bottom: 22px;
  box-shadow: var(--shadow-xs);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}

.lostfound-form-wrap:hover {
  border-color: var(--c-border-hover);
  box-shadow: var(--shadow-sm);
}

.lf-form-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--teal-dark);
  margin-bottom: 12px;
}

.lf-form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px 16px;
}

/* 编辑器工具栏 */
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  width: 100%;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 插入图片按钮：teal 幽灵按钮风格 */
.image-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-size: 12.5px;
  padding: 5px 12px;
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease),
    background var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}

.image-upload-btn:hover:not(:disabled) {
  color: var(--teal);
  border-color: var(--teal);
  background: var(--teal-pale-2);
}

.image-upload-btn:active:not(:disabled) {
  transform: scale(0.96);
}

.image-upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toolbar-tip {
  font-size: 12px;
  color: var(--muted);
}

.toolbar-mode-btns {
  display: flex;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.mode-tab {
  background: var(--card);
  border: none;
  font-size: 12.5px;
  padding: 5px 14px;
  cursor: pointer;
  color: var(--muted);
  transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}

.mode-tab:not(.active):hover {
  color: var(--teal);
  background: var(--teal-pale-2);
}

/* 按压反馈 */
.mode-tab:active {
  transform: scale(0.95);
}

.mode-tab.active {
  background: var(--teal);
  color: #ffffff;
  font-weight: 600;
}

/* 编辑/预览切换过渡（淡入 + 轻微位移） */
.editor-switch-enter-active,
.editor-switch-leave-active {
  transition: opacity var(--dur) var(--ease), transform var(--dur) var(--ease);
}
.editor-switch-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.editor-switch-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.content-editor-input :deep(.el-textarea__inner) {
  font-size: 14.5px;
  line-height: 1.6;
  font-family: inherit;
  border-radius: var(--radius-md);
  border-color: var(--line);
}

.content-editor-input :deep(.el-textarea__inner:focus) {
  border-color: var(--teal);
}

.markdown-preview-box {
  background: #fbfdfc;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  min-height: 280px;
  color: var(--ink);
  transition: border-color var(--dur) var(--ease);
}

/* hover 时边框呼应全局 hover 色 */
.markdown-preview-box:hover {
  border-color: var(--c-border-hover);
}

/* 按钮行 */
.post-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.cancel-action-btn,
.submit-action-btn {
  border-radius: var(--radius-sm);
  font-weight: 600;
  padding: 0 24px;
  transition: transform var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease);
}

/* 取消按钮 hover 呼应全局 hover 色 */
.cancel-action-btn:hover {
  color: var(--teal);
  border-color: var(--c-border-hover);
  background: var(--teal-pale-2);
}

/* 按压反馈 */
.cancel-action-btn:active,
.submit-action-btn:active {
  transform: scale(0.97);
}

.submit-action-btn {
  background: var(--teal);
  border-color: var(--teal);
}

.submit-action-btn:hover {
  background: var(--teal-dark);
  border-color: var(--teal-dark);
}
@media (max-width: 600px) {
  .forum-desktop-post-page { padding: 0; }
  .post-card { padding: 24px 18px; }
  .post-card-title { font-size: 24px; }
  .editor-toolbar { flex-wrap: wrap; gap: 12px; }
  .toolbar-left { flex-wrap: wrap; }
  .lf-form-grid { grid-template-columns: 1fr; }
  .lostfound-form-wrap { padding: 18px 14px; }
  .post-actions-row { flex-wrap: wrap; }
}
</style>
