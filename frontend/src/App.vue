<script setup>
import { ref } from 'vue'
import ResumeUploader from './components/ResumeUploader.vue'
import ResumeEditor from './components/ResumeEditor.vue'
import OptimizationResult from './components/OptimizationResult.vue'
import { optimizeResume } from './api/resume.js'

const step = ref(1) // 1: upload, 2: edit, 3: result
const resume = ref(null)
const suggestions = ref([])
const optimizedResume = ref(null)
const optimizing = ref(false)
const optimizeError = ref('')

const fileId = ref(null)
function onParsed(data) {
  resume.value = data.resume
  fileId.value = data.fileId
  step.value = 2
}

function onUpdateResume(updated) {
  resume.value = updated
}

async function onOptimize() {
  optimizing.value = true
  optimizeError.value = ''
  try {
    const res = await optimizeResume(resume.value)
    suggestions.value = res.data.suggestions || []
    optimizedResume.value = res.data.optimizedResume
    step.value = 3
  } catch (e) {
    optimizeError.value = e.response?.data?.error || '优化失败，请稍后重试'
  } finally {
    optimizing.value = false
  }
}

function onExport() {
  // handled inside OptimizationResult
}

function reset() {
  step.value = 1
  resume.value = null
  suggestions.value = []
  optimizedResume.value = null
}
</script>

<template>
  <div class="app">
    <header>
      <h1>简历智能优化</h1>
      <button v-if="step > 1" class="btn-back" @click="reset">重新开始</button>
    </header>

    <div class="steps">
      <span :class="{ active: step === 1 }">1. 上传</span>
      <span class="sep">→</span>
      <span :class="{ active: step === 2 }">2. 编辑</span>
      <span class="sep">→</span>
      <span :class="{ active: step === 3 }">3. 优化</span>
    </div>

    <ResumeUploader v-if="step === 1" @parsed="onParsed" />

    <ResumeEditor
      v-if="step === 2"
      :resume="resume"
      @update:resume="onUpdateResume"
      @optimize="onOptimize"
    />
    <p v-if="optimizeError" class="error">{{ optimizeError }}</p>
    <p v-if="optimizing" class="optimizing">AI 正在优化中...</p>

    <OptimizationResult
      v-if="step === 3"
      :fileId="fileId"
      :suggestions="suggestions"
      :optimizedResume="optimizedResume"
      @export="onExport"
    />
  </div>
</template>

<style scoped>
.app { max-width: 800px; margin: 0 auto; }
header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
header h1 { font-size: 24px; }
.btn-back { font-size: 13px; padding: 6px 14px; background: #eee; border: none; border-radius: 6px; cursor: pointer; }
.steps { display: flex; gap: 12px; align-items: center; margin-bottom: 24px; font-size: 14px; color: #bbb; }
.steps .active { color: #4a90d9; font-weight: bold; }
.steps .sep { color: #ddd; }
.error { color: #d32f2f; margin: 8px 0; font-size: 14px; }
.optimizing { color: #4a90d9; margin: 8px 0; font-size: 14px; }
</style>
