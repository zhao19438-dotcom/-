<script setup>
import { Plus, Refresh, Lock, Delete } from '@element-plus/icons-vue'
const props = defineProps({ workspace: { type: Object, required: true } })
const { loading, sensitiveWords, sensitiveInput, addSensitive, delSensitive, refreshSensitive } = props.workspace
</script>

<template>
<section class="panel-section">
          <div class="panel-title-row">
            <h2 class="section-title"><el-icon><Lock /></el-icon> 敏感词管理</h2>
            <el-button :icon="Refresh" @click="refreshSensitive">更新敏感词库</el-button>
          </div>

          <div class="card toolbar-card">
            <div class="filter-row">
              <el-input
                v-model="sensitiveInput"
                placeholder="输入需拦截的违规/敏感词后回车或点击添加…"
                :prefix-icon="Lock"
                clearable
                style="max-width: 360px;"
                @keyup.enter="addSensitive"
              />
              <el-button type="primary" :icon="Plus" @click="addSensitive">添加敏感词</el-button>
            </div>
          </div>

          <!-- 敏感词云展示卡 -->
          <div class="card" style="margin-bottom: 16px;">
            <h3 class="dash-subtitle">当前生效敏感词 (点击移除)</h3>
            <div v-if="!sensitiveWords.length" class="muted" style="padding: 20px; text-align: center;">
              暂无敏感词配置
            </div>
            <div v-else class="tag-cloud">
              <el-tag
                v-for="w in sensitiveWords"
                :key="w.id"
                closable
                type="danger"
                effect="plain"
                class="cloud-tag"
                @close="delSensitive(w)"
              >
                {{ w.word }}
              </el-tag>
            </div>
          </div>

          <!-- 敏感词数据明细表 -->
          <div class="card table-card">
            <el-table :data="sensitiveWords" v-loading="loading" stripe class="modern-table">
              <el-table-column prop="id" label="词条 ID" width="100" />
              <el-table-column prop="word" label="敏感词词条" min-width="200" />
              <el-table-column prop="createTime" label="录入系统时间" width="200" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="danger" text :icon="Delete" @click="delSensitive(row)">
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
</template>
