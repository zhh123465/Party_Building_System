import request from './request'

export const listAuditLogs = (params) => request.get('/audit-logs', { params })
export const listExportLogs = (params) => request.get('/export-logs', { params })