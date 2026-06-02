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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParserService {

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

        return extractResumeFromText(text);
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

    private Resume extractResumeFromText(String text) {
        Resume resume = new Resume();
        resume.setLanguage(detectLanguage(text));

        PersonalInfo info = new PersonalInfo();
        info.setEmail(extractEmail(text));
        info.setPhone(extractPhone(text));
        resume.setPersonalInfo(info);

        resume.setSkills(extractSkills(text));
        resume.setSummary(text.trim());
        resume.setWorkExperience(new ArrayList<>());
        resume.setEducation(new ArrayList<>());

        return resume;
    }

    private String detectLanguage(String text) {
        return text.matches(".*[\\u4e00-\\u9fff]+.*") ? "zh" : "en";
    }

    private String extractEmail(String text) {
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text);
        return m.find() ? m.group() : "";
    }

    private String extractPhone(String text) {
        Matcher m = Pattern.compile("1[3-9]\\d{9}").matcher(text);
        return m.find() ? m.group() : "";
    }

    private List<String> extractSkills(String text) {
        String[] commonSkills = {"Java", "Python", "Spring Boot", "MySQL", "Redis",
                "Docker", "Kubernetes", "React", "Vue", "TypeScript", "JavaScript",
                "AWS", "Linux", "Git", "REST API", "微服务"};
        List<String> found = new ArrayList<>();
        String lowerText = text.toLowerCase();
        for (String skill : commonSkills) {
            if (lowerText.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }
        return found;
    }
}
