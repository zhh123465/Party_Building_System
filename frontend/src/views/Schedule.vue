<template>
  <div>
    <h2 class="page-title">发展日历与倒推排期</h2>
    <p class="page-subtitle">输入拟转正日期，系统按节点依赖逆序推算各节点最晚完成日；同时展示年度预警日历。</p>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="form.personId" placeholder="选择人员" filterable style="width:220px">
          <el-option v-for="p in persons" :key="p.personId" :label="`${p.name}（${p.studentNo}）`" :value="p.personId" />
        </el-select>
        <el-date-picker v-model="form.targetDate" value-format="YYYY-MM-DD" type="date" placeholder="拟转正日期" style="width:170px" />
        <el-select v-model="form.targetNode" style="width:160px">
          <el-option label="T-11 转正审批" value="T-11" />
          <el-option label="T-10 预备期满" value="T-10" />
          <el-option label="T-08 上级审批" value="T-08" />
          <el-option label="T-04 培养考察期满" value="T-04" />
        </el-select>
        <el-button type="danger" :loading="loading" @click="doReverse">倒推排期</el-button>
      </div>

      <template v-if="result">
        <div class="result-head">
          <el-tag size="large" :type="result.feasible ? 'success' : 'danger'">
            {{ result.feasible ? '排期可行' : '时间不足' }}
          </el-tag>
          <span class="meta">{{ result.personName }} · 目标 {{ result.targetNode }} · {{ result.targetDate }}</span>
          <el-tag v-for="c in result.criticalPath" :key="c" type="danger" size="small">关键：{{ c }}</el-tag>
        </div>
        <el-table :data="result.nodes" border size="small">
          <el-table-column prop="ruleCode" label="节点" width="80" />
          <el-table-column prop="nodeName" label="名称" min-width="160" />
          <el-table-column prop="stage" label="阶段" width="70" />
          <el-table-column prop="latestStart" label="最晚开始" width="120" />
          <el-table-column prop="latestDue" label="最晚完成" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'NORMAL' ? 'success' : row.status === 'URGENT' ? 'warning' : 'danger'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dependsOn" label="前置节点" min-width="130" />
        </el-table>
      </template>
      <el-empty v-else description="选择人员与拟转正日期后开始倒推" />
    </div>

    <div class="card" style="margin-top:14px">
      <div class="toolbar">
        <h3 style="margin:0">年度预警日历</h3>
        <el-select v-model="year" style="width:120px" @change="loadCalendar">
          <el-option v-for="y in [2026, 2027, 2028]" :key="y" :label="`${y} 年`" :value="y" />
        </el-select>
        <el-button @click="loadCalendar">刷新</el-button>
      </div>
      <el-table :data="calendarItems" v-loading="calLoading" stripe size="small">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="personName" label="人员" width="100" />
        <el-table-column prop="nodeName" label="节点" min-width="160" />
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }"><el-tag :type="levelType(row.level)">{{ levelLabel(row.level) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listPersons } from '../api/person'
import { calendar, reverseSchedule } from '../api/warning'

const persons = ref([])
const form = reactive({ personId: null, targetDate: '2030-06-30', targetNode: 'T-11' })
const result = ref(null)
const loading = ref(false)
const year = ref(2026)
const calendarItems = ref([])
const calLoading = ref(false)

const levelLabel = (v) => ({ OVERDUE: '逾期', URGENT: '紧迫', NEAR: '临期', INFO: '提示' }[v] || v)
const levelType = (v) => ({ OVERDUE: 'danger', URGENT: 'danger', NEAR: 'warning', INFO: 'info' }[v] || 'info')

async function loadPersons() {
  const data = await listPersons({ page: 1, size: 100 })
  persons.value = data.items
  if (!form.personId && persons.value.length) form.personId = persons.value[0].personId
}

async function doReverse() {
  if (!form.personId || !form.targetDate) {
    ElMessage.warning('请选择人员和拟转正日期')
    return
  }
  loading.value = true
  try {
    result.value = await reverseSchedule(form)
  } finally {
    loading.value = false
  }
}

async function loadCalendar() {
  calLoading.value = true
  try {
    calendarItems.value = await calendar({ year: year.value })
  } finally {
    calLoading.value = false
  }
}

onMounted(async () => {
  await loadPersons()
  await loadCalendar()
})
</script>

<style scoped>
.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 14px 0;
  flex-wrap: wrap;
}
.meta {
  color: #667085;
  font-size: 13px;
}
h3 {
  font-size: 16px;
}
</style>