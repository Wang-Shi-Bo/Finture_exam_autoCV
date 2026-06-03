# Design: 简历优化简化改造

Date: 2026-06-03

## 需求

1. **上传限制**: 仅接受 `.doc` / `.docx`，移除 PDF
2. **内容结构**: 基础信息 + 工作经历 + 教育背景 + 专业技能（移除个人总结、项目经历）
3. **优化范围**: 仅优化「专业技能」至高级开发水平，其他字段原样不动
4. **导出格式**: 保持 PDF 导出，排版不变

## 改动清单

### 后端

| 文件 | 改动 |
|------|------|
| `Resume.java` | 移除 `summary`、`projects` 字段 |
| `PersonalInfo.java` | 移除 `location` 字段 |
| `ParserService.java` | 移除 PDF 解析方法 `parsePdf()`、移除 PDF 依赖导入、文件校验仅允许 doc/docx、更新 fallback |
| `OptimizerService.java` | 解析 prompt 不再提取 summary/projects；优化 prompt 改为仅优化 skills，其余字段原样返回 |
| `ExportService.java` | 移除 summary 和 projects 的输出段落 |

### 前端

| 文件 | 改动 |
|------|------|
| `ResumeUploader.vue` | 文件接受类型改为仅 .doc/.docx，更新提示文案 |
| `ResumeEditor.vue` | 移除「个人总结」「项目经历」区块 |
| `OptimizationResult.vue` | 调整结果展示，突出技能优化 |

### 不影响

- `Education.java`、`WorkExperience.java`、`Suggestion.java` 模型不变
- `ExportService.java` 中个人信息/工作/教育/技能排版不变
- API 端点不变 (`/parse`, `/optimize`, `/export`)
- Controller 不变

## 优化 Prompt 策略

```
你是一个高级技术面试官。请仅优化简历中的"专业技能(skills)"部分，使其达到高级/资深开发工程师水平：
1. 补充该技术栈资深工程师通常具备但当前遗漏的技能
2. 将笼统的技能描述细化为具体的技术栈（如"数据库"→"MySQL、PostgreSQL、MongoDB"）
3. 按技能类别分组（语言、框架、基础设施、工具等）

基础信息、工作经历、教育背景必须原样保留，不得修改。
```

## 风险

- LLM 可能仍然修改非 skills 字段 → prompt 需强调"原样保留" + 代码层做防御性校验
- 移除 PDF 后旧用户可能不适应 → 上传页面明确提示仅支持 Word
