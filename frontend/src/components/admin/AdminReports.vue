<script setup>
import { Warning, Refresh, Check, Close } from '@element-plus/icons-vue'
const props = defineProps({ workspace: { type: Object, required: true } })
const { router, loading, reports, reportPage, reportTypeLabels, reportStatusLabels, loadReports, openHandleDialog } = props.workspace
</script>

<template>
<section class="panel-section">
          <div class="panel-title-row">
            <h2 class="section-title"><el-icon><Warning /></el-icon> 举报处理</h2>
            <span class="panel-desc">及时核查涉嫌广告、辱骂或违规内容</span>
          </div>

          <div class="card toolbar-card">
            <div class="filter-row between">
              <div class="filter-group">
                <el-radio-group
                  v-model="reportPage.status"
                  size="small"
                  @change="() => { reportPage.page = 1; loadReports() }"
                >
                  <el-radio-button :value="0">待处理 ({{ reportPage.status === 0 ? reportPage.total : '' }})</el-radio-button>
                  <el-radio-button :value="1">已处理</el-radio-button>
                  <el-radio-button :value="2">已驳回</el-radio-button>
                </el-radio-group>
              </div>
              <el-button :icon="Refresh" @click="loadReports">刷新列表</el-button>
            </div>
          </div>

          <div class="card table-card">
            <el-table :data="reports" v-loading="loading" stripe class="modern-table">
              <el-table-column label="被举报对象" min-width="220">
                <template #default="{ row }">
                  <!-- 私信举报：展示被举报的私信内容；帖子举报：展示帖子标题并可跳转 -->
                  <template v-if="row.messageId">
                    <el-tag size="small" type="info" style="margin-right: 6px;">私信</el-tag>
                    <span class="report-msg-content">{{ row.messageContent || `私信 #${row.messageId}` }}</span>
                  </template>
                  <strong v-else class="clickable" @click="router.push(`/article/${row.articleId}`)">
                    {{ row.articleTitle || `帖子 #${row.articleId}` }}
                  </strong>
                </template>
              </el-table-column>
              <el-table-column prop="reporterName" label="举报学生" width="130" />
              <el-table-column label="违规分类" width="120">
                <template #default="{ row }">
                  <el-tag size="small" type="warning">{{ reportTypeLabels[row.type] || '其他' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="举报理由与说明" min-width="200" show-overflow-tooltip />
              <el-table-column label="审核状态" width="110">
                <template #default="{ row }">
                  <el-tag
                    :type="row.status === 0 ? 'danger' : (row.status === 1 ? 'success' : 'info')"
                    size="small"
                    effect="light"
                  >
                    {{ reportStatusLabels[row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="举报时间" width="170" />
              <el-table-column label="审查动作" width="180" fixed="right">
                <template #default="{ row }">
                  <div class="table-ops" v-if="row.status === 0">
                    <el-button size="small" type="success" :icon="Check" @click="openHandleDialog(row, 1)">
                      批准处理
                    </el-button>
                    <el-button size="small" type="info" :icon="Close" plain @click="openHandleDialog(row, 2)">
                      驳回
                    </el-button>
                  </div>
                  <span v-else class="muted">已归档</span>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                layout="total, prev, pager, next"
                :total="reportPage.total"
                :page-size="reportPage.size"
                :current-page="reportPage.page"
                @current-change="(p) => { reportPage.page = p; loadReports() }"
                background
              />
            </div>
          </div>
        </section>
</template>
