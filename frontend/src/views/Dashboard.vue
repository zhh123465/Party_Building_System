<template>
  <div>
    <h2 class="page-title">工作台</h2>
    <p class="page-subtitle">欢迎，{{ auth.displayName }}。当前为工程骨架与 POC 验证环境。</p>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="label">人员档案</div>
        <div class="value">{{ stats.total }}</div>
      </div>
      <div class="stat-card">
        <div class="label">当前阶段</div>
        <div class="value">S0—S4</div>
      </div>
      <div class="stat-card">
        <div class="label">双引擎核验</div>
        <div class="value" style="color:#b42318">POC</div>
      </div>
      <div class="stat-card">
        <div class="label">我的角色</div>
        <div class="value" style="font-size:20px">{{ roleLabel }}</div>
      </div>
    </div>

    <div class="grid">
      <div class="card">
        <h3>快速入口</h3>
        <div class="quick">
          <el-button type="danger" plain @click="$router.push('/board')">流程看板</el-button>
          <el-button type="danger" plain @click="$router.push('/persons')">人员台账</el-button>
          <el-button type="danger" plain @click="$router.push('/reports')">统计报表</el-button>
          <el-button type="warning" plain @click="$router.push('/poc')">POC 实验室</el-button>
        </div>
        <el-alert
          title="演示数据声明"
          type="warning"
          :closable="false"
          show-icon
          description="当前系统仅使用虚构样例数据；禁止上传真实政审材料或将真实材料发送至模型接口。"
          style="margin-top:18px"
        />
      </div>

      <div class="card">
        <h3>已完成的 POC 验证</h3>
        <el-timeline>
          <el-timeline-item timestamp="POC-1" type="success">模型 JSON 输出与解析（tokenrhythm / deepseek-v4.1-flash-1017）</el-timeline-item>
          <el-timeline-item timestamp="POC-2" type="success">扫描件中文 OCR（Tesseract chi_sim）</el-timeline-item>
          <el-timeline-item timestamp="POC-3" type="success">单份材料核验端到端时延</el-timeline-item>
          <el-timeline-item timestamp="POC-5" type="success">登录、JWT、角色与支部数据范围</el-timeline-item>
          <el-timeline-item timestamp="POC-6" type="success">100 行 Excel 导入与错误行定位</el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { listPersons } from '../api/person'

const auth = useAuthStore()
const stats = reactive({ total: 0 })
const roleMap = { R1: '超级管理员', R2: '支部书记', R3: '党务助理', R4: '学生' }
const roleLabel = computed(() => roleMap[auth.role] || auth.role)

onMounted(async () => {
  try {
    const data = await listPersons({ page: 1, size: 1 })
    stats.total = data.total
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}
h3 {
  margin: 0 0 14px;
  font-size: 16px;
}
.quick {
  display: flex;
  gap: 10px;
}
@media (max-width: 1000px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>