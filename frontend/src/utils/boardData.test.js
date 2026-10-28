import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const seed = readFileSync(new URL('../../../sql/forum_init.sql', import.meta.url), 'utf8')

test('fresh installations classify the campus announcement board explicitly', () => {
  const board = seed.split('\n').find(line => line.includes('INSERT INTO `t_board`') && line.includes("'校园公告'"))
  assert.match(board, /'announcement'\);/)
})

test('announcement articles match their board business type in the initial data', () => {
  const articles = seed.split('\n').filter(line => /INSERT INTO `t_article`.*VALUES \(\d+,14,/.test(line))
  assert.ok(articles.length > 0)
  for (const article of articles) assert.match(article, /'announcement',/)
})
