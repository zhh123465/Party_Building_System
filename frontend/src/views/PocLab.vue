<template>
  <div>
    <h2 class="page-title">POC 实验室</h2>
    <p class="page-subtitle">验证双引擎核验的关键假设：模型 JSON、OCR、端到端时延与降级。</p>

    <div class="card">
      <div class="toolbar">
        <el-tag type="success" v-if="config">模型：{{ config.model }}</el-tag>
        <el-tag type="info" v-if="config">Provider：{{ config.provider }}</el-tag>
        <el-tag type="warning" v-if="config">OCR：{{ config.ocr }}</el-tag>
        <el-tag :type="config?.mock ? 'info' : 'danger'" v-if="config">{{ config?.mock ? 'Mock 模式' : '真实 API' }}</el-tag>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="模型 JSON" name="llm">
          <el-form label-position="top">
            <el-form-item label="System Prompt">
              <el-input v-model="llm.systemPrompt" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="User Prompt">
              <el-input v-model="llm.userPrompt" type="textarea" :rows="4" />
            </el-form-item>
            <el-button type="danger" :loading="loading.llm" @click="runLlm">调用模型</el-button>
          </el-form>
          <el-alert v-if="llmResult" type="success" :closable="false" style="margin-top:14px"
            :title="`validJson=${llmResult.validJson} · ${llmResult.latencyMs}ms · ${llmResult.model}`" />
          <pre v-if="llmResult" class="code-block">{{ JSON.stringify(llmResult.parsed || llmResult.content, null, 2) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="OCR 文本抽取" name="ocr">
          <el-upload drag :auto-upload="false" :limit="1" accept=".pdf,.jpg,.jpeg,.png,.docx" :on-change="(f) => ocrFile = f.raw">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 PDF / 图片 / DOCX 到此处，或<em>点击选择</em></div>
          </el-upload>
          <div style="margin-top:12px">
            <el-button type="danger" :loading="loading.ocr" @click="runOcr">开始抽取</el-button>
          </div>
          <el-alert v-if="ocrResult" type="success" :closable="false" style="margin-top:14px"
            :title="`${ocrResult.charCount} 字 · ${ocrResult.pageCount} 页 · ${ocrResult.engine} · ${ocrResult.latencyMs}ms`" />
          <pre v-if="ocrResult" class="code-block">{{ ocrResult.text }}</pre>
        </el-tab-pane>

        <el-tab-pane label="端到端核验" name="verify">
          <el-upload drag :auto-upload="false" :limit="1" accept=".pdf,.jpg,.jpeg,.png,.docx" :on-change="(f) => verifyFile = f.raw">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">上传一份材料，运行“规则 + 语义”双引擎核验</div>
          </el-upload>
          <el-form label-position="top" style="margin-top:14px">
            <el-form-item label="核验要点（逗号或换行分隔）">
              <el-input v-model="points" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
          <el-button type="danger" :loading="loading.verify" @click="runVerify">开始核验</el-button>

          <template v-if="verifyResult">
            <div class="result-head">
              <el-tag :type="overallType(verifyResult.overall)" size="large">总体结论：{{ verifyResult.overall }}</el-tag>
              <span class="meta">{{ verifyResult.latencyMs }}ms · {{ verifyResult.engine }} · {{ verifyResult.textLength }} 字</span>
              <el-tag v-if="verifyResult.manualRequired" type="warning">已转人工</el-tag>
            </div>
            <el-alert v-if="verifyResult.error" type="warning" :closable="false" :title="verifyResult.error" style="margin:10px 0" />
            <h4>规则层</h4>
            <el-table :data="verifyResult.ruleFindings" size="small" border>
              <el-table-column prop="ruleCode" label="规则" width="120" />
              <el-table-column prop="point" label="要点" width="150" />
              <el-table-column prop="conclusion" label="结论" width="100">
                <template #default="{ row }"><el-tag :type="row.conclusion === 'PASS' ? 'success' : 'danger'">{{ row.conclusion }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="evidence" label="依据" />
            </el-table>
            <h4>语义层</h4>
            <el-table :data="verifyResult.semanticFindings" size="small" border>
              <el-table-column prop="point" label="要点" width="180" />
              <el-table-column prop="conclusion" label="结论" width="100">
                <template #default="{ row }"><el-tag :type="row.conclusion === 'PASS' ? 'success' : 'warning'">{{ row.conclusion }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="evidence" label="原文依据" />
              <el-table-column prop="confidence" label="置信度" width="80" />
            </el-table>
            <h4>文本预览</h4>
            <pre class="code-block">{{ verifyResult.textPreview }}</pre>
          </template>
        </el-tab-pane>

        <el-tab-pane label="POC 说明" name="notes">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="POC-1 模型 JSON">真实调用 OpenAI 兼容接口，校验 JSON 解析、重试与耗时。</el-descriptions-item>
            <el-descriptions-item label="POC-2 OCR">PDFBox 文本抽取；扫描件/图片走 Tesseract chi_sim+eng。</el-descriptions-item>
            <el-descriptions-item label="POC-3 时延">端到端记录抽取、规则、模型与总耗时。</el-descriptions-item>
            <el-descriptions-item label="POC-4 降级">模型失败时 semanticFindings 写入 UNKNOWN，manualRequired=true，不阻断主流程。</el-descriptions-item>
            <el-descriptions-item label="POC-5 权限">登录、JWT、R1—R4 角色与支部数据范围。</el-descriptions-item>
            <el-descriptions-item label="POC-6 导入">100 行 Excel 部分成功、错误行定位、学号幂等。</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pocConfig, pocLlm, pocOcr, pocVerify } from '../api/poc'

const config = ref(null)
const activeTab = ref('llm')
const llm = reactive({
  systemPrompt: '你是高校党务材料核验助手，只输出 JSON。',
  userPrompt: '请输出 JSON：{"ok":true,"message":"pong"}'
})
const llmResult = ref(null)
const ocrFile = ref(null)
const ocrResult = ref(null)
const verifyFile = ref(null)
const verifyResult = ref(null)
const points = ref('入党动机是否具体, 是否结合个人经历, 是否存在空泛套话')
const loading = reactive({ llm: false, ocr: false, verify: false })

function overallType(v) {
  return { PASS: 'success', DOUBT: 'warning', NEED_FIX: 'danger', ERROR: 'info' }[v] || 'info'
}

async function runLlm() {
  loading.llm = true
  try {
    llmResult.value = await pocLlm(llm)
  } finally {
    loading.llm = false
  }
}

async function runOcr() {
  if (!ocrFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  loading.ocr = true
  try {
    const fd = new FormData()
    fd.append('file', ocrFile.value)
    ocrResult.value = await pocOcr(fd)
  } finally {
    loading.ocr = false
  }
}

async function runVerify() {
  if (!verifyFile.value) {
    ElMessage.warning('请先选择材料文件')
    return
  }
  loading.verify = true
  try {
    const fd = new FormData()
    fd.append('file', verifyFile.value)
    fd.append('points', points.value)
    verifyResult.value = await pocVerify(fd)
  } finally {
    loading.verify = false
  }
}

onMounted(async () => {
  try {
    config.value = await pocConfig()
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0 10px;
}
.meta {
  color: #667085;
  font-size: 13px;
}
h4 {
  margin: 18px 0 8px;
}
</style>