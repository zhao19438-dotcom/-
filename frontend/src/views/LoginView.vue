<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Right } from '@element-plus/icons-vue'
import { userApi } from '../api/user'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名或学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit () {
  if (loading.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userApi.login(form)
      store.clear()
      const user = await store.load(true)
      if (user) {
        ElMessage.success('登录成功，欢迎回到论坛')
        const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
        router.replace(redirect)
      } else {
        ElMessage.error('获取登录信息失败，请重试')
      }
    } catch (e) {
      // 失败提示由全局拦截器处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="auth-wrapper">
    <div class="auth-card anim-fade-up">
      <div class="auth-card-header">
        <router-link to="/" class="auth-brand-logo" title="返回首页">
          <div class="brand-crest">
            <span></span><span></span><span></span>
          </div>
          <span class="auth-brand-name">校园论坛</span>
        </router-link>
        <h2 class="auth-title">账号登录</h2>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="账号 / 学号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号或学号"
            autocomplete="username"
            :prefix-icon="User"
            size="large"
            clearable
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入登录密码"
            autocomplete="current-password"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <div class="form-extra-row">
          <router-link to="/forgot" class="forgot-link">忘记密码？</router-link>
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-auth-btn"
          :loading="loading"
          @click="onSubmit"
        >
          登录
        </el-button>

        <div class="form-footer-nav">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">
            <span>注册新账号</span>
            <el-icon><Right /></el-icon>
          </router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
</style>
