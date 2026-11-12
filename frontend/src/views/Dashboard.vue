<template>
  <div>
    <div class="hero">
      <div>
        <div class="hero-kicker">党员发展全流程智能管控</div>
        <h1>把“靠人记、靠人催、靠人看”<br />变成可锁、可核、可算、可见</h1>
        <p class="hero-desc">规则引擎 + 大模型双引擎核验 · 三锁闭环 · 合规指数 · 电子档案</p>
      </div>
      <div class="hero-badges">
        <div><strong>30s</strong><span>文本类核验</span></div>
        <div><strong>60s</strong><span>扫描件核验</span></div>
        <div><strong>4 级</strong><span>时限预警</span></div>
      </div>
    </div>

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
.hero {
  background: linear-gradient(120deg, #8f1d14 0%, #b42318 58%, #7a1710 100%);
  color: #fff;
  border-radius: 14px;
  padding: 24px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  box-shadow: 0 12px 30px rgba(143, 29, 20, 0.18);
}
.hero-kicker {
  color: #ffd98a;
  font-size: 12px;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.hero h1 {
  font-size: 24px;
  line-height: 1.45;
  margin: 0 0 8px;
  font-weight: 700;
}
.hero-desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}
.hero-badges {
  display: flex;
  gap: 26px;
}
.hero-badges div {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero-badges strong {
  font-size: 26px;
  color: #ffd98a;
}
.hero-badges span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
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
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>