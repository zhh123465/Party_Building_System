import request from './request'

export const reportOverview = (params) => request.get('/reports/overview', { params })
export const stageCount = (params) => request.get('/reports/stage-count', { params })
export const complianceList = (params) => request.get('/reports/compliance', { params })
export const complianceHeatmap = (params) => request.get('/reports/compliance/heatmap', { params })