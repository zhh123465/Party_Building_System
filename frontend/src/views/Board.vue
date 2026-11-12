<template>
  <div>
    <h2 class="page-title">流程看板</h2>
    <p class="page-subtitle">三锁状态可视化：缺材料、超时限、未归档都会被锁定；推进前一次性列出全部阻塞项。</p>

    <div class="stat-grid five">
      <div class="stat-card"><div class="label">在办人员</div><div class="value">{{ stats.total }}</div></div>
      <div class="stat-card"><div class="label">被锁定</div><div class="value" style="color:#b42318">{{ stats.locked }}</div></div>
      <div class="stat-card"><div class="label">高风险</div><div class="value" style="color:#b42318">{{ stats.red }}</div></div>
      <div class="stat-card"><div class="label">中风险</div><div class="value" style="color:#c79a3b">{{ stats.yellow }}</div></div>
      <div class="stat-card"><div class="label">逾期节点</div><div class="value" style="color:#b42318">{{ stats.overdue }}</div></div>
    </div>

    <div class="grid">
      <div class="card">
        <div class="toolbar">
          <el-select v-if="auth.role === 'R1'" v-model="filters.branchId" placeholder="全部支部" clearable style="width:190px" @change="loadAll">
            <el-option label="软件工程系学生党支部" :value="1" />
            <el-option label="计算机科学与技术系学生党支部" :value="2" />
          </el-select>
          <el-select v-model="filters.stage" placeholder="全部阶段" clearable style="width:140px" @change="loadAll">
            <el-option v-for="s in ['S0','S1','S2','S3','S4']" :key="s" :label="s" :value="s" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="姓名 / 学号" clearable style="width:190px" @keyup.enter="loadAll" />
          <el-button type="danger" @click="loadAll">查询</el-button>
        </div>
        <div ref="chartRef" class="chart"></div>
      </div>

      <div class="card">
        <h3>阶段分布</h3>
        <div v-for="d in distribution" :key="d.stage" class="dist-row">
          <span class="dist-label">{{ stageLabel(d.stage) }}</span>
          <el-progress :percentage="percent(d.count)" :show-text="false" :stroke-width="12" color="#b42318" />
          <span class="dist-count">{{ d.count }}</span>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:14px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="currentStage" label="阶段" width="80">
          <template #default="{ row }"><el-tag type="danger" effect="plain">{{ row.currentStage }}</el-tag></template>
        </el-table-column>
        <el-table-column label="材料进度" min-width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.percent" :status="row.percent === 100 ? 'success' : undefined" />
            <span class="small">{{ row.passedCount }} / {{ row.requiredCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template #default="{ row }">
            <el-tag :type="riskType(row.riskLevel)">{{ riskLabel(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="锁定" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.locked" type="danger" effect="dark">
              <el-icon><Lock /></el-icon> 锁定
            </el-tag>
            <el-tag v-else type="success">可推进</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阻塞项" min-width="260">
          <template #default="{ row }">
            <div v-if="row.blockers?.length" class="blockers">
              <div v-for="(b, i) in row.blockers" :key="i">· {{ b }}</div>
              <div v-if="row.blockerCount > row.blockers.length" class="small">还有 {{ row.blockerCount - row.blockers.length }} 项…</div>
            </div>
            <span v-else class="small">无</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="auth.role === 'R3'" @click="openAdvance(row)">推进</el-button>
            <el-button link type="warning" :disabled="auth.role === 'R3'" @click="openRollback(row)">回退</el-button>
            <el-button link type="primary" @click="$router.push(`/persons/${row.personId}/materials`)">材料</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                       layout="total, prev, pager, next" @current-change="loadBoard" />
      </div>
    </div>

    <el-dialog v-model="advanceVisible" title="推进阶段" width="700px" top="5vh">
      <template v-if="check">
        <el-alert v-if="!check.passed" type="error" :closable="false" show-icon
                  title="存在阻塞项，不能推进" style="margin-bottom:12px" />
        <el-alert v-else type="success" :closable="false" show-icon
                  title="六项规则校验通过，请填写推进依据并二次确认" style="margin-bottom:12px" />
        <div v-if="check.blockers?.length" class="blocker-list">
          <div v-for="(b, i) in check.blockers" :key="i" class="blocker-item">
            <el-tag type="danger" size="small">{{ b.code }}</el-tag>
            <span>{{ b.message }}</span>
            <span class="small">→ {{ b.action }}</span>
          </div>
        </div>
        <el-descriptions :column="4" border size="small" style="margin-bottom:12px">
          <el-descriptions-item label="人员">{{ check.personName }}</el-descriptions-item>
          <el-descriptions-item label="当前阶段">{{ check.currentStage }}</el-descriptions-item>
          <el-descriptions-item label="目标阶段">{{ check.targetStage }}</el-descriptions-item>
          <el-descriptions-item label="材料">{{ check.materialSummary?.passedCount }} / {{ check.materialSummary?.requiredCount }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="check.warnings?.length" class="warnings">
          <el-tag v-for="w in check.warnings" :key="w" type="warning" size="small">{{ w }}</el-tag>
        </div>
        <el-form :model="form" label-width="100px">
          <el-form-item label="会议日期" required>
            <el-date-picker v-model="form.meetingDate" value-format="YYYY-MM-DD" type="date" style="width:100%" />
          </el-form-item>
          <el-form-item v-if="needFiling" label="备案日期" required>
            <el-date-picker v-model="form.filingDate" value-format="YYYY-MM-DD" type="date" style="width:100%" />
          </el-form-item>
          <el-form-item v-if="needApproval" label="批复日期" required>
            <el-date-picker v-model="form.approvalDate" value-format="YYYY-MM-DD" type="date" style="width:100%" />
          </el-form-item>
          <el-form-item label="推进依据" required>
            <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="如：支部委员会讨论确定、公示无异议、上级备案日期等" />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="form.confirm">我已核对人员、阶段、材料与会议依据，确认提交</el-checkbox>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="advanceVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!check?.passed" :loading="submitting" @click="submitAdvance">确认推进</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rollbackVisible" title="回退阶段" width="560px">
      <el-alert type="warning" :closable="false" show-icon
                title="回退会将当前阶段材料置为已退回，并写入审计日志；仅支部书记及以上可操作。" />
      <el-form :model="rollbackForm" label-width="100px" style="margin-top:14px">
        <el-form-item label="当前阶段"><el-tag type="danger">{{ rollbackRow?.currentStage }}</el-tag></el-form-item>
        <el-form-item label="回退到"><el-tag type="warning">{{ previousStage(rollbackRow?.currentStage) }}</el-tag></el-form-item>
        <el-form-item label="回退理由" required>
          <el-input v-model="rollbackForm.reason" type="textarea" :rows="3" placeholder="至少 10 个字，说明程序或材料问题" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="rollbackForm.confirm">我确认回退并承担相应审批责任</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitRollback">确认回退</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import echarts from '../utils/echarts'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { advance, advanceCheck, board, overview, rollback, stageDistribution } from '../api/process'

const auth = useAuthStore()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({ branchId: null, stage: '', keyword: '' })
const stats = reactive({ total: 0, locked: 0, red: 0, yellow: 0, green: 0, overdue: 0 })
const distribution = ref([])
const loading = ref(false)
const chartRef = ref(null)
let chart = null

const advanceVisible = ref(false)
const check = ref(null)
const currentRow = ref(null)
const form = reactive({ meetingDate: '', filingDate: '', approvalDate: '', reason: '', confirm: false })
const needFiling = ref(false)
const needApproval = ref(false)
const submitting = ref(false)

const rollbackVisible = ref(false)
const rollbackRow = ref(null)
const rollbackForm = reactive({ reason: '', confirm: false })

const stageLabel = (s) => ({ S0: 'S0 申请人', S1: 'S1 积极分子', S2: 'S2 发展对象', S3: 'S3 预备党员', S4: 'S4 正式党员' }[s] || s)
const riskType = (v) => ({ RED: 'danger', YELLOW: 'warning', GREEN: 'success' }[v] || 'info')
const riskLabel = (v) => ({ RED: '高', YELLOW: '中', GREEN: '低' }[v] || v)
const percent = (count) => {
  const max = Math.max(...distribution.value.map((d) => d.count), 1)
  return Math.round((count / max) * 100)
}
const previousStage = (s) => ({ S1: 'S0', S2: 'S1', S3: 'S2', S4: 'S3' }[s] || '')

async function loadBoard() {
  loading.value = true
  try {
    const data = await board({
      page: page.value,
      size: size.value,
      branchId: filters.branchId || undefined,
      stage: filters.stage || undefined,
      keyword: filters.keyword || undefined
    })
    rows.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [o, d] = await Promise.all([
    overview({ branchId: filters.branchId || undefined }),
    stageDistribution({ branchId: filters.branchId || undefined })
  ])
  Object.assign(stats, o)
  distribution.value = d
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: distribution.value.map((d) => d.stage) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: distribution.value.map((d) => d.count),
      barWidth: 34,
      itemStyle: { color: '#b42318', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  })
}

async function loadAll() {
  await Promise.all([loadBoard(), loadStats()])
}

async function openAdvance(row) {
  currentRow.value = row
  form.meetingDate = ''
  form.filingDate = ''
  form.approvalDate = ''
  form.reason = ''
  form.confirm = false
  check.value = await advanceCheck(row.personId)
  needFiling.value = ['S1', 'S2'].includes(check.value.targetStage)
  needApproval.value = ['S3', 'S4'].includes(check.value.targetStage)
  advanceVisible.value = true
}

async function submitAdvance() {
  submitting.value = true
  try {
    await advance(currentRow.value.personId, {
      targetStage: check.value.targetStage,
      meetingDate: form.meetingDate,
      filingDate: needFiling.value ? form.filingDate : undefined,
      approvalDate: needApproval.value ? form.approvalDate : undefined,
      reason: form.reason,
      confirm: form.confirm
    })
    ElMessage.success('阶段推进成功')
    advanceVisible.value = false
    await loadAll()
  } finally {
    submitting.value = false
  }
}

function openRollback(row) {
  rollbackRow.value = row
  rollbackForm.reason = ''
  rollbackForm.confirm = false
  rollbackVisible.value = true
}

async function submitRollback() {
  if (rollbackForm.reason.trim().length < 10) {
    ElMessage.warning('回退理由至少 10 个字')
    return
  }
  submitting.value = true
  try {
    await rollback(rollbackRow.value.personId, {
      targetStage: previousStage(rollbackRow.value.currentStage),
      reason: rollbackForm.reason,
      confirm: rollbackForm.confirm
    })
    ElMessage.success('已回退')
    rollbackVisible.value = false
    await loadAll()
  } finally {
    submitting.value = false
  }
}

function onResize() {
  chart?.resize()
}

onMounted(async () => {
  await loadAll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.five {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}
.grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 14px;
  margin-top: 14px;
}
.chart {
  height: 240px;
}
h3 {
  margin: 0 0 14px;
  font-size: 16px;
}
.dist-row {
  display: grid;
  grid-template-columns: 90px 1fr 40px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.dist-label {
  font-size: 13px;
}
.dist-count {
  text-align: right;
  font-weight: 600;
}
.small {
  color: #667085;
  font-size: 12px;
}
.blockers {
  font-size: 12px;
  color: #b42318;
  line-height: 1.6;
}
.blocker-list {
  margin-bottom: 12px;
}
.blocker-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 6px 0;
  border-bottom: 1px dashed #eaecf0;
  font-size: 13px;
}
.warnings {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
@media (max-width: 1200px) {
  .five { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .grid { grid-template-columns: 1fr; }
}
</style>