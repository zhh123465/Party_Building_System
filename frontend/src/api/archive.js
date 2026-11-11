import request from './request'

export const archiveTree = (personId) => request.get(`/persons/${personId}/archive/tree`)
export const archiveStage = (personId, stage) => request.post(`/persons/${personId}/archive`, null, { params: { stage } })
export const archives = (personId) => request.get(`/persons/${personId}/archives`)
export const searchMaterials = (params) => request.get('/materials/search', { params })
export const archiveDownloadUrl = (id) => `/api/v1/archives/${id}/download`