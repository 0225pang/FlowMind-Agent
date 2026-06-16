import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: { nickname: 'FlowMind 管理员', role: 'ADMIN' } }),
  actions: {
    login(username: string) {
      localStorage.setItem('flowmind-token', `mock-jwt.${username}`)
    }
  }
})
