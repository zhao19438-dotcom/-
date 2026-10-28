import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createPostDraft, postSnapshot } from './postDraft.js'

test('drafts are isolated by account and edited article', () => {
  const values = new Map()
  const storage = { getItem: key => values.get(key), setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
  const alice = createPostDraft(storage, 1)
  alice.write({ title: '私人草稿' })
  assert.equal(createPostDraft(storage, 2).read(), null)
  assert.equal(createPostDraft(storage, 1, 10).read(), null)
  assert.equal(alice.read().title, '私人草稿')
  const edit = createPostDraft(storage, 1, 10)
  edit.write({ title: '编辑草稿' }); edit.clear()
  assert.equal(edit.read(), null)
  assert.equal(alice.read().title, '私人草稿')
})
test('dirty detection includes board and lost-found fields', () => {
  const form = { title: '标题', boardId: 1, content: '正文' }
  const original = postSnapshot(form, { contact: '原联系方式' })
  assert.notEqual(original, postSnapshot(form, { contact: '新联系方式' }))
  assert.notEqual(original, postSnapshot({ ...form, boardId: 2 }, { contact: '原联系方式' }))
  assert.equal(original, postSnapshot({ ...form, boardId: '1' }, { contact: '原联系方式' }))
})
