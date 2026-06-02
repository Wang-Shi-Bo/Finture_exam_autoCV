<script setup>
import { ref } from 'vue'

const emit = defineEmits(['parsed'])

const loading = ref(false)
const error = ref('')
const dragOver = ref(false)

async function handleFile(inputFile) {
  error.value = ''
  const validTypes = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword'
  ]
  if (!validTypes.includes(inputFile.type) &&
      !inputFile.name.endsWith('.pdf') &&
      !inputFile.name.endsWith('.docx') &&
      !inputFile.name.endsWith('.doc')) {
    error.value = '仅支持 PDF 和 Word (.docx/.doc) 文件'
    return
  }
  loading.value = true
  try {
    const { parseResume } = await import('../api/resume.js')
    const res = await parseResume(inputFile)
    emit('parsed', res.data)
  } catch (e) {
    error.value = e.response?.data?.error || '解析失败，请检查文件是否有效'
  } finally {
    loading.value = false
  }
}

function onDrop(e) {
  dragOver.value = false
  if (e.dataTransfer.files.length) handleFile(e.dataTransfer.files[0])
}

function onFileChange(e) {
  if (e.target.files.length) handleFile(e.target.files[0])
}
</script>

<template>
  <div class="uploader">
    <h2>Step 1: 上传简历</h2>
    <div
      class="drop-zone"
      :class="{ 'drag-over': dragOver, loading }"
      @dragover.prevent="dragOver = true"
      @dragleave="dragOver = false"
      @drop.prevent="onDrop"
    >
      <div v-if="loading" class="loading-text">解析中...</div>
      <div v-else>
        <p>拖拽简历文件到此处，或点击选择</p>
        <p class="hint">支持 PDF / Word (.docx, .doc)</p>
      </div>
      <input type="file" accept=".pdf,.doc,.docx" @change="onFileChange" />
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.uploader { margin-bottom: 32px; }
h2 { margin-bottom: 12px; font-size: 18px; }
.drop-zone {
  border: 2px dashed #ccc;
  border-radius: 12px;
  padding: 48px;
  text-align: center;
  position: relative;
  cursor: pointer;
  background: #fff;
  transition: border-color 0.2s;
}
.drop-zone.drag-over { border-color: #4a90d9; background: #f0f6ff; }
.drop-zone.loading { opacity: 0.6; pointer-events: none; }
.drop-zone input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.hint { color: #999; font-size: 13px; margin-top: 8px; }
.error { color: #d32f2f; margin-top: 8px; font-size: 14px; }
</style>
