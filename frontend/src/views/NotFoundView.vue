<script setup>
import { useRouter } from 'vue-router'
import { Compass } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<template>
  <div class="page notfound-page">
    <div class="nf-card anim-fade-up">
      <!-- 插画感装饰圆环，纯展示 -->
      <span class="nf-ring ring-a"></span>
      <span class="nf-ring ring-b"></span>
      <span class="nf-ring ring-c"></span>

      <div class="nf-icon-chip">
        <el-icon><Compass /></el-icon>
      </div>

      <div class="nf-code">404</div>
      <h1 class="nf-title">哎呀，页面走丢了</h1>
      <p class="nf-desc">
        你要找的页面可能已被移动、删除，或者需要先登录才能查看。
        别担心，回到首页继续逛逛校园讨论吧。
      </p>

      <div class="nf-actions">
        <button class="primary-button" @click="router.push('/')">回到首页</button>
        <button class="outline-button" @click="router.push('/login')">去登录</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notfound-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 140px);
  padding: 24px 16px 48px;
}

.nf-card {
  position: relative;
  width: 100%;
  max-width: 560px;
  padding: 64px 40px 56px;
  overflow: hidden;
  text-align: center;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
}

/* 插画感装饰圆环（浮动动画） */
.nf-ring {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  animation: float-slow 5s var(--ease) infinite;
}

.ring-a {
  width: 190px;
  height: 190px;
  top: -80px;
  right: -70px;
  border: 1.5px solid rgba(17, 124, 120, 0.16);
}

.ring-b {
  width: 110px;
  height: 110px;
  bottom: -46px;
  left: -40px;
  border: 1.5px dashed rgba(17, 124, 120, 0.2);
  animation-delay: 0.9s;
}

.ring-c {
  width: 46px;
  height: 46px;
  top: 42px;
  left: 12%;
  background: var(--teal-pale);
  animation-delay: 1.7s;
}

.nf-icon-chip {
  display: inline-flex;
  width: 46px;
  height: 46px;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  color: var(--teal);
  background: var(--teal-pale);
  border: 1px solid #d3e7e4;
  border-radius: 14px;
  font-size: 22px;
}

/* 大号渐变 404 数字 */
.nf-code {
  font-size: 110px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.04em;
  background: var(--grad-brand);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

.nf-title {
  margin: 6px 0 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: -0.01em;
}

.nf-desc {
  max-width: 380px;
  margin: 10px auto 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-soft);
}

.nf-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 30px;
}

@media (max-width: 640px) {
  .nf-card {
    padding: 48px 22px 40px;
  }

  .nf-code {
    font-size: 76px;
  }

  .nf-actions {
    flex-direction: column;
  }
}
</style>
