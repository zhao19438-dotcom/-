<script setup>
import { computed, ref, watch } from 'vue'
import { ChatDotRound, View, Pointer } from '@element-plus/icons-vue'
import UserAvatar from './UserAvatar.vue'
import { displayName, getSnippet, relativeTime } from '../utils/format'

const props = defineProps({ article: { type: Object, required: true } })
const imageFailed = ref(false)
const image = computed(() => {
  const text = props.article.content || ''
  return text.match(/!\[[^\]]*\]\((https?:\/\/[^\s)]+|\/uploads\/[^\s)]+)\)/i)?.[1]
    || text.match(/<img[^>]+src=["'](https?:\/\/[^"']+|\/uploads\/[^"']+)["']/i)?.[1]
    || ''
})
watch(image, () => { imageFailed.value = false })
</script>

<template>
  <router-link :to="`/article/${article.id}`" class="post-preview">
    <div class="preview-meta">
      <UserAvatar :user="article.user" size="small" />
      <span class="preview-author">{{ displayName(article.user) }}</span>
      <span class="preview-time" :title="article.createTime">{{ relativeTime(article.createTime) }}</span>
      <span v-if="article.board" class="preview-board">{{ article.board.name }}</span>
    </div>
    <div class="preview-body">
      <div class="preview-copy">
        <h2 class="preview-title">
          <span v-if="article.topState === 1" class="preview-label">置顶</span>
          <span v-else-if="article.essenceState === 1" class="preview-label">精华</span>
          {{ article.title }}
        </h2>
        <p v-if="getSnippet(article.content)" class="preview-excerpt">{{ getSnippet(article.content, 160) }}</p>
        <div class="preview-stats">
          <span><el-icon><Pointer /></el-icon>{{ article.likeCount || 0 }} 赞</span>
          <span><el-icon><ChatDotRound /></el-icon>{{ article.replyCount || 0 }} 回复</span>
          <span><el-icon><View /></el-icon>{{ article.visitCount || 0 }} 浏览</span>
        </div>
      </div>
      <img v-if="image && !imageFailed" :src="image" class="preview-image" alt="帖子配图" loading="lazy" @error="imageFailed = true" />
    </div>
  </router-link>
</template>

<style scoped>
.post-preview { display: block; padding: 25px 28px; transition: background 160ms ease; }
.post-preview + .post-preview { border-top: 1px solid var(--line); }
.post-preview:hover { background: #fafcfb; }
.preview-meta { display: flex; align-items: center; gap: 9px; min-width: 0; font-size: 12px; color: var(--muted); }
.preview-meta :deep(.avatar) { width: 25px; height: 25px; font-size: 11px; }
.preview-author { color: var(--ink-soft); max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-time { white-space: nowrap; }
.preview-board { margin-left: auto; color: var(--teal); background: var(--teal-pale); border-radius: 5px; padding: 2px 7px; white-space: nowrap; }
.preview-body { display: flex; gap: 24px; align-items: center; margin-top: 12px; }
.preview-copy { min-width: 0; flex: 1; }
.preview-title { margin: 0; font-size: 18px; line-height: 1.65; font-weight: 650; color: var(--ink); overflow-wrap: anywhere; }
.post-preview:hover .preview-title { color: var(--teal); }
.preview-label { display: inline-block; vertical-align: 2px; margin-right: 7px; padding: 1px 5px; font-size: 11px; font-weight: 500; color: var(--teal); border: 1px solid #bfddd6; border-radius: 4px; line-height: 1.6; }
.preview-excerpt { margin: 7px 0 0; color: var(--muted); font-size: 14px; line-height: 1.8; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; overflow-wrap: anywhere; }
.preview-stats { display: flex; gap: 22px; margin-top: 14px; color: var(--muted); font-size: 12px; }
.preview-stats span { display: inline-flex; align-items: center; gap: 5px; }
.preview-image { width: 128px; height: 88px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
@media (max-width: 600px) {
  .post-preview { padding: 20px 17px; }
  .preview-title { font-size: 16px; }
  .preview-author { max-width: 90px; }
  .preview-time { font-size: 11px; }
  .preview-image { width: 80px; height: 70px; }
  .preview-body { gap: 12px; }
  .preview-stats { gap: 15px; }
}
</style>
