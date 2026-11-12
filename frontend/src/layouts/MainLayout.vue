<template>
  <el-container class="layout">
    <div v-if="isMobile && sidebarOpen" class="sidebar-mask" @click="sidebarOpen = false"></div>
    <el-aside width="236px" class="aside" :class="{ 'mobile-open': sidebarOpen }">
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
        <el-menu-item v-if="auth.role !== 'R4'" index="/poc">
          <el-icon><MagicStick /></el-icon><span>POC 实验室</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role !== 'R4'" index="/board">
          <el-icon><DataBoard /></el-icon><span>流程看板</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role !== 'R4'" index="/reviews">
          <el-icon><DocumentChecked /></el-icon><span>材料复核</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role !== 'R4'" index="/warnings">
          <el-icon><AlarmClock /></el-icon><span>时限预警</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role !== 'R4'" index="/schedule">
          <el-icon><Calendar /></el-icon><span>发展日历</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role !== 'R4'" index="/reports">
          <el-icon><DataAnalysis /></el-icon><span>统计报表</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon><span>档案检索</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role === 'R1'" index="/config">
          <el-icon><Setting /></el-icon><span>清单配置</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role === 'R1'" index="/config/time-rules">
          <el-icon><Timer /></el-icon><span>时限规则</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role === 'R1'" index="/audit">
          <el-icon><Document /></el-icon><span>审计日志</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">演示数据均为虚构</div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button v-if="isMobile" link class="menu-btn" @click="sidebarOpen = true">
            <el-icon size="20"><Menu /></el-icon>
          </el-button>
          <span class="header-title">{{ route.meta.title || '工作台' }}</span>
          <el-tag size="small" effect="plain" type="danger">一期 MVP</el-tag>
          <span class="today">{{ today }}</span>
        </div>
        <div class="header-right">
          <el-badge :value="unread" :hidden="unread === 0" class="bell">
            <el-button link @click="$router.push('/messages')">
              <el-icon size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { unreadCount } from '../api/message'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)
const unread = ref(0)
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const isMobile = ref(false)
const sidebarOpen = ref(false)

function updateViewport() {
  isMobile.value = window.innerWidth <= 900
  if (!isMobile.value) sidebarOpen.value = false
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})
onBeforeUnmount(() => window.removeEventListener('resize', updateViewport))

async function refreshUnread() {
  try {
    unread.value = (await unreadCount()).count
  } catch (e) {
    unread.value = 0
  }
}

onMounted(refreshUnread)
watch(() => route.path, () => {
  refreshUnread()
  sidebarOpen.value = false
})
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
  overflow-y: auto;
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
.menu-btn {
  display: none;
  color: #8f1d14;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.today {
  color: #98a2b3;
  font-size: 12px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bell {
  margin-right: 4px;
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
  -webkit-overflow-scrolling: touch;
}
.sidebar-mask {
  position: fixed;
  inset: 0;
  background: rgba(16, 24, 40, 0.45);
  z-index: 1999;
}

@media (max-width: 900px) {
  .aside {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 2000;
    transform: translateX(-100%);
    transition: transform 0.22s ease;
    box-shadow: 12px 0 30px rgba(16, 24, 40, 0.18);
  }
  .aside.mobile-open {
    transform: translateX(0);
  }
  .menu-btn {
    display: inline-flex;
  }
  .today {
    display: none;
  }
  .header {
    padding: 0 10px;
  }
  .header-left .el-tag {
    display: none;
  }
  .header-right .el-tag {
    display: none;
  }
  .main {
    padding: 14px 12px 26px;
  }
}
</style>