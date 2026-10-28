<script setup>
import { Tickets, Search, Refresh } from '@element-plus/icons-vue'
const props = defineProps({ workspace: { type: Object, required: true } })
const { loading, auditLogs, auditPage, formatActionLabel, formatTargetType, formatAuditDetail, loadAudit } = props.workspace
</script>

<template>
<section class="panel-section">
          <div class="panel-title-row">
            <h2 class="section-title"><el-icon><Tickets /></el-icon> 审计日志</h2>
            <span class="panel-desc">记录管理员在后台的操作轨迹与安全事件</span>
          </div>

          <div class="card toolbar-card">
            <div class="filter-row">
              <el-input
                v-model="auditPage.keyword"
                placeholder="搜索操作人用户名…"
                :prefix-icon="Search"
                clearable
                style="max-width: 340px;"
                @keyup.enter="auditPage.page = 1; loadAudit()"
                @clear="auditPage.page = 1; loadAudit()"
              />
              <el-button type="primary" :icon="Search" @click="auditPage.page = 1; loadAudit()">搜索</el-button>
              <el-button :icon="Refresh" @click="auditPage.keyword = ''; auditPage.page = 1; loadAudit()">重置</el-button>
            </div>
          </div>

          <div class="card table-card">
            <el-table :data="auditLogs" v-loading="loading" stripe class="modern-table">
              <el-table-column prop="username" label="操作管理员" width="130">
                <template #default="{ row }">
                  <el-tag size="small" type="danger" effect="plain">{{ row.username }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作内容" min-width="200">
                <template #default="{ row }">
                  <span class="action-tag">{{ formatActionLabel(row.action) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作说明" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ formatAuditDetail(row) }}
                </template>
              </el-table-column>
              <el-table-column label="记录来源" width="110">
                <template #default="{ row }">
                  {{ formatTargetType(row.targetType) }}
                </template>
              </el-table-column>
              <el-table-column label="对象编号" width="100">
                <template #default="{ row }">
                  {{ row.targetId || '—' }}
                </template>
              </el-table-column>
              <el-table-column prop="ip" label="操作来源 IP" width="140">
                <template #default="{ row }">
                  <span class="mono-code">{{ row.ip }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="操作时间" width="170" />
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                layout="total, prev, pager, next"
                :total="auditPage.total"
                :page-size="auditPage.size"
                :current-page="auditPage.page"
                @current-change="(p) => { auditPage.page = p; loadAudit() }"
                background
              />
            </div>
          </div>
        </section>
</template>
