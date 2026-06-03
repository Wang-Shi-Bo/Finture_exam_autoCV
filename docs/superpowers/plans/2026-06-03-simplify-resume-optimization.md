# Resume Optimization Simplification — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove PDF support, remove summary/projects sections, make LLM only optimize skills to senior level.

**Architecture:** 8 tasks — 5 backend (model cleanup → parser → optimizer → export → tests) then 3 frontend (uploader → editor → result). Each task is self-contained with exact file paths, code, and expected outputs.

**Tech Stack:** Spring Boot 3.2 / Java 17 / Vue 3 / DeepSeek API

---

### Task 1: Clean up data models (Resume.java, PersonalInfo.java)

**Files:**
- Modify: `backend/src/main/java/com/finture/resume/model/Resume.java`
- Modify: `backend/src/main/java/com/finture/resume/model/PersonalInfo.java`

- [ ] **Step 1: Remove `summary` and `projects` fields from Resume.java**

Open `backend/src/main/java/com/finture/resume/model/Resume.java`. Remove the `summary`, `projects` fields and their getters/setters. Also remove the `Project` import.

Replace the entire file with:

```java
package com.finture.resume.model;

import java.util.List;

public class Resume {
    private PersonalInfo personalInfo;
    private List<WorkExperience> workExperience;
    private List<Education> education;
    private List<String> skills;
    private String language;

    public Resume() {}

    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public void setPersonalInfo(PersonalInfo personalInfo) { this.personalInfo = personalInfo; }
    public List<WorkExperience> getWorkExperience() { return workExperience; }
    public void setWorkExperience(List<WorkExperience> workExperience) { this.workExperience = workExperience; }
    public List<Education> getEducation() { return education; }
    public void setEducation(List<Education> education) { this.education = education; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
```

- [ ] **Step 2: Remove `location` field from PersonalInfo.java**

Replace the entire file with:

```java
package com.finture.resume.model;

public class PersonalInfo {
    private String name;
    private String email;
    private String phone;

    public PersonalInfo() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/finture/resume/model/Resume.java backend/src/main/java/com/finture/resume/model/PersonalInfo.java
git commit -m "refactor: remove summary, projects, location fields from models"
```

---

### Task 2: Update ParserService — remove PDF, update validation

**Files:**
- Modify: `backend/src/main/java/com/finture/resume/service/ParserService.java`

- [ ] **Step 1: Replace ParserService.java**

Remove PDF parsing, update file validation to only allow .doc/.docx, update fallback to not set summary/projects.

```java
package com.finture.resume.service;

import com.finture.resume.model.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParserService {

    private final OptimizerService optimizerService;

    public ParserService(OptimizerService optimizerService) {
        this.optimizerService = optimizerService;
    }

    public Resume parse(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String text;
        if (filename.endsWith(".docx") || filename.endsWith(".doc")) {
            text = parseDocx(file);
        } else {
            throw new IllegalArgumentException("仅支持 Word (.doc/.docx) 格式，请上传 Word 文件");
        }

        // Use LLM for structured parsing
        try {
            Resume resume = optimizerService.parseResumeFromText(text);
            // Ensure non-null collections
            if (resume.getWorkExperience() == null) resume.setWorkExperience(new ArrayList<>());
            if (resume.getEducation() == null) resume.setEducation(new ArrayList<>());
            if (resume.getSkills() == null) resume.setSkills(new ArrayList<>());
            if (resume.getPersonalInfo() == null) resume.setPersonalInfo(new PersonalInfo());
            return resume;
        } catch (Exception e) {
            // Fallback: basic regex extraction
            return fallbackParse(text);
        }
    }

    private String parseDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            document.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    /**
     * Fallback: basic regex extraction when LLM is unavailable
     */
    private Resume fallbackParse(String text) {
        Resume resume = new Resume();
        resume.setLanguage(text.matches(".*[\\u4e00-\\u9fff]+.*") ? "zh" : "en");

        PersonalInfo info = new PersonalInfo();
        info.setEmail(extractEmail(text));
        info.setPhone(extractPhone(text));
        resume.setPersonalInfo(info);

        resume.setWorkExperience(new ArrayList<>());
        resume.setEducation(new ArrayList<>());
        resume.setSkills(extractSkills(text));

        return resume;
    }

    private String extractEmail(String text) {
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text);
        return m.find() ? m.group() : "";
    }

    private String extractPhone(String text) {
        Matcher m = Pattern.compile("1[3-9]\\d{9}").matcher(text);
        return m.find() ? m.group() : "";
    }

    private java.util.List<String> extractSkills(String text) {
        String[] commonSkills = {"Java", "Python", "Spring Boot", "MySQL", "Redis",
                "Docker", "Kubernetes", "React", "Vue", "TypeScript", "JavaScript",
                "AWS", "Linux", "Git", "REST API", "微服务"};
        java.util.List<String> found = new ArrayList<>();
        String lowerText = text.toLowerCase();
        for (String skill : commonSkills) {
            if (lowerText.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }
        return found;
    }
}
```

Key changes:
- Removed `parsePdf()` method
- Removed `org.apache.pdfbox.Loader`, `org.apache.pdfbox.pdmodel.PDDocument`, `org.apache.pdfbox.text.PDFTextStripper` imports
- Updated error message to "仅支持 Word (.doc/.docx) 格式"
- `fallbackParse()` no longer sets `summary`

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/finture/resume/service/ParserService.java
git commit -m "refactor: remove PDF parsing from ParserService, only accept .doc/.docx"
```

---

### Task 3: Update OptimizerService — skills-only optimization

**Files:**
- Modify: `backend/src/main/java/com/finture/resume/service/OptimizerService.java`

- [ ] **Step 1: Update the parse prompt to not extract summary/projects**

In `parseResumeFromText()`, replace the prompt string (lines 135-160) with one that omits summary and projects:

```java
public Resume parseResumeFromText(String rawText) {
    String prompt = String.format("""
        请从以下简历文本中提取结构化信息，返回严格的JSON格式（不要包含任何其他文字）。

        提取要求：
        - personalInfo: 姓名(name)、邮箱(email)、电话(phone)
        - workExperience: 工作经历数组，每条包含公司(company)、职位(title)、开始日期(startDate)、结束日期(endDate)、亮点(highlights数组)
        - education: 教育经历数组，每条包含学校(school)、学位(degree)、专业(major)、毕业年份(graduationYear)
        - skills: 技能数组
        - language: "zh" 或 "en"

        JSON格式示例：
        {
          "personalInfo": {"name": "姓名", "email": "xxx@xxx.com", "phone": "138xxxx"},
          "workExperience": [{"company": "公司名", "title": "职位", "startDate": "2020-01", "endDate": "2022-06", "highlights": ["亮点1", "亮点2"]}],
          "education": [{"school": "大学名", "degree": "本科", "major": "专业名", "graduationYear": "2018"}],
          "skills": ["Java", "Spring Boot"],
          "language": "zh"
        }

        简历原文：
        %s
        """, rawText);

    // ... rest of method unchanged
```

- [ ] **Step 2: Update the system prompt for optimization**

Replace `getSystemPrompt()` (lines 90-95):

```java
private String getSystemPrompt(String language) {
    if ("zh".equals(language)) {
        return "你是一个高级技术面试官和简历顾问。你的任务是仅优化简历中的"专业技能(skills)"部分，使其达到高级/资深开发工程师水平。基础信息、工作经历、教育背景必须原样保留，一个字都不许改。";
    }
    return "You are a senior technical interviewer and resume consultant. Your task is to ONLY optimize the 'skills' section to senior/lead developer level. Personal info, work experience, and education MUST be preserved verbatim — do not modify a single character.";
}
```

- [ ] **Step 3: Replace the optimization prompt (buildPrompt method)**

Replace `buildPrompt()` (lines 97-129):

```java
private String buildPrompt(String resumeJson, String language) {
    String lang = "zh".equals(language) ? "中文" : "English";
    return String.format("""
        请仅优化以下%s简历中的"专业技能(skills)"部分。

        优化要求：
        1. 补充该技术栈资深工程师通常具备但当前遗漏的关键技能
        2. 将笼统的技能描述细化为具体的技术栈（如"数据库" → "MySQL、PostgreSQL、MongoDB"）
        3. 按技能重要性排序，最核心的技能放在前面
        4. 技能数量控制在 8-15 个，宁缺毋滥

        ⚠️ 重要约束：
        - personalInfo（基础信息）必须原样返回，不得修改任何字段
        - workExperience（工作经历）必须原样返回，不得修改任何字段
        - education（教育背景）必须原样返回，不得修改任何字段
        - 只允许修改 skills 数组

        当前简历JSON：
        %s

        请返回如下JSON格式（不要包含任何其他文字）：
        {
          "suggestions": [
            {
              "section": "skills",
              "original": "原始技能列表",
              "suggestion": "优化后的技能列表（逗号分隔）",
              "reason": "优化原因"
            }
          ],
          "optimizedResume": {
            "personalInfo": {"name": "原样", "email": "原样", "phone": "原样"},
            "workExperience": [{"company": "原样", "title": "原样", "startDate": "原样", "endDate": "原样", "highlights": ["原样"]}],
            "education": [{"school": "原样", "degree": "原样", "major": "原样", "graduationYear": "原样"}],
            "skills": ["优化后的技能1", "优化后的技能2"],
            "language": "zh"
          }
        }
        """, lang, resumeJson);
}
```

- [ ] **Step 4: Add defensive safeguard in the optimize() method**

Right after `parseOptimizeResponse(content)` succeeds (line 72), add a safeguard that ensures non-skills fields are preserved from the original resume. Insert after the return of `parseOptimizeResponse(content)`:

In the `optimize()` method, replace:
```java
            return parseOptimizeResponse(content);
        } catch (Exception e) {
```

With:
```java
            OptimizeResponse result = parseOptimizeResponse(content);
            // Defensive: preserve original non-skills fields in case LLM modified them
            if (result.getOptimizedResume() != null) {
                result.getOptimizedResume().setPersonalInfo(resume.getPersonalInfo());
                result.getOptimizedResume().setWorkExperience(resume.getWorkExperience());
                result.getOptimizedResume().setEducation(resume.getEducation());
            }
            return result;
        } catch (Exception e) {
```

Do the same for the retry block (line 82-83):
```java
                OptimizeResponse retryResult = parseOptimizeResponse(retryContent);
                if (retryResult.getOptimizedResume() != null) {
                    retryResult.getOptimizedResume().setPersonalInfo(resume.getPersonalInfo());
                    retryResult.getOptimizedResume().setWorkExperience(resume.getWorkExperience());
                    retryResult.getOptimizedResume().setEducation(resume.getEducation());
                }
                return retryResult;
```

- [ ] **Step 5: Remove summary-related code in fallback of parseOptimizeResponse**

In `parseOptimizeResponse()` (lines 199-216), the fallback sets `fallbackResume.setSummary(content)`. Since `Resume` no longer has `summary`, update the fallback:

```java
    private OptimizeResponse parseOptimizeResponse(String content) {
        try {
            String jsonStr = content.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("^```(?:json)?\\s*", "");
                jsonStr = jsonStr.replaceAll("\\s*```$", "");
            }
            return objectMapper.readValue(jsonStr, OptimizeResponse.class);
        } catch (JsonProcessingException e) {
            // 降级：返回原始内容
            OptimizeResponse fallback = new OptimizeResponse();
            fallback.setSuggestions(List.of());
            Resume fallbackResume = new Resume();
            fallbackResume.setSkills(List.of("解析失败，请重试"));
            fallback.setOptimizedResume(fallbackResume);
            return fallback;
        }
    }
```

- [ ] **Step 6: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/finture/resume/service/OptimizerService.java
git commit -m "feat: skills-only optimization with defensive preservation of non-skills fields"
```

---

### Task 4: Update ExportService — remove summary and projects output

**Files:**
- Modify: `backend/src/main/java/com/finture/resume/service/ExportService.java`

- [ ] **Step 1: Remove summary and projects export logic**

Remove the "Summary" block (lines 51-55), the "项目经历" block (lines 73-92), and the `Project` import (line 5).

Replace the file:

```java
package com.finture.resume.service;

import com.finture.resume.model.Education;
import com.finture.resume.model.Resume;
import com.finture.resume.model.WorkExperience;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ExportService {

    public byte[] exportToPdf(Resume resume) throws IOException, DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        // 使用系统自带中文字体
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font sectionFont = new Font(baseFont, 14, Font.BOLD);
        Font normalFont = new Font(baseFont, 11, Font.NORMAL);

        // 个人信息
        if (resume.getPersonalInfo() != null) {
            var info = resume.getPersonalInfo();
            if (info.getName() != null && !info.getName().isEmpty()) {
                document.add(new Paragraph(info.getName(), titleFont));
            }
            StringBuilder contact = new StringBuilder();
            if (info.getEmail() != null) contact.append(info.getEmail()).append(" | ");
            if (info.getPhone() != null) contact.append(info.getPhone());
            if (contact.length() > 0) {
                document.add(new Paragraph(contact.toString(), normalFont));
            }
            document.add(new Paragraph(" "));
        }

        // 工作经历
        if (resume.getWorkExperience() != null && !resume.getWorkExperience().isEmpty()) {
            document.add(new Paragraph("工作经历", sectionFont));
            for (WorkExperience we : resume.getWorkExperience()) {
                document.add(new Paragraph(
                    we.getTitle() + " | " + we.getCompany() +
                    " (" + we.getStartDate() + " - " + we.getEndDate() + ")", normalFont));
                if (we.getHighlights() != null) {
                    for (String h : we.getHighlights()) {
                        document.add(new Paragraph("  • " + h, normalFont));
                    }
                }
            }
            document.add(new Paragraph(" "));
        }

        // 教育
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
            document.add(new Paragraph("教育背景", sectionFont));
            for (Education edu : resume.getEducation()) {
                document.add(new Paragraph(
                    edu.getSchool() + " | " + edu.getDegree() + " - " + edu.getMajor() +
                    " (" + edu.getGraduationYear() + ")", normalFont));
            }
            document.add(new Paragraph(" "));
        }

        // 技能
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            document.add(new Paragraph("专业技能", sectionFont));
            document.add(new Paragraph(String.join(", ", resume.getSkills()), normalFont));
        }

        document.close();
        return out.toByteArray();
    }
}
```

Key changes:
- Removed `import com.finture.resume.model.Project;`
- Removed "个人总结" (summary) section
- Removed "项目经历" (projects) section
- Renamed "技能" section header to "专业技能"

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/finture/resume/service/ExportService.java
git commit -m "refactor: remove summary and projects from PDF export"
```

---

### Task 5: Update backend tests

**Files:**
- Modify: `backend/src/test/java/com/finture/resume/service/ParserServiceTest.java`
- Modify: `backend/src/test/java/com/finture/resume/service/OptimizerServiceTest.java`
- Modify: `backend/src/test/java/com/finture/resume/controller/ResumeControllerTest.java`

- [ ] **Step 1: Update ParserServiceTest — remove summary reference**

In `parseDocx_shouldReturnResumeFromLLM()`, remove line 45 (`mockResume.setSummary("A Java developer");`).
In `parseDocx_shouldReturnResumeFromLLM()`, add `mockResume.setEducation(new ArrayList<>());` if not present (already there).

```java
    @Test
    void parseDocx_shouldReturnResumeFromLLM() throws Exception {
        // Mock LLM response
        Resume mockResume = new Resume();
        PersonalInfo info = new PersonalInfo();
        info.setName("Test User");
        info.setEmail("test@example.com");
        info.setPhone("13812345678");
        mockResume.setPersonalInfo(info);
        mockResume.setWorkExperience(new ArrayList<>());
        mockResume.setEducation(new ArrayList<>());
        mockResume.setSkills(List.of("Java", "Spring Boot"));
        mockResume.setLanguage("en");

        when(mockOptimizer.parseResumeFromText(anyString())).thenReturn(mockResume);

        byte[] docxBytes = createMinimalDocx();
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxBytes
        );
        var resume = parserService.parse(file);
        assertNotNull(resume);
        assertEquals("Test User", resume.getPersonalInfo().getName());
        assertEquals("test@example.com", resume.getPersonalInfo().getEmail());
        assertEquals("13812345678", resume.getPersonalInfo().getPhone());
    }
```

Also update the error message check — but the test `parseUnsupportedFormat_shouldThrow` doesn't check the message, just that it throws. No change needed.

- [ ] **Step 2: Update OptimizerServiceTest — remove summary reference**

In `optimize_shouldThrowOnInvalidApiKey()`, remove the line `resume.setSummary("A test resume");`.

```java
    @Test
    void optimize_shouldThrowOnInvalidApiKey() {
        OptimizerService service = new OptimizerService();
        Resume resume = new Resume();
        resume.setPersonalInfo(new PersonalInfo());
        resume.getPersonalInfo().setName("Test");
        resume.setWorkExperience(new ArrayList<>());
        resume.setEducation(new ArrayList<>());
        resume.setSkills(new ArrayList<>());
        resume.setLanguage("en");

        // Without a valid API key, this should throw RuntimeException
        assertThrows(RuntimeException.class, () -> service.optimize(resume));
    }
```

- [ ] **Step 3: Update ResumeControllerTest — change PDF references to .docx**

In `parse_shouldReturnResumeJson()`, change `"resume.pdf"` and `"application/pdf"` to `.docx`:

```java
    @Test
    void parse_shouldReturnResumeJson() throws Exception {
        Resume mockResume = new Resume();
        mockResume.setLanguage("zh");
        when(parserService.parse(any())).thenReturn(mockResume);

        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test".getBytes()
        );

        mockMvc.perform(multipart("/api/resume/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("zh"));
    }
```

In `parse_serverError_shouldReturn422()`, also change:

```java
    @Test
    void parse_serverError_shouldReturn422() throws Exception {
        when(parserService.parse(any()))
            .thenThrow(new java.io.IOException("Parse error"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test".getBytes()
        );

        mockMvc.perform(multipart("/api/resume/parse").file(file))
            .andExpect(status().is(422));
    }
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test`
Expected: All tests pass (BUILD SUCCESS, 8 tests)

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/
git commit -m "test: update tests for removed summary/projects and docx-only parsing"
```

---

### Task 6: Update frontend — ResumeUploader.vue

**Files:**
- Modify: `frontend/src/components/ResumeUploader.vue`

- [ ] **Step 1: Update file validation and UI text**

Replace the file validation logic and UI hints. Only accept .doc/.docx:

```vue
<script setup>
import { ref } from 'vue'

const emit = defineEmits(['parsed'])

const loading = ref(false)
const error = ref('')
const dragOver = ref(false)

async function handleFile(inputFile) {
  error.value = ''
  const validTypes = [
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword'
  ]
  if (!validTypes.includes(inputFile.type) &&
      !inputFile.name.endsWith('.docx') &&
      !inputFile.name.endsWith('.doc')) {
    error.value = '仅支持 Word (.docx/.doc) 文件'
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
        <p class="hint">仅支持 Word (.docx, .doc) 格式</p>
      </div>
      <input type="file" accept=".doc,.docx" @change="onFileChange" />
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
```

Key changes:
- Removed `'application/pdf'` from validTypes
- Updated error message to "仅支持 Word (.docx/.doc) 文件"
- Updated hint text to "仅支持 Word (.docx, .doc) 格式"
- Updated accept attribute to `.doc,.docx` (removed `.pdf`)

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ResumeUploader.vue
git commit -m "feat: restrict upload to .doc/.docx only, remove PDF support"
```

---

### Task 7: Update frontend — ResumeEditor.vue

**Files:**
- Modify: `frontend/src/components/ResumeEditor.vue`

- [ ] **Step 1: Remove "个人总结" and "项目经历" sections**

Remove the summary section (lines 35-43) and projects section (lines 65-87).

```vue
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
```

Key changes:
- Removed "个人总结" section entirely
- Removed "项目经历" section entirely
- Renamed "个人信息" → "基础信息"
- Renamed "技能" → "专业技能"

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ResumeEditor.vue
git commit -m "refactor: remove summary and projects from editor, keep only basic info + work + education + skills"
```

---

### Task 8: Update frontend — OptimizationResult.vue

**Files:**
- Modify: `frontend/src/components/OptimizationResult.vue`

- [ ] **Step 1: Simplify the result display to focus on skills**

Replace the file:

```vue
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

    <!-- 技能变化 -->
    <section v-if="optimizedResume">
      <h3>专业技能</h3>
      <div class="skills-compare">
        <div class="skill-box original">
          <span class="label">优化前</span>
          <div class="skill-tags">
            <span v-for="(s, i) in optimizedResume.skills" :key="'orig-'+i" class="tag orig-tag">{{ s }}</span>
          </div>
        </div>
      </div>
      <p class="note">基础信息、工作经历、教育背景均保持原样不变</p>
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
.note { color: #888; font-size: 13px; margin-top: 12px; text-align: center; }
.skills-compare { display: flex; gap: 16px; }
.skill-box { flex: 1; }
.skill-box .label { font-size: 12px; color: #999; text-transform: uppercase; display: block; margin-bottom: 8px; }
.skill-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { padding: 4px 10px; border-radius: 4px; font-size: 13px; }
.orig-tag { background: #e8f0fe; color: #4a90d9; }
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
.btn-export { width: 100%; padding: 14px; background: #2e7d32; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.btn-export:hover { background: #1b5e20; }
.btn-export:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
```

Key changes:
- Updated title to "优化结果 — 专业技能提升"
- Removed before/after comparison of full resume — now only shows skills optimization
- Added note that non-skills fields are unchanged
- Kept the suggestions list for detailed before/after of skills

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/OptimizationResult.vue
git commit -m "refactor: simplify result view to focus on skills optimization"
```

---

### Final verification

- [ ] **Run full backend tests**

```bash
cd backend && mvn test
```
Expected: 8 tests pass, BUILD SUCCESS

- [ ] **Verify frontend builds**

```bash
cd frontend && npm run build
```
Expected: Build succeeds without errors

- [ ] **Final commit (if any fixups needed)**

```bash
git add -A && git commit -m "chore: final cleanup after skills-only optimization refactor"
```
