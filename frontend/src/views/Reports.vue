<template>
  <div>
    <h2 class="page-title">统计报表与合规指数</h2>
    <p class="page-subtitle">页面与导出共用同一套聚合口径；合规指数 C = 0.40×M + 0.35×T + 0.25×Q。</p>

    <div class="stat-grid five">
      <div class="stat-card"><div class="label">人员总数</div><div class="value">{{ overview.totalPersons }}</div></div>
      <div class="stat-card"><div class="label">材料完成率</div><div class="value">{{ overview.completionRate }}%</div></div>
      <div class="stat-card"><div class="label">平均合规指数</div><div class="value">{{ round(overview.averageCompliance) }}</div></div>
      <div class="stat-card"><div class="label">逾期节点</div><div class="value" style="color:#b42318">{{ overview.overdueWarnings }}</div></div>
      <div class="stat-card"><div class="label">在办人员</div><div class="value">{{ overview.activePersons }}</div></div>
    </div>

    <div class="card" style="margin-top:14px">
      <div class="toolbar">
        <el-select v-if="auth.role === 'R1'" v-model="branchId" placeholder="全部支部" clearable style="width:190px" @change="loadAll">
          <el-option label="软件工程系学生党支部" :value="1" />
          <el-option label="计算机科学与技术系学生党支部" :value="2" />
        </el-select>
        <el-select v-model="stage" placeholder="全部阶段" clearable style="width:140px" @change="loadCompliance">
          <el-option v-for="s in ['S0','S1','S2','S3','S4']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="danger" @click="loadAll">刷新</el-button>
        <div style="flex:1"></div>
        <el-button type="warning" plain :loading="exporting" @click="exportExcel">导出 Excel</el-button>
      </div>

      <div class="grid">
        <div>
          <h3>阶段人数分布</h3>
          <div ref="stageChartRef" class="chart"></div>
        </div>
        <div>
          <h3>支部 × 阶段合规热力图</h3>
          <div ref="heatmapRef" class="chart"></div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:14px">
      <h3>人员合规指数</h3>
      <el-table :data="compliance" v-loading="loading" stripe>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="branchName" label="支部" min-width="180" />
        <el-table-column prop="stage" label="阶段" width="70" />
        <el-table-column label="材料 M" width="130">
          <template #default="{ row }">
            <el-progress :percentage="row.materialScore" :show-text="false" :stroke-width="10" />
            <span class="small">{{ row.passed }}/{{ row.required }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="timeScore" label="时效 T" width="90" />
        <el-table-column prop="contentScore" label="内容 Q" width="90" />
        <el-table-column prop="totalScore" label="合规指数" width="100">
          <template #default="{ row }"><strong>{{ row.totalScore }}</strong></template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险" width="90">
          <template #default="{ row }"><el-tag :type="riskType(row.riskLevel)">{{ riskLabel(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="overdue" label="逾期" width="70" />
        <el-table-column prop="doubt" label="语义存疑" width="90" />
        <el-table-column prop="conflict" label="冲突" width="70" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import echarts from '../utils/echarts'
import { ElMessage } from 'element-plus'
import { complianceHeatmap, complianceList, reportOverview } from '../api/report'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const overview = reactive({
  totalPersons: 0, activePersons: 0, completionRate: 0,
  overdueWarnings: 0, averageCompliance: 100, stageCounts: []
})
const compliance = ref([])
const heatmap = ref([])
const branchId = ref(null)
const stage = ref('')
const loading = ref(false)
const exporting = ref(false)
const stageChartRef = ref(null)
const heatmapRef = ref(null)
let stageChart = null
let heatmapChart = null

const round = (v) => Math.round((v || 0) * 10) / 10
const riskLabel = (v) => ({ RED: '高', YELLOW: '中', GREEN: '低' }[v] || v)
const riskType = (v) => ({ RED: 'danger', YELLOW: 'warning', GREEN: 'success' }[v] || 'info')

async function loadOverview() {
  const data = await reportOverview({ branchId: branchId.value || undefined })
  Object.assign(overview, data)
  await nextTick()
  renderStageChart()
}

async function loadCompliance() {
  loading.value = true
  try {
    compliance.value = await complianceList({ branchId: branchId.value || undefined, stage: stage.value || undefined })
  } finally {
    loading.value = false
  }
}

async function loadHeatmap() {
  heatmap.value = await complianceHeatmap({ branchId: branchId.value || undefined })
  await nextTick()
  renderHeatmap()
}

async function loadAll() {
  await Promise.all([loadOverview(), loadCompliance(), loadHeatmap()])
}

function renderStageChart() {
  if (!stageChartRef.value) return
  if (!stageChart) stageChart = echarts.init(stageChartRef.value)
  stageChart.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: overview.stageCounts.map((x) => x.stage) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: overview.stageCounts.map((x) => x.count),
      barWidth: 34,
      itemStyle: { color: '#b42318', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  })
}

function renderHeatmap() {
  if (!heatmapRef.value) return
  if (!heatmapChart) heatmapChart = echarts.init(heatmapRef.value)
  const stages = ['S0', 'S1', 'S2', 'S3', 'S4']
  const branches = [...new Set(heatmap.value.map((x) => x.branchName))]
  const data = heatmap.value.map((x) => [stages.indexOf(x.stage), branches.indexOf(x.branchName), x.avgScore])
  heatmapChart.setOption({
    grid: { left: 110, right: 20, top: 20, bottom: 50 },
    tooltip: { formatter: (p) => `${branches[p.value[1]]} ${stages[p.value[0]]}：${p.value[2]} 分` },
    xAxis: { type: 'category', data: stages },
    yAxis: { type: 'category', data: branches },
    visualMap: {
      min: 0, max: 100, calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#d92d20', '#f79009', '#12b76a'] }
    },
    series: [{
      type: 'heatmap',
      data,
      label: { show: true, formatter: (p) => p.value[2] },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,.3)' } }
    }]
  })
}

async function exportExcel() {
  exporting.value = true
  try {
    const token = sessionStorage.getItem('pb_token')
    const params = new URLSearchParams()
    if (branchId.value) params.set('branchId', branchId.value)
    params.set('type', 'PERSON')
    const resp = await fetch(`/api/v1/reports/export?${params.toString()}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!resp.ok) {
      ElMessage.error('导出失败')
      return
    }
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '人员发展统计报表.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出')
  } finally {
    exporting.value = false
  }
}

function onResize() {
  stageChart?.resize()
  heatmapChart?.resize()
}

onMounted(async () => {
  await loadAll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  stageChart?.dispose()
  heatmapChart?.dispose()
})
</script>

<style scoped>
.five { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.grid { display: grid; grid-template-columns: 1fr 1.2fr; gap: 20px; }
.chart { height: 260px; }
h3 { margin: 0 0 10px; font-size: 16px; }
.small { color: #667085; font-size: 12px; }
@media (max-width: 1100px) {
  .five { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .five { grid-template-columns: 1fr; }
  .chart { height: 210px; }
}
</style>