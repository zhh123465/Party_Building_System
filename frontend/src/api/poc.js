import request from './request'

export const pocConfig = () => request.get('/poc/config')
export const pocLlm = (data) => request.post('/poc/llm', data)
export const pocOcr = (formData) => request.post('/poc/ocr', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const pocVerify = (formData) => request.post('/poc/verify', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})