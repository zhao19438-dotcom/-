import { defineStore } from 'pinia'
import { userApi } from '../api/user'

// 用户态：登录后保存当前用户信息，供导航与权限判断使用。
export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    loaded: false
  }),
  getters: {
    isLogin: (state) => !!state.userInfo,
    isAdmin: (state) => state.userInfo && state.userInfo.isAdmin === 1
  },
  actions: {
    async load (force = false) {
      if (this.loaded && !force) return this.userInfo
      try {
        this.userInfo = await userApi.info()
      } catch (e) {
        this.userInfo = null
      }
      this.loaded = true
      return this.userInfo
    },
    setUser (user) {
      this.userInfo = user
    },
    clear () {
      this.userInfo = null
      this.loaded = true
    }
  }
})
