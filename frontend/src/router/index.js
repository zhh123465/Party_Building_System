import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'persons', name: 'persons', component: () => import('../views/Persons.vue'), meta: { title: '人员台账' } },
      { path: 'persons/:id/materials', name: 'person-materials', component: () => import('../views/PersonMaterials.vue'), meta: { title: '材料核验' } },
      { path: 'board', name: 'board', component: () => import('../views/Board.vue'), meta: { title: '流程看板', roles: ['R1', 'R2', 'R3'] } },
      { path: 'config', name: 'config', component: () => import('../views/ChecklistConfig.vue'), meta: { title: '清单配置', roles: ['R1'] } },
      { path: 'reviews', name: 'reviews', component: () => import('../views/Reviews.vue'), meta: { title: '材料复核', roles: ['R1', 'R2', 'R3'] } },
      { path: 'warnings', name: 'warnings', component: () => import('../views/Warnings.vue'), meta: { title: '时限预警', roles: ['R1', 'R2', 'R3'] } },
      { path: 'schedule', name: 'schedule', component: () => import('../views/Schedule.vue'), meta: { title: '发展日历', roles: ['R1', 'R2', 'R3'] } },
      { path: 'reports', name: 'reports', component: () => import('../views/Reports.vue'), meta: { title: '统计报表', roles: ['R1', 'R2', 'R3'] } },
      { path: 'config/time-rules', name: 'time-rules', component: () => import('../views/TimeRules.vue'), meta: { title: '时限规则', roles: ['R1'] } },
      { path: 'messages', name: 'messages', component: () => import('../views/Messages.vue'), meta: { title: '消息中心' } },
      { path: 'poc', name: 'poc', component: () => import('../views/PocLab.vue'), meta: { title: 'POC 实验室' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = sessionStorage.getItem('pb_token')
  if (to.path !== '/login' && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  const user = JSON.parse(sessionStorage.getItem('pb_user') || 'null')
  if (to.meta.roles && user && !to.meta.roles.includes(user.roleCode)) {
    return '/dashboard'
  }
  return true
})

export default router