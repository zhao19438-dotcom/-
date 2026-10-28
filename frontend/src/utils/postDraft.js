// 草稿按账号与帖子隔离；不读取旧版共用键，避免串号恢复。
export function createPostDraft (storage, userId, postId = 'new') {
  const key = `campus:post-draft:${userId}:${postId || 'new'}`
  return {
    read () { try { return JSON.parse(storage.getItem(key) || 'null') } catch { return null } },
    write (value) { try { storage.setItem(key, JSON.stringify(value)); return true } catch { return false } },
    clear () { try { storage.removeItem(key) } catch { /* 本机存储不可用。 */ } }
  }
}

export function postSnapshot (form, lostFound) {
  return JSON.stringify({ title: form.title, boardId: String(form.boardId || ''), content: form.content, lf: { ...lostFound } })
}
