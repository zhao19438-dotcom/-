import request from './request'
import { postForm } from './http'

// 后台管理接口（需要管理员身份，非管理员返回 403）
export const adminApi = {
  current: () => request.get('/admin/current'),
  summary: () => request.get('/admin/summary'),
  users: (params) => request.get('/admin/users', { params }),
  boards: (params) => request.get('/admin/boards', { params }),
  articles: (params) => request.get('/admin/articles', { params }),
  updateUserState: (id, state) => postForm('/admin/users/state', { id, state }),
  updateUserAdmin: (id, isAdmin) => postForm('/admin/users/admin', { id, isAdmin }),
  updateUserDelete: (id, deleteState) => postForm('/admin/users/delete', { id, deleteState }),
  resetPassword: (id) => postForm('/admin/users/resetPassword', { id }),
  saveBoard: (data) => postForm('/admin/boards/save', data),
  updateBoardState: (id, state) => postForm('/admin/boards/state', { id, state }),
  updateBoardDelete: (id, deleteState) => postForm('/admin/boards/delete', { id, deleteState }),
  updateArticleState: (id, state) => postForm('/admin/articles/state', { id, state }),
  updateArticleDelete: (id, deleteState) => postForm('/admin/articles/delete', { id, deleteState }),
  updateArticleTop: (id, topState) => postForm('/admin/articles/top', { id, topState }),
  updateArticleEssence: (id, essenceState) => postForm('/admin/articles/essence', { id, essenceState }),
  // 内容审核：敏感词管理
  sensitiveList: () => request.get('/admin/sensitive/list'),
  sensitiveAdd: (word) => postForm('/admin/sensitive/add', { word }),
  sensitiveDelete: (id) => postForm('/admin/sensitive/delete', { id }),
  sensitiveRefresh: () => postForm('/admin/sensitive/refresh'),
  // 举报处理
  reportList: (params) => request.get('/admin/report/list', { params }),
  reportHandle: (data) => postForm('/admin/report/handle', data),
  // 审计日志
  auditList: (params) => request.get('/admin/audit/list', { params })
}
