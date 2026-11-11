<template>
  <div>
    <h2 class="page-title">消息中心</h2>
    <p class="page-subtitle">材料退回、复核通过等站内通知。</p>
    <div class="card">
      <div class="toolbar">
        <el-button @click="load">刷新</el-button>
        <el-button type="danger" plain @click="readAll">全部已读</el-button>
        <div style="flex:1"></div>
        <el-tag type="warning">未读 {{ unread }}</el-tag>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="300" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.readAt" type="info" size="small">已读</el-tag>
            <el-tag v-else type="danger" size="small">未读</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="!row.readAt" link type="danger" @click="read(row)">标记已读</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                       layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMessages, markAllRead, markRead, unreadCount } from '../api/message'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const unread = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await listMessages({ page: page.value, size: size.value })
    rows.value = data.items
    total.value = data.total
    unread.value = (await unreadCount()).count
  } finally {
    loading.value = false
  }
}

async function read(row) {
  await markRead(row.messageId)
  await load()
}

async function readAll() {
  await markAllRead()
  ElMessage.success('已全部标记为已读')
  await load()
}

onMounted(load)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>