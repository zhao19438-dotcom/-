import request from './request'
import { postForm } from './http'

// 用户相关接口：注册、登录、资料、密码、头像、收藏
export const userApi = {
  register: (data) => postForm('/user/register', data),
  login: (data) => postForm('/user/login', data),
  logout: () => postForm('/user/logout', {}),
  securityQuestion: (username) => request.get('/user/securityQuestion', { params: { username } }),
  resetPwd: (data) => postForm('/user/resetPwd', data),
  setSecurity: (data) => postForm('/user/setSecurity', data),
  info: (id) => request.get('/user/info', { params: id ? { id } : {} }),
  modifyInfo: (data) => postForm('/user/modifyInfo', data),
  modifyPwd: (data) => postForm('/user/modifyPwd', data),
  uploadAvatar: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/user/avatar', form)
  },
  collections: (page = 1, size = 10) =>
    request.get('/article/collections', { params: { page, size } })
}
