<script setup>
import { ref } from 'vue'

const props = defineProps({
  suggestions: Array,
  optimizedResume: Object
})

const emit = defineEmits(['export'])

const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    const { exportPdf } = await import('../api/resume.js')
    const res = await exportPdf(props.optimizedResume)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'optimized-resume.pdf'
    a.click()
    window.URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="result" v-if="suggestions || optimizedResume">
    <h2>Step 3: 优化结果 — 专业技能提升</h2>

    <!-- 技能标签展示 -->
    <section v-if="optimizedResume?.skills?.length">
      <h3>优化后专业技能</h3>
      <div class="skill-tags">
        <span class="skill-tag" v-for="(skill, i) in optimizedResume.skills" :key="i">{{ skill }}</span>
      </div>
    </section>

    <!-- 优化建议 -->
    <section v-if="suggestions && suggestions.length > 0">
      <h3>优化建议 ({{ suggestions.length }} 条)</h3>
      <div class="suggestion-card" v-for="(s, i) in suggestions" :key="i">
        <div class="s-header">
          <span class="badge">{{ s.section }}</span>
          <span class="reason">{{ s.reason }}</span>
        </div>
        <div class="s-body">
          <div class="s-original">
            <span class="label">优化前</span>
            <p>{{ s.original }}</p>
          </div>
          <div class="s-arrow">→</div>
          <div class="s-new">
            <span class="label">优化后</span>
            <p>{{ s.suggestion }}</p>
          </div>
        </div>
      </div>
    </section>

    <p class="note">基础信息、工作经历、教育背景均保持原样不变</p>

    <button
      class="btn-export"
      :disabled="exporting"
      @click="handleExport"
    >
      {{ exporting ? '生成中...' : '导出优化后 PDF' }}
    </button>
  </div>
</template>

<style scoped>
.result { margin-bottom: 32px; }
h2 { margin-bottom: 16px; font-size: 18px; }
h3 { font-size: 15px; margin-bottom: 12px; color: #555; }
section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.note { color: #888; font-size: 13px; margin: 16px 0; text-align: center; }
.suggestion-card { border: 1px solid #eee; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.s-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.badge { background: #e8f0fe; color: #4a90d9; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.reason { color: #888; font-size: 13px; }
.s-body { display: flex; gap: 12px; align-items: flex-start; }
.s-original, .s-new { flex: 1; }
.s-arrow { font-size: 18px; color: #4a90d9; padding-top: 16px; }
.label { font-size: 11px; color: #999; text-transform: uppercase; }
.s-original p { color: #c62828; font-size: 14px; }
.s-new p { color: #2e7d32; font-size: 14px; }
.skill-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.skill-tag {
  background: linear-gradient(135deg, #e8f0fe, #d4e4fc);
  color: #1a56b8;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}
.btn-export { width: 100%; padding: 14px; background: #2e7d32; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.btn-export:hover { background: #1b5e20; }
.btn-export:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
