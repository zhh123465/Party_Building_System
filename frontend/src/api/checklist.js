import request from './request'

export const listMaterialTypes = () => request.get('/material-types')
export const createMaterialType = (data) => request.post('/material-types', data)
export const updateMaterialType = (id, data) => request.put(`/material-types/${id}`, data)

export const listTemplates = () => request.get('/checklists/templates')
export const getTemplate = (id) => request.get(`/checklists/templates/${id}`)
export const createTemplate = (data) => request.post('/checklists/templates', data)
export const updateTemplate = (id, data) => request.put(`/checklists/templates/${id}`, data)
export const cloneTemplate = (id) => request.post(`/checklists/templates/${id}/clone`)
export const validateTemplate = (id) => request.post(`/checklists/templates/${id}/validate`)
export const publishTemplate = (id) => request.post(`/checklists/templates/${id}/publish`)

export const addTemplateItem = (templateId, data) => request.post(`/checklists/templates/${templateId}/items`, data)
export const updateTemplateItem = (itemId, data) => request.put(`/checklists/items/${itemId}`, data)
export const deleteTemplateItem = (itemId) => request.delete(`/checklists/items/${itemId}`)