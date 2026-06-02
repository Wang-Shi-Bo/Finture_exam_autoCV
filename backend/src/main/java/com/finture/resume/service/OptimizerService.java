package com.finture.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finture.resume.model.OptimizeResponse;
import com.finture.resume.model.Resume;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OptimizerService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OptimizerService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public OptimizeResponse optimize(Resume resume) throws JsonProcessingException {
        String resumeJson = objectMapper.writeValueAsString(resume);
        String prompt = buildPrompt(resumeJson, resume.getLanguage());

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", getSystemPrompt(resume.getLanguage())),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, Map.class
            );

            Map body = response.getBody();
            if (body == null) {
                throw new RuntimeException("DeepSeek API 返回空响应");
            }

            List<Map> choices = (List<Map>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("DeepSeek API 返回无有效结果");
            }

            Map message = (Map) choices.get(0).get("message");
            String content = (String) message.get("content");

            return parseOptimizeResponse(content);
        } catch (Exception e) {
            // 重试一次
            try {
                ResponseEntity<Map> retryResponse = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, Map.class
                );
                Map retryBody = retryResponse.getBody();
                List<Map> retryChoices = (List<Map>) retryBody.get("choices");
                Map retryMessage = (Map) retryChoices.get(0).get("message");
                String retryContent = (String) retryMessage.get("content");
                return parseOptimizeResponse(retryContent);
            } catch (Exception retryError) {
                throw new RuntimeException("LLM 服务暂时不可用，请稍后重试");
            }
        }
    }

    private String getSystemPrompt(String language) {
        if ("zh".equals(language)) {
            return "你是一个专业的简历优化顾问。根据用户提供的简历JSON，从内容措辞、职位匹配度、ATS关键词三个维度进行优化。返回严格的JSON格式。";
        }
        return "You are a professional resume optimization consultant. Optimize the given resume JSON from three dimensions: wording, job matching, and ATS keywords. Return strict JSON format.";
    }

    private String buildPrompt(String resumeJson, String language) {
        String lang = "zh".equals(language) ? "中文" : "English";
        return String.format("""
            请优化以下%s简历。从三个维度进行优化：
            1. 内容措辞：使用更有力的动词，突出量化成果
            2. 职位匹配：增强通用竞争力
            3. ATS关键词：补充行业标准关键词

            当前简历JSON：
            %s

            请返回如下JSON格式（不要包含任何其他文字）：
            {
              "suggestions": [
                {
                  "section": "summary",
                  "original": "原始文本",
                  "suggestion": "优化后文本",
                  "reason": "优化原因"
                }
              ],
              "optimizedResume": { /* 优化后的完整简历JSON，结构与输入一致 */ }
            }
            """, lang, resumeJson);
    }

    /**
     * 使用 LLM 将原始简历文本解析为结构化的 Resume 对象
     */
    public Resume parseResumeFromText(String rawText) {
        String prompt = String.format("""
            请从以下简历文本中提取结构化信息，返回严格的JSON格式（不要包含任何其他文字）。

            提取要求：
            - personalInfo: 姓名(name)、邮箱(email)、电话(phone)、地点(location)
            - summary: 一句话个人总结（从原文提炼，不要照搬全文）
            - workExperience: 工作经历数组，每条包含公司(company)、职位(title)、开始日期(startDate)、结束日期(endDate)、亮点(highlights数组)
            - education: 教育经历数组，每条包含学校(school)、学位(degree)、专业(major)、毕业年份(graduationYear)
            - skills: 技能数组
            - language: "zh" 或 "en"

            JSON格式示例：
            {
              "personalInfo": {"name": "姓名", "email": "xxx@xxx.com", "phone": "138xxxx", "location": "城市"},
              "summary": "简短的个人总结",
              "workExperience": [{"company": "公司名", "title": "职位", "startDate": "2020-01", "endDate": "2022-06", "highlights": ["亮点1", "亮点2"]}],
              "education": [{"school": "大学名", "degree": "本科", "major": "专业名", "graduationYear": "2018"}],
              "skills": ["Java", "Spring Boot"],
              "language": "zh"
            }

            简历原文：
            %s
            """, rawText);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", "你是一个专业的简历解析器。从简历文本中提取结构化信息，只返回JSON，不要加任何解释。"),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, Map.class
            );

            Map body = response.getBody();
            List<Map> choices = (List<Map>) body.get("choices");
            Map message = (Map) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Extract JSON from response
            String jsonStr = content.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("^```(?:json)?\\s*", "");
                jsonStr = jsonStr.replaceAll("\\s*```$", "");
            }
            return objectMapper.readValue(jsonStr, Resume.class);
        } catch (Exception e) {
            throw new RuntimeException("LLM 简历解析失败: " + e.getMessage());
        }
    }

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
            fallbackResume.setSummary(content);
            fallback.setOptimizedResume(fallbackResume);
            return fallback;
        }
    }
}
