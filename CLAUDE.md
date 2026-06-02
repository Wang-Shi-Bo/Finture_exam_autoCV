# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

简历智能优化平台 — LLM-powered resume optimization tool. Upload PDF/Word resumes, get AI optimization suggestions, export optimized PDF.

## Tech Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.2, Java 17, Maven |
| Frontend | Vue 3 (Composition API), Vite, Axios |
| AI | DeepSeek API (chat completions) |
| PDF Parse | Apache PDFBox 3.0 |
| Word Parse | Apache POI 5.2 |
| PDF Generate | OpenPDF 2.0 |

## Commands

```bash
# Backend
cd backend && mvn spring-boot:run     # Start on :8080
cd backend && mvn test                 # Run tests (8 tests)

# Frontend
cd frontend && npm install             # Install dependencies
cd frontend && npm run dev             # Start dev server on :5173
cd frontend && npm run build           # Production build
```

## Architecture

```
Vue SPA (:5173) → REST API → Spring Boot (:8080) → DeepSeek API
```

Three core services:
- **ParserService** — PDF/Word → structured JSON (Resume model)
- **OptimizerService** — Resume JSON → LLM → suggestions + optimized JSON (with retry)
- **ExportService** — Resume JSON → PDF file (OpenPDF)

API: `POST /api/resume/parse` | `POST /api/resume/optimize` | `POST /api/resume/export`

## Project Structure

```
backend/src/main/java/com/finture/resume/
├── controller/ResumeController.java
├── model/    (Resume, PersonalInfo, WorkExperience, Education, Suggestion, OptimizeResponse)
├── service/  (ParserService, OptimizerService, ExportService)
└── config/   (CorsConfig)

frontend/src/
├── App.vue              (3-step wizard: upload → edit → optimize)
├── api/resume.js        (Axios wrappers)
└── components/          (ResumeUploader, ResumeEditor, OptimizationResult)
```

## Configuration

DeepSeek API key set via `DEEPSEEK_API_KEY` env var (or in `application.yml`).
Vite dev server proxies `/api` to `localhost:8080`.
