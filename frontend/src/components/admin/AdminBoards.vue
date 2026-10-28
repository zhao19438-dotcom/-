<script setup>
import { CollectionTag, Plus, Delete, Edit } from '@element-plus/icons-vue'
const props = defineProps({ workspace: { type: Object, required: true } })
const { boards, loading, boardPage, loadBoards, openBoard, setBoardState, delBoard } = props.workspace
</script>

<template>
<section class="panel-section">
          <div class="panel-title-row">
            <h2 class="section-title"><el-icon><CollectionTag /></el-icon> 版块管理</h2>
            <el-button type="primary" :icon="Plus" @click="openBoard(null)">新增版块</el-button>
          </div>

          <div class="card table-card">
            <el-table :data="boards" v-loading="loading" stripe class="modern-table">
              <el-table-column label="版块名称" min-width="180">
                <template #default="{ row }">
                  <div class="board-cell">
                    <strong class="b-text">{{ row.name }}</strong>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="articleCount" label="帖子总数" width="120">
                <template #default="{ row }">
                  <span class="count-pill">{{ row.articleCount || 0 }} 篇</span>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="展示权重 (排序)" width="140">
                <template #default="{ row }">
                  <el-tag size="small" type="info">权重 {{ row.sort }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="开放状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.state === 1 ? 'danger' : 'success'" size="small">
                    {{ row.state === 1 ? '已禁用' : '正常开放' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="管理操作" width="220" fixed="right">
                <template #default="{ row }">
                  <div class="table-ops">
                    <el-button size="small" :icon="Edit" @click="openBoard(row)">编辑</el-button>
                    <el-button
                      size="small"
                      :type="row.state === 1 ? 'success' : 'warning'"
                      plain
                      @click="setBoardState(row)"
                    >
                      {{ row.state === 1 ? '启用' : '禁用' }}
                    </el-button>
                    <el-button size="small" text type="danger" :icon="Delete" @click="delBoard(row)">
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                layout="total, prev, pager, next"
                :total="boardPage.total"
                :page-size="boardPage.size"
                :current-page="boardPage.page"
                @current-change="(p) => { boardPage.page = p; loadBoards() }"
                background
              />
            </div>
          </div>
        </section>
</template>
