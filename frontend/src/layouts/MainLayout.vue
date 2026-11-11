<template>
  <el-container class="layout">
    <el-aside width="236px" class="aside">
      <div class="brand">
        <div class="brand-mark">党</div>
        <div>
          <div class="brand-title">党员发展智能管理系统</div>
          <div class="brand-sub">流程智能协同 · POC</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#8f1d14" text-color="#f7e8e5" active-text-color="#ffd98a">
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon><span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/persons">
          <el-icon><User /></el-icon><span>人员台账</span>
        </el-menu-item>
        <el-menu-item index="/poc">
          <el-icon><MagicStick /></el-icon><span>POC 实验室</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">演示数据均为虚构</div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="header-title">{{ route.meta.title || '工作台' }}</span>
          <el-tag size="small" effect="plain" type="danger">一期 MVP 骨架</el-tag>
        </div>
        <div class="header-right">
          <el-tag size="small" type="info">{{ roleLabel }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user">
              {{ auth.displayName }} <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)
const roleMap = { R1: '超级管理员', R2: '支部书记', R3: '党务助理', R4: '学生' }
const roleLabel = computed(() => roleMap[auth.role] || auth.role || '未登录')

async function onCommand(command) {
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #8f1d14;
  color: #fff;
  display: flex;
  flex-direction: column;
}
.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 20px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 9px;
  background: #ffd98a;
  color: #8f1d14;
  font-weight: 800;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}
.brand-sub {
  font-size: 11px;
  opacity: 0.7;
  margin-top: 2px;
}
.menu {
  border-right: none;
  flex: 1;
}
.aside-footer {
  padding: 14px 18px;
  font-size: 11px;
  opacity: 0.55;
}
.header {
  background: #fff;
  border-bottom: 1px solid #eaecf0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}
.main {
  padding: 20px 24px 30px;
  overflow: auto;
}
</style>