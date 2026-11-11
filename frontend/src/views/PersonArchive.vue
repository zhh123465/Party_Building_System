<template>
  <div>
    <div class="head">
      <el-button link @click="$router.push(`/persons/${personId}/materials`)">← 返回材料</el-button>
      <h2 class="page-title" style="margin:0">电子档案 · {{ tree?.name || '' }}</h2>
      <el-tag v-if="tree" type="danger" effect="plain">{{ tree.currentStage }}</el-tag>
    </div>
    <p class="page-subtitle">按人员—阶段—材料类型组织电子档案；归档前自动核查缺件，缺件不允许归档。</p>

    <div class="card" v-loading="loading">
      <el-collapse v-model="activeStages">
        <el-collapse-item v-for="stage in tree?.stages || []" :key="stage.stage" :name="stage.stage">
          <template #title>
            <div class="stage-title">
              <strong>{{ stage.stage }}</strong>
              <el-tag size="small" :type="stage.complete ? 'success' : 'warning'">
                {{ stage.complete ? '可归档' : '存在缺件' }}
              </el-tag>
              <span class="small">{{ stage.passedCount }} / {{ stage.requiredCount }} 项已通过</span>
              <el-button
                v-if="stage.complete && ['R1','R2'].includes(auth.role)"
                size="small"
                type="danger"
                @click.stop="doArchive(stage.stage)"
              >归档本阶段</el-button>
            </div>
          </template>
          <el-table :data="stage.materials" size="small" border>
            <el-table-column prop="materialTypeName" label="材料类型" min-width="200" />
            <el-table-column prop="required" label="必填" width="70" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="versionCount" label="版本数" width="80" />
            <el-table-column prop="latestFileName" label="最新文件" min-width="180">
              <template #default="{ row }">{{ row.latestFileName || '—' }}</template>
            </el-table-column>
            <el-table-column label="归档" width="80">
              <template #default="{ row }"><el-tag v-if="row.archived" type="success" size="small">已归档</el-tag><span v-else>—</span></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="card" style="margin-top:14px">
      <h3>归档记录</h3>
      <el-table :data="tree?.archives || []" size="small" stripe>
        <el-table-column prop="archiveNo" label="归档编号" min-width="200" />
        <el-table-column prop="stage" label="阶段" width="80" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="archivedByName" label="归档人" width="100" />
        <el-table-column prop="archivedAt" label="归档时间" width="180" />
        <el-table-column label="包大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="download(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!tree?.archives?.length" description="尚未归档" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { archiveStage, archiveTree } from '../api/archive'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const personId = Number(route.params.id)
const tree = ref(null)
const loading = ref(false)
const activeStages = ref(['S0'])

const statusMap = { M1: '待上传', M2: '待核验', M3: '待人工复核', M4: '已通过', M5: '已退回', M6: '已归档', M7: '核验异常' }
const statusTypes = { M1: 'info', M2: 'warning', M3: 'warning', M4: 'success', M5: 'danger', M6: 'success', M7: 'danger' }
const statusLabel = (v) => statusMap[v] || v
const statusType = (v) => statusTypes[v] || 'info'
const formatSize = (size) => {
  if (!size) return '—'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

async function load() {
  loading.value = true
  try {
    tree.value = await archiveTree(personId)
    if (tree.value.stages?.length) activeStages.value = [tree.value.stages[tree.value.stages.length - 1].stage]
  } finally {
    loading.value = false
  }
}

async function doArchive(stage) {
  await ElMessageBox.confirm(`确认归档 ${stage} 阶段？归档后该阶段材料将转为已归档。`, '归档确认', { type: 'warning' })
  await archiveStage(personId, stage)
  ElMessage.success('归档完成')
  await load()
}

async function download(row) {
  const token = sessionStorage.getItem('pb_token')
  const resp = await fetch(`/api/v1/archives/${row.archiveId}/download`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!resp.ok) {
    ElMessage.error('下载失败')
    return
  }
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${row.archiveNo}.zip`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 14px; margin-bottom: 4px; }
.stage-title { display: flex; align-items: center; gap: 12px; }
.small { color: #667085; font-size: 12px; }
h3 { margin: 0 0 12px; font-size: 16px; }
</style>