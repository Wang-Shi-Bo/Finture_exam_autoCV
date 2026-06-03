# 简历智能优化平台

AI 驱动的简历优化工具 — 上传 Word 简历，自动解析并优化专业技能描述，提升简历竞争力。

## 解决什么问题

投简历时最常见的痛点：**技能部分只罗列了技术名词**（如 "Java、MySQL、Redis"），缺乏深度描述，无法体现真实技术水平。

这个工具做的事情：

1. **解析简历** — 上传 Word 文件（.doc / .docx），自动提取结构化信息
2. **智能优化** — 调用 DeepSeek AI，将每个技能关键词展开为高级/资深工程师水平的专业描述
3. **结果展示** — 对比优化前后的技能描述，一目了然

**优化示例：**

| 优化前 | 优化后 |
|--------|--------|
| `Java` | 精通 Java 集合框架、并发编程及 JVM 内存模型与性能调优 |
| `MySQL` | 深入理解 MySQL 索引原理、锁机制、SQL 优化及分库分表方案 |
| `Redis` | 熟练掌握 Redis 数据结构、持久化策略、集群方案及缓存常见问题 |

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2 / Java 17 / Maven |
| 前端 | Vue 3 (Composition API) / Vite / Axios |
| AI | DeepSeek API (deepseek-chat) |
| 文档解析 | Apache POI 5.2 (Word) / Apache PDFBox 3.0 (PDF) |

## 快速开始

### 前置条件

- Java 17+
- Maven 3.8+
- Node.js 18+
- DeepSeek API Key（[platform.deepseek.com](https://platform.deepseek.com) 注册获取）

### 1. 配置 API Key（关键步骤）

**方式 A：环境变量（推荐）**

```bash
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

然后正常启动即可。

**方式 B：配置文件**

创建 `backend/src/main/resources/application-local.yml`（已加入 .gitignore，不会提交到 git）：

```yaml
deepseek:
  api:
    key: sk-xxxxxxxxxxxxxxxx
```

> ⚠️ **注意**：使用方式 B 时，启动必须带 `local` profile，否则 key 不生效。

### 2. 启动后端

```bash
cd backend

# 方式 A（环境变量已设置）
mvn spring-boot:run

# 方式 B（application-local.yml）
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

后端启动后监听 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install        # 首次运行需要
npm run dev
```

前端启动后监听 `http://localhost:5173`，API 请求自动代理到后端 8080。

### 4. 使用

1. 打开浏览器访问 `http://localhost:5173`
2. 上传 Word 简历（.doc 或 .docx）
3. 查看 AI 优化后的技能描述

## 项目结构

```
backend/src/main/java/com/finture/resume/
├── controller/ResumeController.java   # REST API
├── model/                             # 数据模型
│   ├── Resume.java
│   ├── PersonalInfo.java
│   ├── WorkExperience.java
│   ├── Education.java
│   ├── Suggestion.java
│   └── OptimizeResponse.java
├── service/
│   ├── ParserService.java             # Word 解析 → 结构化数据
│   ├── OptimizerService.java          # 调用 DeepSeek AI 优化
│   └── ExportService.java             # 生成优化后的 Word
└── config/CorsConfig.java             # 跨域配置

frontend/src/
├── App.vue                            # 主流程：上传 → 编辑 → 优化
├── api/resume.js                      # API 封装
└── components/
    ├── ResumeUploader.vue             # 文件上传
    ├── ResumeEditor.vue               # 简历编辑
    └── OptimizationResult.vue         # 优化结果展示
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/resume/parse` | 上传 Word 文件，返回结构化简历 |
| POST | `/api/resume/optimize` | 传入简历 JSON，返回 AI 优化建议 |

## 运行测试

```bash
cd backend && mvn test
```
