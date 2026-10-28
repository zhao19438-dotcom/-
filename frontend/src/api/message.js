import request from './request'
import { postForm } from './http'

// 站内信、通知与黑名单相关接口，和后台管理接口保持独立。
export const messageApi = {
  unreadCount: () => request.get('/message/getUnreadCount'),
  inbox: () => request.get('/message/inbox'),
  sent: () => request.get('/message/sent'),
  notifications: (limit = 50) => request.get('/message/notifications', { params: { limit } }),
  send: (data) => postForm('/message/send', data),
  markRead: (id) => postForm('/message/markRead', { id }),
  reply: (data) => postForm('/message/reply', data),
  conversation: (peerId) => request.get('/message/conversation', { params: { peerId } }),
  blacklistList: () => request.get('/message/blacklist/list'),
  blacklistAdd: (blockedUserId) => postForm('/message/blacklist/add', { blockedUserId }),
  blacklistRemove: (blockedUserId) => postForm('/message/blacklist/remove', { blockedUserId }),
  reportMessage: (data) => postForm('/message/report', data)
}
