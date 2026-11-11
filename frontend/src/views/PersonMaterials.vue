<template>
  <div>
    <div class="head">
      <el-button link @click="$router.push('/persons')">← 返回台账</el-button>
      <h2 class="page-title" style="margin:0">材料核验 · {{ person?.name || '' }}</h2>
      <el-tag v-if="person" type="danger" effect="plain">{{ stageLabel(person.currentStage) }}</el-tag>
      <el-button link type="danger" @click="$router.push(`/persons/${personId}/archive`)">电子档案</el-button>
    </div>
    <p class="page-subtitle">上传材料后系统自动进入“规则 + 语义”异步核验；核验完成前材料保持待核验状态。</p>

    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="演示环境：请使用虚构样例材料，禁止上传真实政审材料。"
      style="margin-bottom:14px"
    />

    <div class="card">
      <el-table :data="materials" v-loading="loading" stripe>
        <el-table-column prop="materialTypeName" label="材料类型" min-width="200" />
        <el-table-column prop="stage" label="阶段" width="80" />
        <el-table-column prop="required" label="必填" width="70">
          <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="材料状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latestFileName" label="最新文件" min-width="180">
          <template #default="{ row }">{{ row.latestFileName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="taskStatus" label="核验任务" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.taskStatus" size="small" :type="taskType(row.taskStatus)">{{ taskLabel(row.taskStatus) }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="overallConclusion" label="核验结论" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.overallConclusion" :type="conclusionType(row.overallConclusion)">{{ row.overallConclusion }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              :limit="1"
              accept=".pdf,.jpg,.jpeg,.png,.docx"
              :on-change="(f) => doUpload(row, f.raw)"
            >
              <el-button link type="danger" :loading="uploading[row.materialId]">上传</el-button>
            </el-upload>
            <el-button v-if="row.currentVersionId" link type="warning" :loading="verifying[row.materialId]" @click="doVerify(row)">重新核验</el-button>
            <el-button v-if="row.reportId" link type="primary" @click="openReport(row)">查看报告</el-button>
            <el-button v-if="row.status === 'M3' && auth.role !== 'R4'" link type="danger"
                       @click="$router.push(`/reviews?materialId=${row.materialId}`)">去复核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="reportVisible" title="核验报告" size="70%">
      <template v-if="report">
        <div class="report-head">
          <el-tag size="large" :type="conclusionType(report.overallConclusion)">{{ report.overallConclusion }}</el-tag>
          <span class="meta">规则层：{{ report.ruleConclusion }} · 语义层：{{ report.semanticConclusion }} · {{ report.durationMs }}ms</span>
        </div>
        <h4>规则层</h4>
        <el-table :data="ruleFindings" size="small" border>
          <el-table-column prop="ruleCode" label="规则编号" width="130" />
          <el-table-column prop="pointName" label="要点" width="180" />
          <el-table-column prop="conclusion" label="结论" width="100">
            <template #default="{ row }"><el-tag :type="row.conclusion === 'PASS' ? 'success' : 'danger'">{{ row.conclusion }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="evidenceText" label="依据" />
          <el-table-column prop="suggestion" label="建议" width="180" />
        </el-table>
        <h4>语义层</h4>
        <el-table :data="semanticFindings" size="small" border>
          <el-table-column prop="pointName" label="要点" width="180" />
          <el-table-column prop="conclusion" label="结论" width="100">
            <template #default="{ row }"><el-tag :type="row.conclusion === 'PASS' ? 'success' : 'warning'">{{ row.conclusion }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="evidenceText" label="原文依据" />
          <el-table-column prop="confidence" label="置信度" width="80" />
        </el-table>
        <p class="meta" style="margin-top:14px">AI 结论仅为辅助建议，最终以人工复核为准。</p>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPerson } from '../api/person'
import { getReport, getTask, listMaterials, uploadVersion, verifyMaterial } from '../api/material'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const route = useRoute()
const personId = Number(route.params.id)
const person = ref(null)
const materials = ref([])
const loading = ref(false)
const uploading = reactive({})
const verifying = reactive({})
const reportVisible = ref(false)
const report = ref(null)

const ruleFindings = computed(() => report.value?.findings?.filter((f) => f.layer === 'RULE') || [])
const semanticFindings = computed(() => report.value?.findings?.filter((f) => f.layer === 'SEMANTIC') || [])

const stages = {
  S0: 'S0 入党申请人', S1: 'S1 入党积极分子', S2: 'S2 发展对象', S3: 'S3 预备党员', S4: 'S4 正式党员'
}
const statusMap = { M1: '待上传', M2: '待核验', M3: '待人工复核', M4: '已通过', M5: '已退回', M6: '已归档', M7: '核验异常' }
const statusTypes = { M1: 'info', M2: 'warning', M3: 'warning', M4: 'success', M5: 'danger', M6: 'success', M7: 'danger' }
const taskMap = { PENDING: '排队中', RUNNING: '核验中', SUCCESS: '成功', FAILED: '失败', TIMEOUT: '超时' }
const taskTypes = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', TIMEOUT: 'danger' }

const stageLabel = (v) => stages[v] || v
const statusLabel = (v) => statusMap[v] || v
const statusType = (v) => statusTypes[v] || 'info'
const taskLabel = (v) => taskMap[v] || v
const taskType = (v) => taskTypes[v] || 'info'
const conclusionType = (v) => ({ PASS: 'success', DOUBT: 'warning', NEED_FIX: 'danger', ERROR: 'info' }[v] || 'info')

async function load() {
  loading.value = true
  try {
    const [p, list] = await Promise.all([getPerson(personId), listMaterials(personId)])
    person.value = p
    materials.value = list
  } finally {
    loading.value = false
  }
}

async function doUpload(row, file) {
  uploading[row.materialId] = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const result = await uploadVersion(row.materialId, fd)
    ElMessage.success(`已上传 v${result.versionNo}，核验任务已排队`)
    await load()
    poll(result.taskId)
  } finally {
    uploading[row.materialId] = false
  }
}

async function poll(taskId) {
  for (let i = 0; i < 60; i++) {
    await new Promise((resolve) => setTimeout(resolve, 1500))
    try {
      const task = await getTask(taskId)
      if (['SUCCESS', 'FAILED', 'TIMEOUT'].includes(task.status)) {
        if (task.status === 'SUCCESS') {
          ElMessage.success('核验完成，请查看报告')
        } else {
          ElMessage.error(`核验失败：${task.errorMsg || task.status}`)
        }
        await load()
        return
      }
    } catch (e) {
      return
    }
  }
  ElMessage.warning('核验耗时较长，请稍后刷新查看')
}

async function doVerify(row) {
  verifying[row.materialId] = true
  try {
    const task = await verifyMaterial(row.materialId)
    ElMessage.info('核验任务已提交')
    poll(task.taskId)
  } finally {
    verifying[row.materialId] = false
  }
}

async function openReport(row) {
  report.value = await getReport(row.reportId)
  reportVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 4px;
}
.report-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}
.meta {
  color: #667085;
  font-size: 13px;
}
h4 {
  margin: 18px 0 8px;
}
</style>