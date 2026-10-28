<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User,
  Lock,
  Camera,
  Key,
  School,
  Postcard,
  Message,
  Iphone
} from '@element-plus/icons-vue'
import { userApi } from '../api/user'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const u = store.userInfo || {}

const info = reactive({
  username: u.username || '',
  nickname: u.nickname || '',
  gender: u.gender ?? 0,
  email: u.email || '',
  phoneNum: u.phoneNum || '',
  college: u.college || '',
  className: u.className || '',
  remark: u.remark || ''
})

const pwd = reactive({ oldPassword: '', newPassword: '', passwordRepeat: '' })
const savingInfo = ref(false)
const savingPwd = ref(false)

async function saveInfo () {
  savingInfo.value = true
  try {
    const updated = await userApi.modifyInfo({ ...info, gender: Number(info.gender) })
    store.setUser(updated)
    ElMessage.success('个人资料已成功更新')
  } finally {
    savingInfo.value = false
  }
}
async function savePwd () {
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwd.newPassword !== pwd.passwordRepeat) return ElMessage.warning('两次输入密码不一致')
  savingPwd.value = true
  try {
    await userApi.modifyPwd(pwd)
    ElMessage.success('登录密码已成功修改，请妥善保管')
    pwd.oldPassword = pwd.newPassword = pwd.passwordRepeat = ''
  } finally {
    savingPwd.value = false
  }
}
async function uploadAvatar ({ file }) {
  try {
    const updated = await userApi.uploadAvatar(file)
    store.setUser(updated)
    ElMessage.success('头像上传并更新成功')
  } catch (e) {}
}

// 设置密保问题：登录用户为自己设置找回密码用的问题与答案。
const security = reactive({ question: '', answer: '' })
const savingSecurity = ref(false)
async function saveSecurity () {
  if (!security.question.trim()) return ElMessage.warning('请输入密保问题')
  if (!security.answer.trim()) return ElMessage.warning('请输入密保答案')
  savingSecurity.value = true
  try {
    await userApi.setSecurity({ question: security.question.trim(), answer: security.answer.trim() })
    ElMessage.success('密保问题已成功绑定/更新')
    security.question = security.answer = ''
  } finally {
    savingSecurity.value = false
  }
}
</script>

<template>
  <div class="page settings-page">
    <header class="page-heading"><h1>账号设置</h1></header>
    <!-- 基本资料与头像 -->
    <div class="card settings-card anim-fade-up" style="animation-delay: 0ms">
      <div class="card-head">
        <div>
          <h2 class="card-title">基本信息</h2>
        </div>

        <div class="avatar-upload-box">
          <el-upload
            :show-file-list="false"
            :http-request="uploadAvatar"
            accept="image/*"
            class="avatar-uploader"
          >
            <div class="avatar-wrap">
              <el-avatar :size="64" :src="store.userInfo && store.userInfo.avatarUrl || ''" class="avatar-circle">
                {{ (store.userInfo && (store.userInfo.nickname || store.userInfo.username) || '?').slice(0, 1) }}
              </el-avatar>
              <div class="avatar-mask">
                <el-icon><Camera /></el-icon>
              </div>
            </div>
          </el-upload>
          <span class="avatar-tip">点击更换头像</span>
        </div>
      </div>

      <el-form label-position="top" class="settings-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="账号 / 学号">
              <el-input v-model="info.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称">
              <el-input v-model="info.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="8">
            <el-form-item label="性别">
              <el-select v-model="info.gender" style="width:100%">
                <el-option label="保密" :value="0" />
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="学院">
              <el-input v-model="info.college" placeholder="如：软件工程学院" :prefix-icon="School" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="班级">
              <el-input v-model="info.className" placeholder="如：2201班" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="电子邮箱">
              <el-input v-model="info.email" placeholder="常用邮箱" :prefix-icon="Message" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号码">
              <el-input v-model="info.phoneNum" placeholder="联系手机号" :prefix-icon="Iphone" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="个人简介">
          <el-input
            v-model="info.remark"
            type="textarea"
            :rows="2"
            placeholder="填写个人简介…"
            maxlength="150"
            show-word-limit
          />
        </el-form-item>

        <div class="form-btn-row">
          <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存资料</el-button>
        </div>
      </el-form>
    </div>

    <!-- 密保问题设置 -->
    <div class="card settings-card anim-fade-up" style="animation-delay: 80ms">
      <h3 class="card-title">密保设置</h3>
      <el-form label-position="top" class="settings-form" style="max-width: 480px;">
        <el-form-item label="密保问题">
          <el-input v-model="security.question" placeholder="例如：我最喜欢的高中班主任是谁？" />
        </el-form-item>
        <el-form-item label="密保答案">
          <el-input v-model="security.answer" placeholder="输入密保答案" />
        </el-form-item>
        <el-button type="primary" plain :loading="savingSecurity" @click="saveSecurity">保存密保</el-button>
      </el-form>
    </div>

    <!-- 修改密码 -->
    <div class="card settings-card anim-fade-up" style="animation-delay: 160ms">
      <h3 class="card-title">修改密码</h3>
      <el-form label-position="top" class="settings-form" style="max-width: 480px;">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入当前密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password placeholder="新密码至少 6 位" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwd.passwordRepeat" type="password" show-password placeholder="再次输入新密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" :loading="savingPwd" @click="savePwd">修改密码</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 8px 16px 32px;
}

.settings-card {
  padding: 28px 32px;
  margin-bottom: 20px;
  background: var(--card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-xs);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}

.settings-card:hover {
  border-color: var(--line);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 20px;
}

/* 区块标题：左侧品牌色小竖条装饰 */
.card-title {
  position: relative;
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.01em;
}

.card-title::before {
  content: none;
  position: absolute;
  left: 0;
  top: 3px;
  bottom: 3px;
  width: 3px;
  border-radius: 3px;
  background: var(--teal);
}

.card-desc {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
}

.avatar-upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.avatar-wrap {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  border: 2px solid var(--line);
  transition: border-color var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}

.avatar-wrap:hover {
  border-color: var(--teal-light);
  transform: scale(1.04);
}

.avatar-circle {
  width: 100%;
  height: 100%;
  background: var(--teal);
  color: #fff;
  font-weight: 700;
}

/* 头像悬停遮罩：品牌色渐变 + 图标放大浮现 */
.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(140deg, rgba(10, 95, 93, 0.62), rgba(17, 124, 120, 0.5));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--dur) var(--ease);
  font-size: 18px;
}

.avatar-mask .el-icon {
  transform: scale(0.7);
  transition: transform var(--dur) var(--ease-spring);
}

.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}

.avatar-wrap:hover .avatar-mask .el-icon {
  transform: scale(1);
}

.avatar-tip {
  font-size: 11px;
  color: var(--muted);
}

.settings-form {
  margin-top: 10px;
}

.form-btn-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
@media (max-width: 600px) {
  .settings-page { padding: 0; }
  .settings-card { padding: 22px 18px; }
}
</style>
