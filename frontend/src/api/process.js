import request from './request'

export const board = (params) => request.get('/process/board', { params })
export const overview = (params) => request.get('/process/overview', { params })
export const stageDistribution = (params) => request.get('/process/stage-distribution', { params })
export const advanceCheck = (personId) => request.get(`/process/persons/${personId}/advance-check`)
export const advance = (personId, data) => request.post(`/process/persons/${personId}/advance`, data)
export const rollback = (personId, data) => request.post(`/process/persons/${personId}/rollback`, data)