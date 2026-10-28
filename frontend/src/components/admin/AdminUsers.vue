<script setup>
import { User, Search, Refresh, Delete } from '@element-plus/icons-vue'
import UserAvatar from '../UserAvatar.vue'
import { useUserStore } from '../../stores/user'
const props = defineProps({ workspace: { type: Object, required: true } })
const { users, loading, userPage, loadUsers, setUserState, setUserAdmin, delUser, resetPwd } = props.workspace
const store = useUserStore()
const isSelf = row => String(row.id) === String(store.userInfo?.id)
function userCommand (command, row) {
  if (command === 'password') resetPwd(row)
  else if (command === 'role' && !isSelf(row)) setUserAdmin(row)
  else if (command === 'delete' && !isSelf(row)) delUser(row)
}
</script>

<template>
<section class="panel-section">
          <div class="panel-title-row">
            <h2 class="section-title"><el-icon><User /></el-icon> 用户管理</h2>
            <span class="panel-desc">管理用户状态、权限及重置密码</span>
          </div>

          <div class="card toolbar-card">
            <div class="filter-row">
              <el-input
                v-model="userPage.keyword"
                placeholder="搜索用户名、昵称、学号、真实姓名…"
                :prefix-icon="Search"
                clearable
                style="max-width: 340px;"
                @keyup.enter="userPage.page = 1; loadUsers()"
                @clear="userPage.page = 1; loadUsers()"
              />
              <el-button type="primary" :icon="Search" @click="userPage.page = 1; loadUsers()">搜索</el-button>
              <el-button :icon="Refresh" @click="userPage.keyword = ''; userPage.page = 1; loadUsers()">重置</el-button>
            </div>
          </div>

          <div class="card table-card">
            <el-table :data="users" v-loading="loading" stripe class="modern-table">
              <el-table-column label="学生账号 / 昵称" min-width="180">
                <template #default="{ row }">
                  <div class="user-cell">
                    <UserAvatar :user="row" />
                    <div class="u-names">
                      <strong class="u-nick">{{ row.nickname || row.username }}</strong>
                      <span class="u-uname">@{{ row.username }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="studentNo" label="学号" width="130">
                <template #default="{ row }">
                  <span class="mono-code">{{ row.studentNo || '未填' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="realName" label="姓名" width="100">
                <template #default="{ row }">{{ row.realName || '未填' }}</template>
              </el-table-column>
              <el-table-column label="学院 / 班级" min-width="160">
                <template #default="{ row }">
                  <span class="cell-sub">{{ row.college || '—' }} · {{ row.className || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="权限角色" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.isAdmin === 1" type="danger" size="small">
                    管理员
                  </el-tag>
                  <el-tag v-else type="info" size="small">
                    学生
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="账号状态" width="110">
                <template #default="{ row }">
                  <span v-if="row.state === 1" class="status-pill banned">
                    <span class="status-dot"></span> 已封禁
                  </span>
                  <span v-else class="status-pill active">
                    <span class="status-dot"></span> 正常
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="管理操作" width="150" fixed="right">
                <template #default="{ row }">
                  <div class="table-ops">
                    <el-button
                      size="small"
                      :type="row.state === 1 ? 'success' : 'warning'"
                      link
                      :disabled="isSelf(row)"
                      @click="setUserState(row)"
                    >
                      {{ row.state === 1 ? '解封' : '封禁' }}
                    </el-button>
                    <el-dropdown trigger="click" @command="command => userCommand(command, row)">
                      <el-button size="small" link type="primary">更多操作</el-button>
                      <template #dropdown><el-dropdown-menu>
                        <el-dropdown-item command="password">重置密码</el-dropdown-item>
                        <el-dropdown-item command="role" :disabled="isSelf(row)">{{ row.isAdmin === 1 ? '取消管理员权限' : '设为管理员' }}</el-dropdown-item>
                        <el-dropdown-item command="delete" :disabled="isSelf(row)" divided :icon="Delete">注销账号</el-dropdown-item>
                      </el-dropdown-menu></template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                layout="total, prev, pager, next"
                :total="userPage.total"
                :page-size="userPage.size"
                :current-page="userPage.page"
                @current-change="(p) => { userPage.page = p; loadUsers() }"
                background
              />
            </div>
          </div>
        </section>
</template>
