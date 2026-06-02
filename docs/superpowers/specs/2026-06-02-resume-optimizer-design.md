# 简历智能优化平台 — 设计文档

**日期:** 2026-06-02
**状态:** 已确认

---

## 1. 项目概述

一个基于 LLM 的简历智能优化工具。用户上传 PDF/Word 简历，系统解析后调用 DeepSeek API 进行综合优化（内容措辞、职位匹配、ATS关键词），展示优化建议，并导出优化后的 PDF 简历。

**当前范围：** 单用户本地运行
**扩展预留：** 架构上不堵死后续多用户扩展

## 2. 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3（Vite 构建） |
| 后端 | Spring Boot 3（Java 17+） |
| AI | DeepSeek API（已配置） |
| PDF 解析 | Apache PDFBox |
| Word 解析 | Apache POI |
| PDF 生成 | iText 或 OpenPDF |

## 3. 整体架构

经典单体应用：Vue SPA → REST API → Spring Boot → DeepSeek API

```
┌─────────────────┐     HTTP/REST      ┌──────────────────┐
│   Vue Frontend  │ ◄──────────────► │  Spring Boot API │
│   (localhost:   │                   │  (localhost:8080) │
│    5173)        │                   │                  │
│                 │                   │  ┌────────────┐  │
│  · 上传简历     │                   │  │ 简历解析    │  │
│  · 查看建议     │                   │  │ PDF/Word→  │  │
│  · 预览/下载   │                   │  │ JSON       │  │
│                 │                   │  └────────────┘  │       ┌──────────┐
│                 │                   │        ↓         │       │ DeepSeek │
│                 │                   │  ┌────────────┐  │ HTTP  │   API    │
│                 │                   │  │ AI 优化    │──┼──────►│ (已有)   │
│                 │                   │  │ 内容/匹配/ │  │       └──────────┘
│                 │                   │  │ ATS        │  │
│                 │                   │  └────────────┘  │
│                 │                   │        ↓         │
│                 │                   │  ┌────────────┐  │
│                 │                   │  │ 简历导出    │  │
│                 │                   │  │ → PDF      │  │
│                 │                   │  └────────────┘  │
└─────────────────┘                   └──────────────────┘
```

**核心流程：** 上传 → 解析为结构化 JSON → 调用 LLM 优化 → 返回建议 + 导出优化版 PDF

## 4. 数据模型

解析后的简历统一用 JSON 表示，这是整个系统的"中间语言"：

```json
{
  "personalInfo": {
    "name": "张三",
    "email": "zhangsan@example.com",
    "phone": "138xxxx",
    "location": "上海"
  },
  "summary": "5年Java开发经验...",
  "workExperience": [
    {
      "company": "某某科技",
      "title": "高级Java工程师",
      "startDate": "2021-03",
      "endDate": "至今",
      "highlights": ["主导了xx系统架构升级", "..."]
    }
  ],
  "education": [
    {
      "school": "某某大学",
      "degree": "本科",
      "major": "计算机科学",
      "graduationYear": "2019"
    }
  ],
  "skills": ["Java", "Spring Boot", "MySQL", "Redis"],
  "language": "zh"
}
```

- 上传 PDF/Word → 解析为上述 JSON
- 手动输入也生成同样的 JSON
- AI 优化和导出都基于这个结构
- `language` 字段决定 LLM 的 prompt 语言和 PDF 生成字体

## 5. API 设计

三个核心接口：

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| POST | `/api/resume/parse` | PDF/Word 文件 (multipart) | 结构化 JSON | 纯解析，无 AI |
| POST | `/api/resume/optimize` | 结构化 JSON | 优化建议 + 优化后 JSON | 调用 LLM |
| POST | `/api/resume/export` | 优化后 JSON | PDF 文件流 | 返回二进制 |

**前端流程：** 上传 → 拿到 JSON → 展示并允许手动编辑 → 点优化 → 展示建议 → 点导出 → 下载 PDF

**API 响应格式：**

```json
// /api/resume/optimize 响应
{
  "suggestions": [
    {
      "section": "summary",
      "original": "...",
      "suggestion": "...",
      "reason": "用词更精炼，突出量化成果"
    }
  ],
  "optimizedResume": { /* 完整优化后 JSON */ }
}
```

## 6. 前端页面

Vue 单页应用，一个主页面串联三步流程：

**Step 1 — 上传简历：**
- 拖拽/点击上传区域，支持 PDF/Word
- "或手动填写" 展开表单入口
- 上传后展示解析进度

**Step 2 — 简历预览 & 编辑：**
- 结构化展示简历各 section
- 每个字段可点击编辑
- "开始优化" 按钮，可选填写目标 JD 用于匹配优化

**Step 3 — 优化结果：**
- 上半部分：优化建议列表，标注修改位置和原因
- 下半部分：优化前后对比视图
- "导出优化后 PDF" 按钮

## 7. LLM Prompt 设计

系统构造结构化 prompt，包含：
- 系统指令：你是一个专业的简历优化顾问
- 当前简历 JSON
- 优化维度：内容措辞、职位匹配、ATS 关键词
- 可选：目标职位描述（JD）

要求 LLM 返回结构化 JSON（优化建议 + 优化后简历 JSON），便于前端展示。

## 8. 错误处理

| 场景 | 处理 |
|------|------|
| 文件格式不支持 | 返回 400，提示仅支持 PDF/Word |
| 文件解析失败（扫描件/图片PDF） | 返回明确错误信息，建议手动输入 |
| LLM API 调用失败 | 重试一次，仍失败则返回 502，提示稍后重试 |
| LLM 返回非预期格式 | 解析失败时返回原始响应，降级展示 |

## 9. 项目结构

```
resume-optimizer/
├── backend/                    # Spring Boot 项目
│   └── src/main/java/...
│       ├── controller/         # REST 控制器
│       ├── service/
│       │   ├── ParserService   # PDF/Word 解析
│       │   ├── OptimizerService # LLM 调用
│       │   └── ExportService   # PDF 生成
│       ├── model/              # 数据模型 (Resume, Suggestion...)
│       └── config/             # CORS, LLM client 配置
├── frontend/                   # Vue 3 项目
│   └── src/
│       ├── components/         # Uploader, ResumeView, SuggestionList...
│       └── App.vue
└── docs/
    └── superpowers/specs/      # 设计文档
```

## 10. 测试策略

- **后端单元测试：** ParserService、OptimizerService 各写核心逻辑测试
- **后端集成测试：** Controller 层 REST API 测试（Mock LLM 调用）
- **前端组件测试：** 关键步骤组件的基本渲染测试
- **暂不覆盖：** 端到端测试、LLM 真实调用测试（本地使用，成本考量）

## 11. 待定事项

- 后续版本考虑：JD 匹配优化（根据职位描述定制简历）
- 后续版本考虑：多用户系统、历史记录
- PDF 生成库：iText 需注意 AGPL 协议，优先选 OpenPDF（Apache 2.0）
