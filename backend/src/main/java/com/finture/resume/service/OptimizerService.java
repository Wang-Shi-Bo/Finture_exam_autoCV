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
