<script setup>
const props = defineProps({ workspace: { type: Object, required: true } })
const { boardDialog, saveBoard, handleDialog, submitHandle } = props.workspace
</script>

<template>
  <el-dialog v-model="boardDialog.visible" :title="boardDialog.id ? '编辑版块' : '新增版块'" width="440px" :close-on-click-modal="!boardDialog.saving" :close-on-press-escape="!boardDialog.saving" :show-close="!boardDialog.saving">
    <el-form label-position="top" @submit.prevent="saveBoard">
      <el-form-item label="版块名称" required><el-input v-model="boardDialog.name" maxlength="50" placeholder="请输入版块名称" /></el-form-item>
      <el-form-item label="业务类型"><el-select v-model="boardDialog.bizType" style="width: 100%"><el-option label="普通讨论" value="forum" /><el-option label="失物招领" value="lostfound" /><el-option label="公告" value="announcement" /></el-select></el-form-item>
      <el-form-item label="排序（数字越小越靠前）"><el-input-number v-model="boardDialog.sort" :min="0" :max="999" style="width: 100%" /></el-form-item>
      <el-form-item label="版块状态"><el-select v-model="boardDialog.state" style="width: 100%"><el-option label="开放" :value="0" /><el-option label="停用" :value="1" /></el-select></el-form-item>
    </el-form>
    <template #footer><el-button :disabled="boardDialog.saving" @click="boardDialog.visible = false">取消</el-button><el-button type="primary" :loading="boardDialog.saving" @click="saveBoard">保存</el-button></template>
  </el-dialog>
  <el-dialog v-model="handleDialog.visible" :title="handleDialog.status === 1 ? '处理举报' : '驳回举报'" width="480px" :close-on-click-modal="!handleDialog.loading" :close-on-press-escape="!handleDialog.loading" :show-close="!handleDialog.loading">
    <el-form label-position="top">
      <el-form-item v-if="handleDialog.status === 1" label="处置措施">
        <el-radio-group v-model="handleDialog.action" class="sanction-options">
          <el-radio value="none">仅标记为已处理</el-radio>
          <el-radio value="warn">警告违规用户</el-radio>
          <el-radio value="delete">删除违规内容并警告</el-radio>
          <el-radio value="ban">禁言违规账号</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="处理说明" required><el-input v-model="handleDialog.handleResult" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写处理结果，将通知举报人" /></el-form-item>
    </el-form>
    <template #footer><el-button :disabled="handleDialog.loading" @click="handleDialog.visible = false">取消</el-button><el-button type="primary" :loading="handleDialog.loading" @click="submitHandle">确认{{ handleDialog.status === 1 ? '处理' : '驳回' }}</el-button></template>
  </el-dialog>
</template>

<style scoped>
.sanction-options { display: grid; gap: 8px; }
</style>
