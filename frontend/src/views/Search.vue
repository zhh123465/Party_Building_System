<template>
  <div>
    <h2 class="page-title">档案检索</h2>
    <p class="page-subtitle">对已抽取材料文本做关键词检索，返回命中片段并定位到人员与材料。</p>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="输入关键词，如：入党动机、家庭成员、谈话" clearable style="width:380px" @keyup.enter="search" />
        <el-button type="danger" :loading="loading" @click="search">检索</el-button>
        <span class="small">POC 使用文本 LIKE 检索；生产可切换 MySQL FULLTEXT/ngram。</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="personName" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="stage" label="阶段" width="70" />
        <el-table-column prop="materialTypeName" label="材料" min-width="160" />
        <el-table-column prop="fileName" label="文件" min-width="160" />
        <el-table-column prop="snippet" label="命中片段" min-width="340" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="$router.push(`/persons/${row.personId}/materials`)">查看材料</el-button>
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
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchMaterials } from '../api/archive'

const keyword = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

async function load() {
  if (!keyword.value.trim()) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await searchMaterials({ keyword: keyword.value.trim(), page: page.value, size: size.value })
    rows.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load().catch(() => ElMessage.error('检索失败'))
}
</script>

<style scoped>
.small { color: #667085; font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>