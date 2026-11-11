import request from './request'

export const listMaterials = (personId) => request.get(`/persons/${personId}/materials`)
export const getMaterial = (id) => request.get(`/materials/${id}`)
export const uploadVersion = (id, formData) => request.post(`/materials/${id}/versions`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const verifyMaterial = (id) => request.post(`/materials/${id}/verify`)
export const getTask = (taskId) => request.get(`/verify-tasks/${taskId}`)
export const latestReport = (id) => request.get(`/materials/${id}/reports/latest`)
export const getReport = (reportId) => request.get(`/reports/${reportId}`)
export const downloadUrl = (id, versionId) => `/api/v1/materials/${id}/versions/${versionId}/download`