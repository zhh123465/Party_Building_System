import request from './request'

export const listPersons = (params) => request.get('/persons', { params })
export const getPerson = (id) => request.get(`/persons/${id}`)
export const getIdCard = (id) => request.get(`/persons/${id}/id-card`)
export const importPersons = (formData) => request.post('/persons/import', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})