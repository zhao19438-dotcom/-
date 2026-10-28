import request from './request'
import { postForm } from './http'

// 帖子与版块接口（分页统一返回 PageResult：records/total/page/size/pages）
export const articleApi = {
  list: (params) => request.get('/article/getAllByBoardId', { params }),
  search: (params) => request.get('/article/search', { params }),
  detail: (id) => request.get('/article/details', { params: { id } }),
  create: (data) => postForm('/article/create', data),
  modify: (data) => postForm('/article/modify', data),
  delete: (id) => postForm('/article/delete', { id }),
  thumbsUp: (id) => postForm('/article/thumbsUp', { id }),
  thumbsDown: (id) => postForm('/article/thumbsDown', { id }),
  collect: (id) => postForm('/article/collect', { id }),
  uncollect: (id) => postForm('/article/uncollect', { id }),
  report: (data) => postForm('/article/report', data),
  byUser: (userId) => request.get('/article/getAllByUserId', { params: userId ? { userId } : {} }),
  uploadImage: (formData) => request.post('/article/uploadImage', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const replyApi = {
  list: (articleId) => request.get('/reply/getReplies', { params: { articleId } }),
  create: (data) => postForm('/reply/create', data),
  thumbsUp: (id) => postForm('/reply/thumbsUp', { id }),
  thumbsDown: (id) => postForm('/reply/thumbsDown', { id })
}

export const boardApi = {
  available: () => request.get('/board/available'),
  topList: () => request.get('/board/topList'),
  getById: (id) => request.get('/board/getById', { params: { id } })
}
