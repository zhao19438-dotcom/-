<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, School, Postcard, Stamp, Right } from '@element-plus/icons-vue'
import { userApi } from '../api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  studentNo: '',
  realName: '',
  college: '',
  className: '',
  captcha: ''
})

const captchaUrl = ref('')

function refreshCaptcha () {
  captchaUrl.value = '/user/captcha?t=' + Date.now()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirm: [{
    validator: (rule, value, cb) =>
      value === form.password ? cb() : cb(new Error('两次输入的密码不一致')),
    trigger: 'blur'
  }],
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{6,20}$/, message: '学号为 6-20 位数字', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  college: [{ required: true, message: '请输入所属学院', trigger: 'blur' }],
  className: [{ required: true, message: '请输入所属班级', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function onSubmit () {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userApi.register({
        username: form.username,
        studentNo: form.studentNo,
        realName: form.realName,
        college: form.college,
        className: form.className,
        nickname: form.username,
        password: form.password,
        passwordRepeat: form.confirm,
        captcha: form.captcha
      })
      ElMessage.success('账号注册成功，请登录')
      router.replace('/login')
    } catch (e) {
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="auth-wrapper">
    <div class="auth-card register-card anim-fade-up">
      <div class="auth-card-header">
        <router-link to="/" class="auth-brand-logo" title="返回首页">
          <div class="brand-crest">
            <span></span><span></span><span></span>
          </div>
          <span class="auth-brand-name">校园论坛</span>
        </router-link>
        <h2 class="auth-title">注册新账号</h2>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
      >
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户名 / 登录账号" prop="username">
              <el-input
                v-model="form.username"
                placeholder="3-20位字母/数字"
                :prefix-icon="User"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="在校学号" prop="studentNo">
              <el-input
                v-model="form.studentNo"
                placeholder="例如：20240101"
                :prefix-icon="Postcard"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input
                v-model="form.realName"
                placeholder="请输入姓名"
                :prefix-icon="Stamp"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="所属学院" prop="college">
              <el-input
                v-model="form.college"
                placeholder="例如：计算机学院"
                :prefix-icon="School"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="所在班级" prop="className">
              <el-input
                v-model="form.className"
                placeholder="例如：计科2401班"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="验证码" prop="captcha">
              <div class="captcha-row">
                <el-input
                  v-model="form.captcha"
                  placeholder="计算结果"
                  :prefix-icon="Key"
                  @keyup.enter="onSubmit"
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
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="设置密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="至少 6 位密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="确认密码" prop="confirm">
              <el-input
                v-model="form.confirm"
                type="password"
                placeholder="再次输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="onSubmit"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-button
          type="primary"
          class="submit-auth-btn"
          size="large"
          :loading="loading"
          @click="onSubmit"
        >
          立即注册
        </el-button>
      </el-form>

      <div class="form-footer-nav">
        <span>已有账号？</span>
        <router-link to="/login" class="register-link">
          <span>立即登录</span>
          <el-icon><Right /></el-icon>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-img-container {
  flex-shrink: 0;
  width: 110px;
  height: 38px;
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
</style>
