
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'


export function useAdminWorkspace () {
const router = useRouter()
const route = useRoute()
const validTabs = ['summary', 'users', 'boards', 'articles', 'sensitive', 'reports', 'audit']
const tab = ref(validTabs.includes(route.query.tab) ? route.query.tab : 'summary')
const loadError = ref(false)
const busy = ref(false)
const pendingReports = ref(0)
const summary = ref({})
const users = ref([])
const boards = ref([])
const articles = ref([])
const loading = ref(false)

const userPage = reactive({ page: 1, size: 10, total: 0, keyword: '' })
const boardPage = reactive({ page: 1, size: 10, total: 0 })
const articlePage = reactive({ page: 1, size: 10, total: 0, keyword: '' })

const boardDialog = reactive({ visible: false, id: null, name: '', sort: 0, state: 0, bizType: 'forum', saving: false })

// 敏感词
const sensitiveWords = ref([])
const sensitiveInput = ref('')

// 举报
const reports = ref([])
const reportPage = reactive({ page: 1, size: 10, total: 0, status: 0 })
const reportTypeLabels = ['垃圾广告', '色情低俗', '辱骂攻击', '违法违规', '其他']
const reportStatusLabels = ['待处理', '已处理', '已驳回']

// 审计日志
const auditLogs = ref([])
const auditPage = reactive({ page: 1, size: 10, total: 0, keyword: '' })

// 动作类型中文映射：把后台接口路径翻译成使用者能看懂的操作说明
const ADMIN_ACTION_MAP = {
  'GET /admin/current': '查看管理员信息',
  'GET /admin/summary': '查看运行数据大盘',
  'GET /admin/users': '浏览用户列表',
  'POST /admin/users/state': '封禁 / 解封用户',
  'POST /admin/users/admin': '调整用户管理权限',
  'POST /admin/users/delete': '注销用户账号',
  'POST /admin/users/resetPassword': '重置用户密码',
  'GET /admin/boards': '浏览版块列表',
  'POST /admin/boards/save': '新增 / 保存版块',
  'POST /admin/boards/state': '开放 / 禁用版块',
  'POST /admin/boards/delete': '删除版块',
  'GET /admin/articles': '浏览帖子列表',
  'POST /admin/articles/state': '屏蔽 / 恢复帖子',
  'POST /admin/articles/delete': '删除帖子',
  'POST /admin/articles/top': '设置 / 取消置顶',
  'POST /admin/articles/essence': '设置 / 取消精华',
  'GET /admin/sensitive/list': '浏览敏感词列表',
  'POST /admin/sensitive/add': '新增敏感词',
  'POST /admin/sensitive/delete': '删除敏感词',
  'POST /admin/sensitive/refresh': '更新敏感词库',
  'GET /admin/report/list': '浏览举报列表',
  'POST /admin/report/handle': '处理违规举报',
  'LOGIN_SUCCESS': '登录成功',
  'LOGIN_FAILED': '登录失败（密码错误）'
}

function formatActionLabel (action) {
  if (!action) return '未知操作'
  // 精确匹配已知动作
  if (ADMIN_ACTION_MAP[action]) return ADMIN_ACTION_MAP[action]
  // 兜底：拦截器记录的 "方法 /路径" 格式，至少把方法名翻译掉
  if (action.includes('/')) {
    const methodLabels = { GET: '查看', POST: '操作' }
    const method = action.split(' ')[0]
    return (methodLabels[method] || '后台') + '后台数据'
  }
  return action
}

// 记录来源翻译
function formatTargetType (type) {
  const labels = {
    login: '登录安全',
    user: '用户管理',
    board: '版块管理',
    article: '帖子管理',
    report: '举报处理',
    sensitive: '敏感词库',
    admin: '后台管理'
  }
  return labels[type] || type || '—'
}

// 操作说明翻译：把写操作的原始参数拼成一句话，浏览类记录不展示查询参数
function formatAuditDetail (row) {
  const action = row.action || ''
  if (!row.detail) return '—'
  if (action.startsWith('GET')) return '—' // 浏览类记录的翻页参数对使用者无意义
  // 已经是自然语言说明（历史记录或人工补录）时直接展示，避免被当成参数解析
  if (!row.detail.includes('=')) return row.detail
  const params = {}
  row.detail.split('&').forEach(p => {
    const i = p.indexOf('=')
    if (i > 0) params[p.slice(0, i)] = p.slice(i + 1)
  })
  const target = (label, key) => (params[key] ? `${label} #${params[key]}` : null)
  const yesNo = (key, yes, no) => (params[key] === '1' ? yes : no)
  // 同时兼容新版中文动作名与历史数据里的「方法 /路径」写法
  const is = (...keys) => keys.some(k => action.includes(k))
  const parts = []
  if (is('/users/state', '变更用户状态')) {
    parts.push(target('用户', 'id'), yesNo('state', '执行封禁，禁止登录发言', '解除封禁，恢复正常'))
  } else if (is('/users/admin', '变更管理员权限')) {
    parts.push(target('用户', 'id'), yesNo('isAdmin', '授予管理员权限', '收回管理员权限'))
  } else if (is('/users/delete', '删除用户')) {
    parts.push(target('用户', 'id'), '注销删除该账号')
  } else if (is('/users/resetPassword', '重置用户密码')) {
    parts.push(target('用户', 'id'), '已生成一次性临时密码')
  } else if (is('/boards/save', '保存版块')) {
    parts.push(params.id ? `版块 #${params.id}` : '新建版块', params.name ? `名称「${params.name}」` : null)
  } else if (is('/boards/state', '变更版块状态')) {
    parts.push(target('版块', 'id'), yesNo('state', '禁用隐藏', '恢复开放'))
  } else if (is('/boards/delete', '删除版块')) {
    parts.push(target('版块', 'id'), '删除该版块')
  } else if (is('/articles/state', '变更帖子状态')) {
    parts.push(target('帖子', 'id'), yesNo('state', '屏蔽隐藏', '恢复展示'))
  } else if (is('/articles/delete', '删除帖子')) {
    parts.push(target('帖子', 'id'), '删除该帖子')
  } else if (is('/articles/top', '设置帖子置顶')) {
    parts.push(target('帖子', 'id'), yesNo('topState', '设为置顶', '取消置顶'))
  } else if (is('/articles/essence', '设置帖子精华')) {
    parts.push(target('帖子', 'id'), yesNo('essenceState', '加为精华', '取消精华'))
  } else if (is('/sensitive/add', '新增敏感词')) {
    parts.push('新增词条', params.word ? `「${params.word}」` : null)
  } else if (is('/sensitive/delete', '删除敏感词')) {
    parts.push(target('删除词条', 'id'))
  } else if (is('/sensitive/refresh', '刷新敏感词缓存')) {
    parts.push('重建敏感词缓存')
  } else if (is('/report/handle', '处理举报')) {
    const sanctionLabels = { none: '', warn: '并向违规方发出警告', delete: '删除违规内容并警告', ban: '并将违规方禁言' }
    parts.push(
      target('举报', 'id'),
      params.status === '1' ? '核实为违规' : '判定举报不成立',
      sanctionLabels[params.action] || ''
    )
  } else {
    return row.detail // 未覆盖的操作保留原文，避免误导
  }
  return parts.filter(Boolean).join('，') || '—'
}



async function loadSummary () {
  loading.value = true
  try {
    summary.value = await adminApi.summary()
    // 顺便拉取待处理举报数用于气泡提醒
    const rep = await adminApi.reportList({ status: 0, page: 1, size: 1 })
    pendingReports.value = rep.total || 0
  } finally {
    loading.value = false
  }
}

async function loadUsers () {
  loading.value = true
  try {
    const r = await adminApi.users({ keyword: userPage.keyword || undefined, page: userPage.page, size: userPage.size })
    users.value = r.records || []
    userPage.total = r.total || 0
  } finally { loading.value = false }
}

async function loadBoards () {
  loading.value = true
  try {
    const r = await adminApi.boards({ page: boardPage.page, size: boardPage.size })
    boards.value = r.records || []
    boardPage.total = r.total || 0
  } finally { loading.value = false }
}

async function loadArticles () {
  loading.value = true
  try {
    const r = await adminApi.articles({ keyword: articlePage.keyword || undefined, page: articlePage.page, size: articlePage.size })
    articles.value = r.records || []
    articlePage.total = r.total || 0
  } finally { loading.value = false }
}

// 敏感词
async function loadSensitive () {
  loading.value = true
  try { sensitiveWords.value = await adminApi.sensitiveList() } finally { loading.value = false }
}
async function addSensitive () {
  const w = sensitiveInput.value.trim()
  if (!w) return ElMessage.warning('请输入敏感词内容')
  await adminApi.sensitiveAdd(w)
  sensitiveInput.value = ''
  ElMessage.success('敏感词已添加并生效')
  loadSensitive()
}
async function delSensitive (w) {
  await ElMessageBox.confirm(`确认删除敏感词「${w.word}」？`, '提示', { type: 'warning' })
  await adminApi.sensitiveDelete(w.id)
  ElMessage.success('敏感词已移除')
  loadSensitive()
}
async function refreshSensitive () {
  await adminApi.sensitiveRefresh()
  ElMessage.success('敏感词库已更新')
}

// 举报处理
async function loadReports () {
  loading.value = true
  try {
    const r = await adminApi.reportList({ status: reportPage.status ?? undefined, page: reportPage.page, size: reportPage.size })
    reports.value = r.records || []
    reportPage.total = r.total || 0
    if (reportPage.status === 0) pendingReports.value = reportPage.total
  } finally { loading.value = false }
}
// 举报处置对话框：批准时可选择对违规方的联动制裁
const handleDialog = reactive({
  visible: false,
  report: null,
  status: 1,
  action: 'none',
  handleResult: '',
  loading: false
})

function openHandleDialog (r, status) {
  handleDialog.report = r
  handleDialog.status = status
  handleDialog.action = status === 1 ? 'warn' : 'none'
  handleDialog.handleResult = status === 1 ? '已核实处理' : '举报不成立，予以驳回'
  handleDialog.visible = true
}

async function submitHandle () {
  if (!handleDialog.handleResult.trim()) return ElMessage.warning('请填写处理说明')
  handleDialog.loading = true
  try {
    await adminApi.reportHandle({
      id: handleDialog.report.id,
      status: handleDialog.status,
      handleResult: handleDialog.handleResult,
      action: handleDialog.status === 1 ? handleDialog.action : 'none'
    })
    handleDialog.visible = false
    const actionLabels = { none: '', warn: '，已向违规方发出警告', delete: '，违规内容已删除并警告', ban: '，违规账号已禁言' }
    ElMessage.success('处理完成' + (handleDialog.status === 1 ? actionLabels[handleDialog.action] : ''))
    await loadReports()
    const pending = await adminApi.reportList({ status: 0, page: 1, size: 1 })
    pendingReports.value = pending.total || 0
  } finally { handleDialog.loading = false }
}

// 审计日志
async function loadAudit () {
  loading.value = true
  try {
    const r = await adminApi.auditList({ keyword: auditPage.keyword || undefined, page: auditPage.page, size: auditPage.size })
    auditLogs.value = r.records || []
    auditPage.total = r.total || 0
  } finally { loading.value = false }
}

async function refreshPanel () {
  loadError.value = false
  try {
    const loaders = { summary: loadSummary, users: loadUsers, boards: loadBoards, articles: loadArticles, sensitive: loadSensitive, reports: loadReports, audit: loadAudit }
    await loaders[tab.value]()
  } catch { loadError.value = true }
}
function onTabChange (name) {
  if (!validTabs.includes(name)) return
  tab.value = name
  if (route.query.tab === name) return refreshPanel()
  router.replace({ query: { ...route.query, tab: name } })
}

// 用户操作
async function setUserState (u) {
  const next = u.state === 1 ? 0 : 1
  const tip = next === 1 ? `确认封禁用户「${u.username}」？` : `确认解封用户「${u.username}」？`
  await ElMessageBox.confirm(tip, '状态变更', { type: 'warning' })
  await adminApi.updateUserState(u.id, next)
  u.state = next
  ElMessage.success(next === 1 ? '该用户已被封禁禁言' : '该用户已解封恢复正常')
}
async function setUserAdmin (u) {
  const next = u.isAdmin === 1 ? 0 : 1
  const tip = next === 1 ? `确认将「${u.username}」提升为系统管理员？` : `确认取消「${u.username}」的管理员权限？`
  await ElMessageBox.confirm(tip, '权限变更', { type: 'warning' })
  await adminApi.updateUserAdmin(u.id, next)
  u.isAdmin = next
  ElMessage.success('权限更新成功')
}
async function delUser (u) {
  await ElMessageBox.confirm(`确认注销用户「${u.username}」？`, '注销用户', { type: 'warning' })
  await adminApi.updateUserDelete(u.id, 1)
  u.deleteState = 1
  ElMessage.success('已注销该用户')
  if (users.value.length === 1 && userPage.page > 1) userPage.page--
  await loadUsers()
}
async function resetPwd (u) {
  await ElMessageBox.confirm(`确认重置用户「${u.username}」的密码？系统将生成一次性临时密码。`, '重置密码', { type: 'warning' })
  const temporaryPassword = await adminApi.resetPassword(u.id)
  await ElMessageBox.alert(`临时密码：${temporaryPassword}\n请立即复制并告知用户。`, '密码重置成功', {
    confirmButtonText: '我已保存',
    type: 'success'
  })
}

// 版块操作
function openBoard (b) {
  boardDialog.visible = true
  boardDialog.id = b ? b.id : null
  boardDialog.name = b ? b.name : ''
  boardDialog.sort = b ? b.sort : 0
  boardDialog.state = b ? b.state : 0
  boardDialog.bizType = b?.bizType || 'forum'
}
async function saveBoard () {
  if (boardDialog.saving) return
  if (!boardDialog.name.trim()) return ElMessage.warning('请输入版块名称')
  boardDialog.saving = true
  try {
    await adminApi.saveBoard({
    id: boardDialog.id || undefined,
    name: boardDialog.name.trim(),
    sort: Number(boardDialog.sort),
    state: Number(boardDialog.state),
    bizType: boardDialog.bizType
  })
  boardDialog.visible = false
  ElMessage.success('版块配置保存成功')
  window.dispatchEvent(new Event('boards:changed'))
    if (tab.value === 'summary') await loadSummary()
    else await loadBoards()
  } finally { boardDialog.saving = false }
}
async function setBoardState (b) {
  const next = b.state === 1 ? 0 : 1
  await adminApi.updateBoardState(b.id, next)
  window.dispatchEvent(new Event('boards:changed'))
  b.state = next
  ElMessage.success(next === 1 ? '版块已禁用' : '版块已开放')
}
async function delBoard (b) {
  await ElMessageBox.confirm(`确认删除版块「${b.name}」？此操作将隐藏该版块。`, '提示', { type: 'warning' })
  await adminApi.updateBoardDelete(b.id, 1)
  window.dispatchEvent(new Event('boards:changed'))
  b.deleteState = 1
  ElMessage.success('版块已移除')
  if (boards.value.length === 1 && boardPage.page > 1) boardPage.page--
  await loadBoards()
}

// 帖子操作
async function toggleTop (a) {
  const next = a.topState === 1 ? 0 : 1
  await adminApi.updateArticleTop(a.id, next)
  a.topState = next
  ElMessage.success(next === 1 ? '已将该贴置顶' : '已取消置顶')
}
async function toggleEssence (a) {
  const next = a.essenceState === 1 ? 0 : 1
  await adminApi.updateArticleEssence(a.id, next)
  a.essenceState = next
  ElMessage.success(next === 1 ? '已加为精华帖' : '已取消精华')
}
async function setArticleState (a) {
  const next = a.state === 1 ? 0 : 1
  await adminApi.updateArticleState(a.id, next)
  a.state = next
  ElMessage.success(next === 1 ? '帖子已被隐藏屏蔽' : '帖子恢复正常展示')
}
async function delArticle (a) {
  await ElMessageBox.confirm(`确认删除帖子「${a.title}」？`, '删除确认', { type: 'warning' })
  await adminApi.updateArticleDelete(a.id, 1)
  a.deleteState = 1
  ElMessage.success('帖子已删除')
  if (articles.value.length === 1 && articlePage.page > 1) articlePage.page--
  await loadArticles()
}

watch(() => route.query.tab, (value) => {
  tab.value = validTabs.includes(value) ? value : 'summary'
  refreshPanel()
}, { immediate: true })

// All panels keep the same cancellation/error behavior and block repeated writes.
function guardAction (action) {
  return async (...args) => {
    if (busy.value) return
    busy.value = true
    try { return await action(...args) } catch (e) {
      if (e !== 'cancel' && e !== 'close' && !e?.isAxiosError && !e?.message) console.error(e)
    } finally { busy.value = false }
  }
}

function safeLoad (loader) {
  return async (...args) => {
    loadError.value = false
    try { return await loader(...args) } catch { loadError.value = true }
  }
}

return { router, tab, summary, users, boards, articles, loading, loadError, busy, pendingReports, refreshPanel, userPage, boardPage, articlePage, boardDialog, sensitiveWords, sensitiveInput, reports, reportPage, reportTypeLabels, reportStatusLabels, auditLogs, auditPage, formatActionLabel, formatTargetType, formatAuditDetail, handleDialog, openHandleDialog, onTabChange, openBoard,
  loadSummary: safeLoad(loadSummary), loadUsers: safeLoad(loadUsers), loadBoards: safeLoad(loadBoards), loadArticles: safeLoad(loadArticles),
  loadSensitive: safeLoad(loadSensitive), loadReports: safeLoad(loadReports), loadAudit: safeLoad(loadAudit),
  addSensitive: guardAction(addSensitive), delSensitive: guardAction(delSensitive), refreshSensitive: guardAction(refreshSensitive),
  submitHandle: guardAction(submitHandle), setUserState: guardAction(setUserState), setUserAdmin: guardAction(setUserAdmin),
  delUser: guardAction(delUser), resetPwd: guardAction(resetPwd), saveBoard: guardAction(saveBoard),
  setBoardState: guardAction(setBoardState), delBoard: guardAction(delBoard), toggleTop: guardAction(toggleTop),
  toggleEssence: guardAction(toggleEssence), setArticleState: guardAction(setArticleState), delArticle: guardAction(delArticle) }
}
