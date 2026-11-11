<template>
  <div>
    <h2 class="page-title">材料复核</h2>
    <p class="page-subtitle">
      {{ auth.role === 'R3' ? '党务助理初审：通过后仍停留在待复核，由支部书记终审。' : '支部书记/管理员终审：通过后材料进入 M4 已通过。' }}
    </p>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="stage" placeholder="全部阶段" clearable style="width:150px" @change="load">
          <el-option v-for="s in ['S0','S1','S2','S3','S4']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="danger" @click="load">刷新</el-button>
        <div style="flex:1"></div>
        <el-tag type="warning">待复核 {{ total }} 条</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="personName" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="stage" label="阶段" width="70" />
        <el-table-column prop="materialTypeName" label="材料类型" min-width="180" />
        <el-table-column prop="versionNo" label="版本" width="70">
          <template #default="{ row }">v{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column prop="overallConclusion" label="AI结论" width="100">
          <template #default="{ row }">
            <el-tag :type="conclusionType(row.overallConclusion)">{{ row.overallConclusion }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="核验时间" width="160" />
        <el-table-column label="初审" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.initialReviewed" type="success" size="small">已初审</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="openReview(row)">复核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                       layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="材料复核" width="860px" top="4vh">
      <template v-if="current">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="姓名">{{ current.personName }}</el-descriptions-item>
          <el-descriptions-item label="材料">{{ current.materialTypeName }}</el-descriptions-item>
          <el-descriptions-item label="版本">v{{ current.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="AI结论">
            <el-tag :type="conclusionType(current.overallConclusion)">{{ current.overallConclusion }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <h4>核验报告</h4>
        <el-table :data="report?.findings || []" size="small" border max-height="240">
          <el-table-column prop="layer" label="层" width="80" />
          <el-table-column prop="ruleCode" label="规则" width="120" />
          <el-table-column prop="pointName" label="要点" width="160" />
          <el-table-column prop="conclusion" label="结论" width="90">
            <template #default="{ row }">
              <el-tag :type="row.conclusion === 'PASS' ? 'success' : 'warning'">{{ row.conclusion }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="evidenceText" label="依据" />
          <el-table-column prop="suggestion" label="建议" width="180" />
        </el-table>

        <h4>复核意见</h4>
        <el-form label-width="90px">
          <el-form-item label="结论">
            <el-radio-group v-model="form.conclusion">
              <el-radio label="PASS">通过</el-radio>
              <el-radio label="RETURN">退回整改</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.conclusion === 'RETURN'" label="退回原因">
            <el-select v-model="form.reasonCode" style="width:100%">
              <el-option label="材料确有问题" value="MATERIAL_ISSUE" />
              <el-option label="误判" value="MISJUDGE" />
              <el-option label="标准理解不同" value="STANDARD_DIFF" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="意见">
            <el-input v-model="form.opinion" type="textarea" :rows="4"
                      placeholder="退回意见至少 10 字，并具体到材料段落" />
          </el-form-item>
          <el-form-item v-if="form.conclusion === 'RETURN'" label="常用模板">
            <div class="templates">
              <el-tag v-for="t in templates" :key="t" class="tpl" @click="form.opinion = t">{{ t }}</el-tag>
            </div>
          </el-form-item>
        </el-form>

        <h4>复核流水</h4>
        <el-table :data="logs" size="small" border>
          <el-table-column prop="reviewerName" label="复核人" width="110" />
          <el-table-column prop="reviewLevel" label="级别" width="90">
            <template #default="{ row }">{{ row.reviewLevel === 'INITIAL' ? '初审' : '终审' }}</template>
          </el-table-column>
          <el-table-column prop="conclusion" label="结论" width="90" />
          <el-table-column prop="opinion" label="意见" />
          <el-table-column prop="createdAt" label="时间" width="160" />
        </el-table>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submit">提交复核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pendingReviews, reviewLogs, reviewTemplates, submitReview } from '../api/review'
import { getReport } from '../api/material'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const stage = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const current = ref(null)
const report = ref(null)
const logs = ref([])
const templates = ref([])
const submitting = ref(false)
const form = reactive({ conclusion: 'PASS', reasonCode: 'MATERIAL_ISSUE', opinion: '' })

const conclusionType = (v) => ({ PASS: 'success', DOUBT: 'warning', NEED_FIX: 'danger', ERROR: 'info' }[v] || 'info')

async function load() {
  loading.value = true
  try {
    const data = await pendingReviews({ page: page.value, size: size.value, stage: stage.value || undefined })
    rows.value = data.items
    total.value = data.total
    if (route.query.materialId) {
      const target = rows.value.find((r) => String(r.materialId) === String(route.query.materialId))
      if (target) openReview(target)
    }
  } finally {
    loading.value = false
  }
}

async function openReview(row) {
  current.value = row
  form.conclusion = 'PASS'
  form.reasonCode = 'MATERIAL_ISSUE'
  form.opinion = ''
  report.value = row.reportId ? await getReport(row.reportId) : null
  logs.value = await reviewLogs(row.materialId)
  dialogVisible.value = true
}

async function submit() {
  if (form.conclusion === 'RETURN' && form.opinion.trim().length < 10) {
    ElMessage.warning('退回意见至少 10 个字')
    return
  }
  submitting.value = true
  try {
    await submitReview(current.value.materialId, {
      conclusion: form.conclusion,
      reasonCode: form.conclusion === 'RETURN' ? form.reasonCode : undefined,
      opinion: form.opinion,
      versionId: current.value.versionId,
      reportId: current.value.reportId
    })
    ElMessage.success(form.conclusion === 'PASS' ? '复核通过' : '已退回整改')
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    templates.value = await reviewTemplates()
  } catch (e) {
    // ignore
  }
  await load()
})
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
h4 {
  margin: 18px 0 8px;
}
.templates {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tpl {
  cursor: pointer;
}
</style>