import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000
})

export function parseResume(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/resume/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function optimizeResume(resumeData) {
  return api.post('/resume/optimize', resumeData)
}

export function exportPdf(resumeData) {
  return api.post('/resume/export', resumeData, {
    responseType: 'blob'
  })
}
