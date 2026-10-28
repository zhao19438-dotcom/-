import request from './request'

/** 以表单编码提交（后端多数写接口按 application/x-www-form-urlencoded 解析） */
export function postForm (url, data) {
  return request.post(url, data, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    transformRequest: [(obj) => new URLSearchParams(
      Object.entries(obj || {}).filter(([, value]) => value !== undefined && value !== null)
    ).toString()]
  })
}
