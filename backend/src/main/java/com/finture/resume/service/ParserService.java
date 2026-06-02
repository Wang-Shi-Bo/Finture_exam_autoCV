package com.finture.resume.service;

import com.finture.resume.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
        if (filename.endsWith(".pdf")) {
            text = parsePdf(file);
        } else if (filename.endsWith(".docx") || filename.endsWith(".doc")) {
            text = parseDocx(file);
        } else {
            throw new IllegalArgumentException("仅支持 PDF 和 Word (.docx/.doc) 格式");
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

    private String parsePdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text.trim().isEmpty()) {
                throw new IOException("PDF 无法解析（可能是扫描件或图片PDF），请尝试手动输入");
            }
            return text;
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

        resume.setSummary(text.trim());
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
