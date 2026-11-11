import request from './request'

export const pendingReviews = (params) => request.get('/reviews/pending', { params })
export const reviewTemplates = () => request.get('/reviews/templates')
export const submitReview = (materialId, data) => request.post(`/materials/${materialId}/review`, data)
export const reviewLogs = (materialId) => request.get(`/materials/${materialId}/reviews`)