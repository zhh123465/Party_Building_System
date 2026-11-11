<template>
  <div>
    <h2 class="page-title">人员台账</h2>
    <p class="page-subtitle">统一台账 · 阶段可视 · 敏感字段默认掩码</p>

    <div class="card">
      <div class="toolbar">
        <el-select v-if="auth.role === 'R1'" v-model="filters.branchId" placeholder="全部支部" clearable style="width: 190px">
          <el-option label="软件工程系学生党支部" :value="1" />
          <el-option label="计算机科学与技术系学生党支部" :value="2" />
        </el-select>
        <el-select v-model="filters.stage" placeholder="全部阶段" clearable style="width: 150px">
          <el-option v-for="s in stages" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="姓名 / 学号" clearable style="width: 200px" @keyup.enter="load" />
        <el-button type="danger" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div style="flex:1"></div>
        <el-button v-if="auth.role !== 'R4'" type="warning" plain @click="importVisible = true">批量导入</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="branchId" label="支部" width="110">
          <template #default="{ row }">{{ row.branchId === 1 ? '软件工程系' : '计算机系' }}</template>
        </el-table-column>
        <el-table-column prop="currentStage" label="当前阶段" width="120">
          <template #default="{ row }">
            <el-tag :type="stageType(row.currentStage)" effect="light">{{ stageLabel(row.currentStage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stageStartDate" label="阶段起始日" width="130" />
        <el-table-column prop="phoneMask" label="联系电话" width="130" />
        <el-table-column prop="idCardMask" label="身份证号" min-width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="人员详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ detail.college }}</el-descriptions-item>
        <el-descriptions-item label="专业 / 班级">{{ detail.major }} / {{ detail.className }}</el-descriptions-item>
        <el-descriptions-item label="当前阶段">{{ stageLabel(detail.currentStage) }}</el-descriptions-item>
        <el-descriptions-item label="阶段起始日">{{ detail.stageStartDate }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phoneMask }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">
          {{ idCardFull || detail.idCardMask }}
          <el-button
            v-if="canViewIdCard && !idCardFull"
            link
            type="danger"
            style="margin-left:8px"
            @click="showIdCard"
          >查看完整</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入人员" width="560px">
      <el-alert type="warning" :closable="false" show-icon title="请使用虚构样例数据，禁止导入真实政审材料。" />
      <div style="margin:16px 0">
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="onFileChange"
          :on-exceed="() => ElMessage.warning('一次只能上传一个文件')"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽 Excel 到此处，或<em>点击选择</em></div>
          <template #tip><div class="el-upload__tip">支持 .xlsx / .xls，建议先下载模板</div></template>
        </el-upload>
      </div>
      <div v-if="importResult" class="import-result">
        <el-tag type="success">成功 {{ importResult.success }}</el-tag>
        <el-tag type="danger" style="margin-left:8px">失败 {{ importResult.failed }}</el-tag>
        <el-table v-if="importResult.errors?.length" :data="importResult.errors" size="small" style="margin-top:10px" max-height="220">
          <el-table-column prop="rowNo" label="行号" width="70" />
          <el-table-column prop="column" label="列" width="120" />
          <el-table-column prop="message" label="错误" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="danger" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getPerson, getIdCard, importPersons, listPersons } from '../api/person'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const stages = [
  { value: 'S0', label: 'S0 入党申请人' },
  { value: 'S1', label: 'S1 入党积极分子' },
  { value: 'S2', label: 'S2 发展对象' },
  { value: 'S3', label: 'S3 预备党员' },
  { value: 'S4', label: 'S4 正式党员' }
]
const filters = reactive({ branchId: null, stage: '', keyword: '' })
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const idCardFull = ref('')
const canViewIdCard = computed(() => ['R1', 'R2'].includes(auth.role))
const importVisible = ref(false)
const importing = ref(false)
const importResult = ref(null)
let importFile = null

function stageLabel(v) {
  return stages.find((s) => s.value === v)?.label || v
}
function stageType(v) {
  return { S0: 'info', S1: 'warning', S2: 'danger', S3: 'success', S4: 'primary' }[v] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await listPersons({
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

function reset() {
  filters.branchId = null
  filters.stage = ''
  filters.keyword = ''
  page.value = 1
  load()
}

async function openDetail(row) {
  idCardFull.value = ''
  detail.value = await getPerson(row.personId)
  detailVisible.value = true
}

async function showIdCard() {
  idCardFull.value = await getIdCard(detail.value.personId)
}

function onFileChange(file) {
  importFile = file.raw
  importResult.value = null
}

async function doImport() {
  if (!importFile) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile)
    importResult.value = await importPersons(fd)
    ElMessage.success(`导入完成：成功 ${importResult.value.success} 行，失败 ${importResult.value.failed} 行`)
    await load()
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.import-result {
  border-top: 1px solid #eaecf0;
  padding-top: 12px;
}
</style>