<template>
  <div>
    <h2 class="page-title">时限规则</h2>
    <p class="page-subtitle">节点、起算事件、时限与预警提前量均可配置；上线前须对照组织部现行口径核定。</p>

    <div class="card">
      <el-table :data="rules" v-loading="loading" border size="small">
        <el-table-column prop="nodeCode" label="节点" width="80" />
        <el-table-column prop="nodeName" label="名称" min-width="160" />
        <el-table-column prop="stage" label="阶段" width="70">
          <template #default="{ row }">{{ row.stage || '通用' }}</template>
        </el-table-column>
        <el-table-column prop="nodeType" label="类型" width="90" />
        <el-table-column prop="startEvent" label="起算事件" width="140" />
        <el-table-column label="时限" width="90">
          <template #default="{ row }">{{ row.offsetValue }} {{ row.offsetUnit === 'MONTH' ? '月' : '天' }}</template>
        </el-table-column>
        <el-table-column prop="warnOffsets" label="预警提前量" min-width="140" />
        <el-table-column prop="dependsOnNode" label="前置节点" width="120" />
        <el-table-column prop="responsibleRole" label="责任角色" width="90" />
        <el-table-column prop="enabled" label="启用" width="70">
          <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" title="编辑时限规则" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="节点">{{ form.nodeCode }}</el-form-item>
        <el-form-item label="名称"><el-input v-model="form.nodeName" /></el-form-item>
        <el-form-item label="适用阶段">
          <el-select v-model="form.stage" style="width:100%" clearable placeholder="通用">
            <el-option v-for="s in ['S0','S1','S2','S3','S4']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="时限值"><el-input-number v-model="form.offsetValue" :min="0" /></el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.offsetUnit" style="width:100%">
            <el-option label="天" value="DAY" />
            <el-option label="月" value="MONTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警提前量">
          <el-input v-model="form.warnOffsetsText" placeholder="如 30,15,7,3" />
        </el-form-item>
        <el-form-item label="责任角色">
          <el-select v-model="form.responsibleRole" style="width:100%">
            <el-option label="R1 超级管理员" value="R1" />
            <el-option label="R2 支部书记" value="R2" />
            <el-option label="R3 党务助理" value="R3" />
            <el-option label="R4 学生" value="R4" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="danger" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTimeRules, updateTimeRule } from '../api/warning'

const rules = ref([])
const loading = ref(false)
const visible = ref(false)
const form = reactive({
  ruleId: null, nodeCode: '', nodeName: '', stage: '', offsetValue: 0,
  offsetUnit: 'DAY', warnOffsetsText: '', responsibleRole: 'R2', enabled: 1
})

async function load() {
  loading.value = true
  try {
    rules.value = await listTimeRules()
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, {
    ruleId: row.ruleId,
    nodeCode: row.nodeCode,
    nodeName: row.nodeName,
    stage: row.stage || '',
    offsetValue: row.offsetValue,
    offsetUnit: row.offsetUnit,
    warnOffsetsText: parseOffsets(row.warnOffsets).join(','),
    responsibleRole: row.responsibleRole,
    enabled: row.enabled
  })
  visible.value = true
}

function parseOffsets(json) {
  try {
    const arr = JSON.parse(json || '[]')
    return arr.map((x) => (typeof x === 'number' ? x : x.days)).filter((x) => x != null)
  } catch (e) {
    return []
  }
}

async function save() {
  const warnOffsets = form.warnOffsetsText.split(/[,，\s]+/).map((x) => Number(x)).filter((x) => !Number.isNaN(x))
  await updateTimeRule(form.ruleId, {
    nodeName: form.nodeName,
    stage: form.stage || null,
    offsetValue: form.offsetValue,
    offsetUnit: form.offsetUnit,
    warnOffsets,
    responsibleRole: form.responsibleRole,
    enabled: form.enabled
  })
  ElMessage.success('已保存')
  visible.value = false
  await load()
}

onMounted(load)
</script>