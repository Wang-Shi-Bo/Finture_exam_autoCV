<script setup>
import { ref } from 'vue'

const props = defineProps({ resume: Object })
const emit = defineEmits(['optimize', 'update:resume'])

const editMode = ref({})

function toggleEdit(key) {
  editMode.value[key] = !editMode.value[key]
}

function emitUpdate() {
  emit('update:resume', { ...props.resume })
}
</script>

<template>
  <div class="editor" v-if="resume">
    <h2>Step 2: 简历预览 & 编辑</h2>

    <!-- 个人信息 -->
    <section>
      <h3>基础信息</h3>
      <div class="field" v-if="resume.personalInfo">
        <label>姓名</label>
        <input v-model="resume.personalInfo.name" @input="emitUpdate" />
        <label>邮箱</label>
        <input v-model="resume.personalInfo.email" @input="emitUpdate" />
        <label>电话</label>
        <input v-model="resume.personalInfo.phone" @input="emitUpdate" />
      </div>
    </section>

    <!-- 工作经历 -->
    <section>
      <h3>工作经历</h3>
      <div v-for="(exp, i) in resume.workExperience" :key="i" class="card">
        <input v-model="exp.company" placeholder="公司" @input="emitUpdate" />
        <input v-model="exp.title" placeholder="职位" @input="emitUpdate" />
        <input v-model="exp.startDate" placeholder="开始日期" @input="emitUpdate" />
        <input v-model="exp.endDate" placeholder="结束日期" @input="emitUpdate" />
        <textarea
          :value="exp.highlights?.join('\n')"
          @input="exp.highlights = $event.target.value.split('\n'); emitUpdate()"
          placeholder="工作亮点（每行一个）"
          rows="3"
        ></textarea>
      </div>
      <button class="btn-sm" @click="resume.workExperience.push({company:'',title:'',startDate:'',endDate:'',highlights:[]}); emitUpdate()">
        + 添加经历
      </button>
    </section>

    <!-- 教育 -->
    <section>
      <h3>教育背景</h3>
      <div v-for="(edu, i) in resume.education" :key="i" class="card">
        <input v-model="edu.school" placeholder="学校" @input="emitUpdate" />
        <input v-model="edu.degree" placeholder="学位" @input="emitUpdate" />
        <input v-model="edu.major" placeholder="专业" @input="emitUpdate" />
        <input v-model="edu.graduationYear" placeholder="毕业年份" @input="emitUpdate" />
      </div>
      <button class="btn-sm" @click="resume.education.push({school:'',degree:'',major:'',graduationYear:''}); emitUpdate()">
        + 添加教育
      </button>
    </section>

    <!-- 技能 -->
    <section>
      <h3>专业技能</h3>
      <input
        :value="resume.skills?.join(', ')"
        @input="resume.skills = $event.target.value.split(',').map(s => s.trim()); emitUpdate()"
        placeholder="技能（逗号分隔）"
      />
    </section>

    <button class="btn-primary" @click="$emit('optimize', resume)">
      开始优化
    </button>
  </div>
</template>

<style scoped>
.editor { margin-bottom: 32px; }
h2 { margin-bottom: 16px; font-size: 18px; }
h3 { font-size: 15px; margin: 16px 0 8px; color: #555; }
section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.field { display: grid; grid-template-columns: 80px 1fr; gap: 8px; align-items: center; }
label { font-size: 13px; color: #888; }
input, textarea { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
input:focus, textarea:focus { border-color: #4a90d9; outline: none; }
.card { border: 1px solid #eee; border-radius: 8px; padding: 12px; margin-bottom: 8px; display: flex; flex-direction: column; gap: 8px; }
.btn-sm { font-size: 13px; padding: 6px 12px; background: #f0f0f0; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; }
.btn-primary { margin-top: 16px; width: 100%; padding: 14px; background: #4a90d9; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.btn-primary:hover { background: #357abd; }
</style>
