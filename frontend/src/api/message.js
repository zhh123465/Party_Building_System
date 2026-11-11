import request from './request'

export const listMessages = (params) => request.get('/messages', { params })
export const unreadCount = () => request.get('/messages/unread-count')
export const markRead = (id) => request.put(`/messages/${id}/read`)
export const markAllRead = () => request.put('/messages/read-all')