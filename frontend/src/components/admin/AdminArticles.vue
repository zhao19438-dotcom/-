<script setup>
import { Document, Search, Refresh, Delete, Top, Star } from '@element-plus/icons-vue'
const props = defineProps({ workspace: { type: Object, required: true } })
const { router, articles, loading, articlePage, loadArticles, toggleTop, toggleEssence, setArticleState, delArticle } = props.workspace
</script>

<template>
<section class="panel-section">
          <div class="panel-title-row">
            <h2 class="section-title"><el-icon><Document /></el-icon> 帖子管理</h2>
            <span class="panel-desc">内容合规审查、屏蔽下架、置顶与精华管理</span>
          </div>

          <div class="card toolbar-card">
            <div class="filter-row">
              <el-input
                v-model="articlePage.keyword"
                placeholder="搜索帖子标题关键字…"
                :prefix-icon="Search"
                clearable
                style="max-width: 320px;"
                @keyup.enter="articlePage.page = 1; loadArticles()"
                @clear="articlePage.page = 1; loadArticles()"
              />
              <el-button type="primary" :icon="Search" @click="articlePage.page = 1; loadArticles()">搜索</el-button>
              <el-button :icon="Refresh" @click="articlePage.keyword = ''; articlePage.page = 1; loadArticles()">重置</el-button>
            </div>
          </div>

          <div class="card table-card">
            <el-table :data="articles" v-loading="loading" stripe class="modern-table">
              <el-table-column label="帖子标题与属性" min-width="240">
                <template #default="{ row }">
                  <div class="article-title-cell">
                    <span v-if="row.topState === 1" class="tag-top">置顶</span>
                    <span v-if="row.essenceState === 1" class="tag-essence">精华</span>
                    <span v-if="row.bizType === 'lostfound'" class="tag-lostfound">失物</span>
                    <strong class="a-title clickable" @click="router.push(`/article/${row.id}`)">
                      {{ row.title }}
                    </strong>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="发帖同学" width="130">
                <template #default="{ row }">
                  <span>{{ row.user ? (row.user.nickname || row.user.username) : '匿名' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="互动数据" width="160">
                <template #default="{ row }">
                  <span class="cell-sub">赞 {{ row.likeCount }} · 评 {{ row.replyCount }} · 阅 {{ row.visitCount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="显示状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.state === 1 ? 'danger' : 'success'" size="small">
                    {{ row.state === 1 ? '已屏蔽' : '正常公开' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="治理操作" width="280" fixed="right">
                <template #default="{ row }">
                  <div class="table-ops">
                    <el-button size="small" :icon="Top" @click="toggleTop(row)">
                      {{ row.topState === 1 ? '取消置顶' : '置顶' }}
                    </el-button>
                    <el-button size="small" :icon="Star" @click="toggleEssence(row)">
                      {{ row.essenceState === 1 ? '取消精华' : '精华' }}
                    </el-button>
                    <el-button
                      size="small"
                      :type="row.state === 1 ? 'success' : 'warning'"
                      plain
                      @click="setArticleState(row)"
                    >
                      {{ row.state === 1 ? '公开' : '屏蔽' }}
                    </el-button>
                    <el-button size="small" text type="danger" :icon="Delete" @click="delArticle(row)">
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                layout="total, prev, pager, next"
                :total="articlePage.total"
                :page-size="articlePage.size"
                :current-page="articlePage.page"
                @current-change="(p) => { articlePage.page = p; loadArticles() }"
                background
              />
            </div>
          </div>
        </section>
</template>
