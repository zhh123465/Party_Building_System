import request from './request'

export const listWarnings = (params) => request.get('/warnings', { params })
export const warningSummary = (params) => request.get('/warnings/summary', { params })
export const handleWarning = (id, data) => request.put(`/warnings/${id}/handle`, data)
export const generateWarnings = () => request.post('/warnings/generate')
export const calendar = (params) => request.get('/schedule/calendar', { params })
export const reverseSchedule = (data) => request.post('/schedule/reverse', data)
export const listTimeRules = () => request.get('/time-rules')
export const updateTimeRule = (id, data) => request.put(`/time-rules/${id}`, data)