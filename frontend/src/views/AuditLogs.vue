<template>
  <div>
    <h2 class="page-title">审计日志</h2>
    <p class="page-subtitle">登录、查看、上传、复核、推进、回退、配置、导出等关键操作留痕；日志只增不改不删。</p>

    <div class="card">
      <el-tabs v-model="tab">
        <el-tab-pane label="操作日志" name="audit">
          <div class="toolbar">
            <el-input v-model="filters.username" placeholder="账号" clearable style="width:140px" />
            <el-select v-model="filters.action" placeholder="操作类型" clearable style="width:190px">
              <el-option v-for="a in actions" :key="a.value" :label="a.label" :value="a.value" />
            </el-select>
            <el-select v-model="filters.objectType" placeholder="对象类型" clearable style="width:130px">
              <el-option label="人员" value="PERSON" />
              <el-option label="材料" value="MATERIAL" />
              <el-option label="用户" value="USER" />
              <el-option label="导入" value="IMPORT" />
            </el-select>
            <el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                            start-placeholder="开始时间" end-placeholder="结束时间" style="width:340px" />
            <el-button type="danger" @click="loadAudit">查询</el-button>
          </div>

          <el-table :data="auditRows" v-loading="loading" stripe size="small">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="username" label="账号" width="110" />
            <el-table-column prop="action" label="操作" width="170">
              <template #default="{ row }"><el-tag size="small">{{ actionLabel(row.action) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="objectType" label="对象" width="90" />
            <el-table-column prop="objectId" label="对象ID" width="90" />
            <el-table-column prop="result" label="结果" width="90">
              <template #default="{ row }"><el-tag :type="row.result === 'SUCCESS' ? 'success' : row.result === 'FAIL' ? 'danger' : 'warning'">{{ row.result }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="reason" label="理由/说明" min-width="160" />
            <el-table-column prop="ip" label="IP" width="130" />
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                           layout="total, prev, pager, next" @current-change="loadAudit" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="导出记录" name="export">
          <el-table :data="exportRows" v-loading="exportLoading" stripe size="small">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="exportType" label="类型" width="100" />
            <el-table-column prop="fileName" label="文件" min-width="220" />
            <el-table-column prop="rowCount" label="行数" width="80" />
            <el-table-column prop="watermarkText" label="水印" min-width="220" />
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="exportPage" v-model:page-size="exportSize" :total="exportTotal"
                           layout="total, prev, pager, next" @current-change="loadExports" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAuditLogs, listExportLogs } from '../api/audit'

const tab = ref('audit')
const filters = reactive({ username: '', action: '', objectType: '' })
const range = ref([])
const auditRows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const exportRows = ref([])
const exportTotal = ref(0)
const exportPage = ref(1)
const exportSize = ref(20)
const exportLoading = ref(false)

const actions = [
  { label: '登录', value: 'LOGIN' },
  { label: '登出', value: 'LOGOUT' },
  { label: '查看材料', value: 'VIEW_MATERIAL' },
  { label: '查看身份证号', value: 'VIEW_ID_CARD' },
  { label: '上传材料', value: 'UPLOAD_MATERIAL' },
  { label: '复核材料', value: 'REVIEW_MATERIAL' },
  { label: '推进阶段', value: 'ADVANCE_STAGE' },
  { label: '回退阶段', value: 'ROLLBACK_STAGE' },
  { label: '归档', value: 'ARCHIVE_STAGE' },
  { label: '导入人员', value: 'IMPORT_PERSON' }
]
const actionMap = Object.fromEntries(actions.map((a) => [a.value, a.label]))
const actionLabel = (v) => actionMap[v] || v

async function loadAudit() {
  loading.value = true
  try {
    const data = await listAuditLogs({
      page: page.value,
      size: size.value,
      username: filters.username || undefined,
      action: filters.action || undefined,
      objectType: filters.objectType || undefined,
      start: range.value?.[0] || undefined,
      end: range.value?.[1] || undefined
    })
    auditRows.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadExports() {
  exportLoading.value = true
  try {
    const data = await listExportLogs({ page: exportPage.value, size: exportSize.value })
    exportRows.value = data.items
    exportTotal.value = data.total
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  await loadAudit()
  await loadExports()
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>