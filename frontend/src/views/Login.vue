<template>
  <div class="login-page">
    <div class="login-left">
      <div class="badge">大创项目 · 一期 MVP</div>
      <h1>党员发展<br />智能管理系统</h1>
      <p class="sub">规则引擎 + 大模型双引擎核验<br />流程可锁 · 材料可核 · 时限可算 · 风险可见</p>
      <div class="features">
        <div><el-icon><Lock /></el-icon> 三锁闭环</div>
        <div><el-icon><DocumentChecked /></el-icon> 材料智能核验</div>
        <div><el-icon><DataAnalysis /></el-icon> 合规指数</div>
      </div>
      <div class="notice">演示环境，全部使用虚构数据；请勿上传真实政审材料。</div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <h2>账号登录</h2>
        <p class="hint">使用学号 / 工号登录</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" :prefix-icon="Key" />
          </el-form-item>
          <el-button type="danger" size="large" class="submit" :loading="loading" @click="submit">登 录</el-button>
        </el-form>

        <el-divider>演示账号（密码均为 Party@2026）</el-divider>
        <div class="accounts">
          <el-tag v-for="a in accounts" :key="a.username" class="account" @click="fill(a.username)">
            {{ a.label }} / {{ a.username }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Key } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Party@2026' })
const accounts = [
  { label: '管理员', username: 'admin' },
  { label: '支部书记', username: 'secretary' },
  { label: '党务助理', username: 'assistant' },
  { label: '学生', username: 'student' }
]
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(username) {
  form.username = username
  form.password = 'Party@2026'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  background: #fff;
}
.login-left {
  background: linear-gradient(145deg, #8f1d14 0%, #b42318 55%, #7a1710 100%);
  color: #fff;
  padding: 64px 70px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-left::after {
  content: "";
  position: absolute;
  width: 420px;
  height: 420px;
  border: 1px solid rgba(255, 217, 138, 0.25);
  border-radius: 50%;
  right: -160px;
  bottom: -160px;
}
.badge {
  display: inline-block;
  width: fit-content;
  border: 1px solid rgba(255, 217, 138, 0.5);
  color: #ffd98a;
  border-radius: 20px;
  padding: 4px 12px;
  font-size: 12px;
  margin-bottom: 26px;
}
h1 {
  font-size: 46px;
  line-height: 1.18;
  margin: 0 0 20px;
  letter-spacing: 2px;
}
.sub {
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.9;
  font-size: 15px;
}
.features {
  display: flex;
  gap: 22px;
  margin-top: 34px;
  font-size: 14px;
  color: #ffd98a;
}
.features div {
  display: flex;
  gap: 6px;
  align-items: center;
}
.notice {
  margin-top: 46px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}
.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
}
.login-card {
  width: 380px;
}
h2 {
  margin: 0 0 6px;
  font-size: 26px;
}
.hint {
  color: #667085;
  font-size: 13px;
  margin: 0 0 26px;
}
.submit {
  width: 100%;
  margin-top: 6px;
}
.accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.account {
  cursor: pointer;
}
@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .login-left {
    display: none;
  }
  .login-right {
    padding: 18px;
  }
  .login-card {
    width: 100%;
    max-width: 420px;
  }
}
@media (max-width: 480px) {
  .login-right {
    padding: 14px;
    align-items: flex-start;
    padding-top: 8vh;
  }
  h2 {
    font-size: 22px;
  }
  .accounts {
    gap: 6px;
  }
}
</style>