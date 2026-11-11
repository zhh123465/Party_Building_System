import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem('pb_token') || '',
    user: JSON.parse(sessionStorage.getItem('pb_user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state) => state.user?.roleCode || '',
    displayName: (state) => state.user?.realName || state.user?.username || ''
  },
  actions: {
    async login(payload) {
      const data = await authApi.login(payload)
      this.token = data.token
      this.user = data.user
      sessionStorage.setItem('pb_token', data.token)
      sessionStorage.setItem('pb_user', JSON.stringify(data.user))
      return data
    },
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // ignore
      }
      this.token = ''
      this.user = null
      sessionStorage.removeItem('pb_token')
      sessionStorage.removeItem('pb_user')
    }
  }
})