<template>
  <div>
    <h2 class="page-title">时限预警</h2>
    <p class="page-subtitle">按 30/15/7/3 天分级预警；逾期未处置会阻止阶段推进。</p>

    <div class="stat-grid five">
      <div class="stat-card"><div class="label">待处理</div><div class="value">{{ summary.open }}</div></div>
      <div class="stat-card"><div class="label">逾期</div><div class="value" style="color:#b42318">{{ summary.overdue }}</div></div>
      <div class="stat-card"><div class="label">紧迫</div><div class="value" style="color:#b42318">{{ summary.urgent }}</div></div>
      <div class="stat-card"><div class="label">临期</div><div class="value" style="color:#c79a3b">{{ summary.near }}</div></div>
      <div class="stat-card"><div class="label">已处理</div><div class="value" style="color:#12b76a">{{ summary.handled }}</div></div>
    </div>

    <div class="card" style="margin-top:14px">
      <div class="toolbar">
        <el-select v-if="auth.role === 'R1'" v-model="filters.branchId" placeholder="全部支部" clearable style="width:180px" @change="load">
          <el-option label="软件工程系学生党支部" :value="1" />
          <el-option label="计算机科学与技术系学生党支部" :value="2" />
        </el-select>
        <el-select v-model="filters.level" placeholder="全部级别" clearable style="width:130px" @change="load">
          <el-option label="逾期" value="OVERDUE" />
          <el-option label="紧迫" value="URGENT" />
          <el-option label="临期" value="NEAR" />
          <el-option label="提示" value="INFO" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px" @change="load">
          <el-option label="待处理" value="OPEN" />
          <el-option label="已处理" value="HANDLED" />
          <el-option label="已忽略" value="IGNORED" />
        </el-select>
        <el-button type="danger" @click="load">查询</el-button>
        <div style="flex:1"></div>
        <el-button v-if="auth.role === 'R1'" type="warning" plain :loading="generating" @click="doGenerate">生成预警</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="personName" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="stage" label="阶段" width="70" />
        <el-table-column prop="nodeName" label="节点" min-width="150" />
        <el-table-column prop="dueDate" label="应完成日" width="120" />
        <el-table-column prop="daysLeft" label="剩余天数" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.daysLeft < 0 ? '#b42318' : '#667085' }">{{ row.daysLeft }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }"><el-tag :type="levelType(row.level)">{{ levelLabel(row.level) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'danger' : 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleNote" label="处置说明" min-width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'OPEN'" link type="danger" @click="openHandle(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                       layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="handleVisible" title="处置预警" width="520px">
      <el-descriptions :column="2" border size="small" style="margin-bottom:12px">
        <el-descriptions-item label="人员">{{ current?.personName }}</el-descriptions-item>
        <el-descriptions-item label="节点">{{ current?.nodeName }}</el-descriptions-item>
        <el-descriptions-item label="应完成日">{{ current?.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="级别">{{ levelLabel(current?.level) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="处置结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio label="HANDLED">已处理</el-radio>
            <el-radio label="IGNORED">忽略</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处置说明">
          <el-input v-model="handleForm.note" type="textarea" :rows="3" placeholder="如：已联系支部书记完成谈话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { generateWarnings, handleWarning, listWarnings, warningSummary } from '../api/warning'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const generating = ref(false)
const filters = reactive({ branchId: null, level: '', status: 'OPEN' })
const summary = reactive({ open: 0, overdue: 0, urgent: 0, near: 0, handled: 0 })
const handleVisible = ref(false)
const current = ref(null)
const handleForm = reactive({ status: 'HANDLED', note: '' })
const submitting = ref(false)

const levelLabel = (v) => ({ OVERDUE: '逾期', URGENT: '紧迫', NEAR: '临期', INFO: '提示' }[v] || v)
const levelType = (v) => ({ OVERDUE: 'danger', URGENT: 'danger', NEAR: 'warning', INFO: 'info' }[v] || 'info')
const statusLabel = (v) => ({ OPEN: '待处理', HANDLED: '已处理', IGNORED: '已忽略' }[v] || v)

async function load() {
  loading.value = true
  try {
    const [data, sum] = await Promise.all([
      listWarnings({
        page: page.value,
        size: size.value,
        level: filters.level || undefined,
        status: filters.status || undefined,
        branchId: filters.branchId || undefined
      }),
      warningSummary({ branchId: filters.branchId || undefined })
    ])
    rows.value = data.items
    total.value = data.total
    Object.assign(summary, sum)
  } finally {
    loading.value = false
  }
}

async function doGenerate() {
  generating.value = true
  try {
    const r = await generateWarnings()
    ElMessage.success(`已生成 ${r.created} 条新预警`)
    await load()
  } finally {
    generating.value = false
  }
}

function openHandle(row) {
  current.value = row
  handleForm.status = 'HANDLED'
  handleForm.note = ''
  handleVisible.value = true
}

async function submitHandle() {
  submitting.value = true
  try {
    await handleWarning(current.value.warningId, handleForm)
    ElMessage.success('已处置')
    handleVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.five { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
@media (max-width: 1200px) { .five { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>