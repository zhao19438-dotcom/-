<script setup>
import { computed, ref, watch } from 'vue'
import { displayName, initials } from '../utils/format'

const props = defineProps({
  user: { type: Object, default: null },
  size: { type: String, default: 'small' },
  fallback: { type: String, default: '?' }
})

const letter = computed(() => initials(props.user, props.fallback))
const label = computed(() => displayName(props.user, '用户头像'))
const failed = ref(false)
watch(() => props.user?.avatarUrl, () => { failed.value = false })
</script>

<template>
  <span class="avatar" :class="'avatar-' + size" :title="label">
    <img v-if="user?.avatarUrl && !failed" :src="user.avatarUrl" alt="" @error="failed = true" />
    <template v-else>{{ letter }}</template>
  </span>
</template>
