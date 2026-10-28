import { defineStore } from 'pinia'
import { boardApi } from '../api/article'

export const useBoardStore = defineStore('boards', {
  state: () => ({ items: [], loaded: false }),
  getters: {
    lostFound: state => state.items.find(b => b.bizType === 'lostfound'),
    announcement: state => state.items.find(b => b.bizType === 'announcement')
  },
  actions: {
    async load (force = false) {
      if (this.loaded && !force) return this.items
      this.items = await boardApi.available()
      this.loaded = true
      return this.items
    }
  }
})
