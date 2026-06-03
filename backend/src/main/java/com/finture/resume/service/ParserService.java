package com.finture.resume.service;

import com.finture.resume.model.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParserService {

    private final OptimizerService optimizerService;
    private final FileStorageService fileStorageService;

    public ParserService(OptimizerService optimizerService,
                         FileStorageService fileStorageService) {
        this.optimizerService = optimizerService;
        this.fileStorageService = fileStorageService;
    }

    public ParseResult parse(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // Save original file bytes immediately (before consuming the stream)
        byte[] fileBytes = file.getBytes();
        String fileId = fileStorageService.save(fileBytes, filename);

        String text;
        if (filename.endsWith(".docx")) {
            text = parseDocx(fileBytes);
        } else if (filename.endsWith(".doc")) {
            text = parseDoc(fileBytes);
        } else {
            throw new IllegalArgumentException("仅支持 Word (.doc/.docx) 格式，请上传 Word 文件");
        }

        // Use LLM for structured parsing
        Resume resume;
        try {
            resume = optimizerService.parseResumeFromText(text);
            // Ensure non-null collections
            if (resume.getWorkExperience() == null) resume.setWorkExperience(new ArrayList<>());
            if (resume.getEducation() == null) resume.setEducation(new ArrayList<>());
            if (resume.getSkills() == null) resume.setSkills(new ArrayList<>());
            if (resume.getPersonalInfo() == null) resume.setPersonalInfo(new PersonalInfo());
        } catch (Exception e) {
            // Fallback: basic regex extraction
            resume = fallbackParse(text);
        }

        return new ParseResult(resume, fileId);
    }

    private String parseDocx(byte[] fileBytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            StringBuilder sb = new StringBuilder();
            document.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    private String parseDoc(byte[] fileBytes) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileBytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
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
