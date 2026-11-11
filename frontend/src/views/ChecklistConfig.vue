<template>
  <div>
    <h2 class="page-title">材料清单配置</h2>
    <p class="page-subtitle">阶段—材料映射、份数/页数要求、核验要点均在此维护；发布后新人员按新版本执行。</p>

    <div class="card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="清单模板" name="templates">
          <div class="toolbar">
            <el-select v-model="selectedId" placeholder="选择模板" style="width: 360px" @change="selectTemplate">
              <el-option
                v-for="t in templates"
                :key="t.templateId"
                :label="`${t.templateName} · v${t.versionNo} · ${t.status}`"
                :value="t.templateId"
              />
            </el-select>
            <el-button type="danger" @click="openTemplateDialog">新建模板</el-button>
            <el-button :disabled="!selectedId" @click="doClone">克隆新版本</el-button>
            <el-button :disabled="!selectedId" @click="doValidate">校验</el-button>
            <el-button type="warning" :disabled="!selectedId || selected?.status !== 'DRAFT'" @click="doPublish">发布</el-button>
            <div style="flex:1"></div>
            <el-tag v-if="selected" :type="selected.status === 'PUBLISHED' ? 'success' : 'info'">{{ selected.status }}</el-tag>
          </div>

          <template v-if="selected">
            <el-descriptions :column="4" border size="small" style="margin-bottom:12px">
              <el-descriptions-item label="模板">{{ selected.templateName }}</el-descriptions-item>
              <el-descriptions-item label="编码">{{ selected.templateCode }}</el-descriptions-item>
              <el-descriptions-item label="学年版">{{ selected.academicYearId }}</el-descriptions-item>
              <el-descriptions-item label="版本">v{{ selected.versionNo }}</el-descriptions-item>
            </el-descriptions>

            <div class="toolbar">
              <el-button type="danger" size="small" :disabled="selected.status !== 'DRAFT'" @click="openItemDialog()">新增条目</el-button>
              <span class="hint">仅草稿模板可编辑；发布后历史人员沿用原版本。</span>
            </div>

            <el-table :data="selected.items" border size="small">
              <el-table-column prop="stage" label="阶段" width="70" />
              <el-table-column prop="materialTypeName" label="材料类型" min-width="200" />
              <el-table-column prop="required" label="必填" width="70">
                <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column prop="minCount" label="最少份数" width="90" />
              <el-table-column prop="minPages" label="最少页数" width="90">
                <template #default="{ row }">{{ row.minPages ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="verifyPoints" label="核验要点" min-width="260">
                <template #default="{ row }">
                  <el-tag v-for="p in row.verifyPoints" :key="p" size="small" style="margin:2px">{{ p }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button link type="danger" :disabled="selected.status !== 'DRAFT'" @click="openItemDialog(row)">编辑</el-button>
                  <el-button link type="danger" :disabled="selected.status !== 'DRAFT'" @click="removeItem(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="请选择或新建一个清单模板" />
        </el-tab-pane>

        <el-tab-pane label="材料类型" name="types">
          <div class="toolbar">
            <el-button type="danger" size="small" @click="openTypeDialog()">新增材料类型</el-button>
          </div>
          <el-table :data="materialTypes" border size="small">
            <el-table-column prop="typeCode" label="编码" width="160" />
            <el-table-column prop="typeName" label="名称" min-width="180" />
            <el-table-column prop="stageScope" label="适用阶段" width="110" />
            <el-table-column prop="fileFormats" label="格式" min-width="150" />
            <el-table-column prop="defaultRequired" label="默认必填" width="90">
              <template #default="{ row }">{{ row.defaultRequired ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" width="70" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="danger" @click="openTypeDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="typeDialogVisible" :title="typeForm.typeId ? '编辑材料类型' : '新增材料类型'" width="520px">
      <el-form :model="typeForm" label-width="100px">
        <el-form-item label="编码"><el-input v-model="typeForm.typeCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="typeForm.typeName" /></el-form-item>
        <el-form-item label="适用阶段"><el-input v-model="typeForm.stageScope" placeholder="如 S0 或 S1,S2,S3" /></el-form-item>
        <el-form-item label="格式"><el-input v-model="typeForm.fileFormats" placeholder="pdf,jpg,png,docx" /></el-form-item>
        <el-form-item label="默认必填"><el-switch v-model="typeForm.defaultRequired" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="typeForm.sortNo" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialogVisible" title="新建模板" width="520px">
      <el-form :model="templateForm" label-width="100px">
        <el-form-item label="模板编码"><el-input v-model="templateForm.templateCode" /></el-form-item>
        <el-form-item label="模板名称"><el-input v-model="templateForm.templateName" /></el-form-item>
        <el-form-item label="学年版 ID"><el-input-number v-model="templateForm.academicYearId" :min="1" /></el-form-item>
        <el-form-item label="生效日期"><el-date-picker v-model="templateForm.effectiveFrom" value-format="YYYY-MM-DD" type="date" /></el-form-item>
        <el-form-item label="失效日期"><el-date-picker v-model="templateForm.effectiveTo" value-format="YYYY-MM-DD" type="date" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="saveTemplate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialogVisible" :title="itemForm.itemId ? '编辑条目' : '新增条目'" width="560px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item label="阶段">
          <el-select v-model="itemForm.stage" style="width:100%">
            <el-option v-for="s in ['S0','S1','S2','S3','S4']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="材料类型">
          <el-select v-model="itemForm.materialTypeId" style="width:100%" filterable>
            <el-option v-for="t in materialTypes" :key="t.typeId" :label="`${t.typeName}（${t.typeCode}）`" :value="t.typeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="必填"><el-switch v-model="itemForm.required" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="最少份数"><el-input-number v-model="itemForm.minCount" :min="1" /></el-form-item>
        <el-form-item label="最少页数"><el-input-number v-model="itemForm.minPages" :min="0" /></el-form-item>
        <el-form-item label="核验要点">
          <el-input v-model="itemForm.verifyPointsText" type="textarea" :rows="3" placeholder="逗号或换行分隔" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="itemForm.sortNo" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addTemplateItem, cloneTemplate, createMaterialType, createTemplate, deleteTemplateItem,
  getTemplate, listMaterialTypes, listTemplates, publishTemplate, updateMaterialType,
  updateTemplateItem, validateTemplate
} from '../api/checklist'

const activeTab = ref('templates')
const materialTypes = ref([])
const templates = ref([])
const selectedId = ref(null)
const selected = ref(null)
const typeDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const itemDialogVisible = ref(false)

const typeForm = reactive({ typeId: null, typeCode: '', typeName: '', stageScope: 'S0', fileFormats: 'pdf,jpg,png,docx', defaultRequired: 1, sortNo: 0 })
const templateForm = reactive({ templateCode: 'DEFAULT', templateName: '党员发展材料清单', academicYearId: 1, effectiveFrom: '2026-09-01', effectiveTo: '2027-08-31' })
const itemForm = reactive({ itemId: null, stage: 'S0', materialTypeId: null, required: 1, minCount: 1, minPages: 1, verifyPointsText: '', sortNo: 0 })

const canEdit = computed(() => selected.value?.status === 'DRAFT')

async function loadAll() {
  const [types, list] = await Promise.all([listMaterialTypes(), listTemplates()])
  materialTypes.value = types
  templates.value = list
  if (!selectedId.value && list.length) {
    selectedId.value = list[0].templateId
  }
  if (selectedId.value) {
    await selectTemplate(selectedId.value)
  }
}

async function selectTemplate(id) {
  selected.value = await getTemplate(id)
}

function openTypeDialog(row) {
  Object.assign(typeForm, row || { typeId: null, typeCode: '', typeName: '', stageScope: 'S0', fileFormats: 'pdf,jpg,png,docx', defaultRequired: 1, sortNo: 0 })
  typeDialogVisible.value = true
}

async function saveType() {
  if (!typeForm.typeCode || !typeForm.typeName) {
    ElMessage.warning('请填写编码和名称')
    return
  }
  if (typeForm.typeId) {
    await updateMaterialType(typeForm.typeId, typeForm)
  } else {
    await createMaterialType(typeForm)
  }
  ElMessage.success('已保存')
  typeDialogVisible.value = false
  await loadAll()
}

function openTemplateDialog() {
  Object.assign(templateForm, { templateCode: 'DEFAULT', templateName: '党员发展材料清单', academicYearId: 1, effectiveFrom: '2026-09-01', effectiveTo: '2027-08-31' })
  templateDialogVisible.value = true
}

async function saveTemplate() {
  const t = await createTemplate(templateForm)
  ElMessage.success('已创建草稿')
  templateDialogVisible.value = false
  await loadAll()
  selectedId.value = t.templateId
  await selectTemplate(t.templateId)
}

async function doClone() {
  const t = await cloneTemplate(selectedId.value)
  ElMessage.success('已克隆新版本')
  await loadAll()
  selectedId.value = t.templateId
  await selectTemplate(t.templateId)
}

async function doValidate() {
  const result = await validateTemplate(selectedId.value)
  if (result.valid) {
    ElMessage.success('校验通过')
  } else {
    ElMessageBox.alert(result.errors.join('\n'), '校验未通过', { type: 'warning' })
  }
}

async function doPublish() {
  await ElMessageBox.confirm('发布后该模板将生效，历史人员仍沿用原版本。确认发布？', '发布模板', { type: 'warning' })
  await publishTemplate(selectedId.value)
  ElMessage.success('已发布')
  await loadAll()
}

function openItemDialog(row) {
  if (!canEdit.value) {
    ElMessage.warning('只有草稿模板可编辑')
    return
  }
  if (row) {
    Object.assign(itemForm, {
      itemId: row.itemId, stage: row.stage, materialTypeId: row.materialTypeId, required: row.required,
      minCount: row.minCount, minPages: row.minPages ?? 0, verifyPointsText: (row.verifyPoints || []).join('，'), sortNo: row.sortNo
    })
  } else {
    Object.assign(itemForm, { itemId: null, stage: 'S0', materialTypeId: null, required: 1, minCount: 1, minPages: 1, verifyPointsText: '', sortNo: 0 })
  }
  itemDialogVisible.value = true
}

function parsePoints(text) {
  return (text || '').split(/[,，;；\n]/).map((s) => s.trim()).filter(Boolean)
}

async function saveItem() {
  if (!itemForm.materialTypeId) {
    ElMessage.warning('请选择材料类型')
    return
  }
  const payload = {
    stage: itemForm.stage,
    materialTypeId: itemForm.materialTypeId,
    required: itemForm.required,
    minCount: itemForm.minCount,
    minPages: itemForm.minPages || null,
    verifyPoints: parsePoints(itemForm.verifyPointsText),
    sortNo: itemForm.sortNo
  }
  if (itemForm.itemId) {
    await updateTemplateItem(itemForm.itemId, payload)
  } else {
    await addTemplateItem(selectedId.value, payload)
  }
  ElMessage.success('已保存')
  itemDialogVisible.value = false
  await selectTemplate(selectedId.value)
}

async function removeItem(row) {
  await ElMessageBox.confirm(`确认删除“${row.materialTypeName}”？`, '删除条目', { type: 'warning' })
  await deleteTemplateItem(row.itemId)
  ElMessage.success('已删除')
  await selectTemplate(selectedId.value)
}

onMounted(loadAll)
</script>

<style scoped>
.hint {
  color: #667085;
  font-size: 12px;
}
</style>