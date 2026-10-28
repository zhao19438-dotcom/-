<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, QuestionFilled, ArrowLeft } from '@element-plus/icons-vue'
import { userApi } from '../api/user'

const router = useRouter()

// step: 1 = 输入用户名验证密保存在；2 = 回答密保并重置登录密码
const step = ref(1)
const loading = ref(false)
const captchaUrl = ref('/user/captcha?t=' + Date.now())

const form = reactive({
  username: '',
  question: '',
  securityAnswer: '',
  newPassword: '',
  passwordRepeat: '',
  captcha: ''
})

function refreshCaptcha () {
  captchaUrl.value = '/user/captcha?t=' + Date.now()
}

async function nextStep () {
  if (!form.username.trim()) return ElMessage.warning('请输入学号或用户名')
  loading.value = true
  try {
    const data = await userApi.securityQuestion(form.username.trim())
    form.question = data.securityQuestion || ''
    step.value = 2
    refreshCaptcha()
  } catch (e) {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}

async function submitReset () {
  if (!form.securityAnswer.trim()) return ElMessage.warning('请输入密保答案')
  if (form.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (form.newPassword !== form.passwordRepeat) return ElMessage.warning('两次输入的密码不一致')
  if (!form.captcha.trim()) return ElMessage.warning('请输入验证码')
  loading.value = true
  try {
    await userApi.resetPwd({
      username: form.username.trim(),
      securityAnswer: form.securityAnswer.trim(),
      newPassword: form.newPassword,
      passwordRepeat: form.passwordRepeat,
      captcha: form.captcha.trim()
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.replace('/login')
  } catch (e) {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
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
        <h2 class="auth-title">找回密码</h2>
      </div>

      <el-steps :active="step" finish-status="success" align-center class="auth-steps">
        <el-step title="验证账号" />
        <el-step title="重置密码" />
      </el-steps>

      <!-- 第 1 步：输入账号查找密保 -->
      <template v-if="step === 1">
        <el-form label-position="top" class="auth-form">
          <el-form-item label="学号 / 用户名">
            <el-input
              v-model="form.username"
              placeholder="请输入学号或用户名"
              :prefix-icon="User"
              size="large"
              clearable
              @keyup.enter="nextStep"
            />
          </el-form-item>

          <el-button
            type="primary"
            class="submit-auth-btn"
            size="large"
            :loading="loading"
            @click="nextStep"
          >
            下一步
          </el-button>
        </el-form>
      </template>

      <!-- 第 2 步：回答密保并重置密码 -->
      <template v-else>
        <el-form label-position="top" class="auth-form">
          <el-form-item label="密保问题">
            <el-input
              v-model="form.question"
              disabled
              :prefix-icon="QuestionFilled"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密保答案">
            <el-input
              v-model="form.securityAnswer"
              placeholder="请输入密保问题答案"
              :prefix-icon="Key"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item label="新密码">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="至少 6 位新密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认新密码">
            <el-input
              v-model="form.passwordRepeat"
              type="password"
              placeholder="再次输入新密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="计算结果"
                :prefix-icon="Key"
                size="large"
                @keyup.enter="submitReset"
              />
              <div
                class="captcha-img-container"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              >
                <img :src="captchaUrl" alt="验证码" class="captcha-img" />
              </div>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            class="submit-auth-btn"
            size="large"
            :loading="loading"
            @click="submitReset"
          >
            确认重置密码
          </el-button>
        </el-form>
      </template>

      <div class="form-footer-nav">
        <router-link to="/login" class="back-login-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回登录</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-steps {
  margin-bottom: 24px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-img-container {
  flex-shrink: 0;
  width: 110px;
  height: 40px;
  background: var(--paper-deep);
  border: 1px solid var(--line);
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}
.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.back-login-link {
  color: var(--muted);
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.15s ease;
}
.back-login-link:hover {
  color: var(--teal);
}
</style>
