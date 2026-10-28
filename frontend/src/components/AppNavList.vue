<script setup>
defineProps({
  items: { type: Array, required: true },
  ariaLabel: { type: String, default: '' }
})

const emit = defineEmits(['navigate'])
</script>

<template>
  <nav class="main-nav" :aria-label="ariaLabel || undefined">
    <button
      v-for="item in items"
      :key="item.label"
      type="button"
      class="nav-item"
      :class="{ active: item.active() }"
      @click="emit('navigate', item.to)"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
      <span v-if="item.badge > 0" class="nav-badge">{{ item.badge > 99 ? '99+' : item.badge }}</span>
    </button>
  </nav>
</template>
