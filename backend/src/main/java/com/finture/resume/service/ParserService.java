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
        boolean isZh = detectLanguage(text).equals("zh");
        resume.setLanguage(isZh ? "zh" : "en");

        PersonalInfo info = new PersonalInfo();
        info.setEmail(extractEmail(text));
        info.setPhone(extractPhone(text));

        String[] lines = text.split("\\n");
        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) nonEmptyLines.add(trimmed);
        }

        // Name: first non-empty line that isn't an email/phone
        for (String line : nonEmptyLines) {
            if (!line.contains("@") && !line.matches(".*\\d{8,}.*") && line.length() < 50) {
                info.setName(line);
                break;
            }
        }

        // Location: common Chinese cities or "地点"/"Location" pattern
        info.setLocation(extractLocation(text, isZh));

        resume.setPersonalInfo(info);
        resume.setSkills(extractSkills(text));

        // Split text into sections
        String[] workSection = extractSection(text, isZh);
        String introSection = workSection[0];  // text before work experience
        String workText = workSection[1];       // work experience section
        String eduText = workSection[2];        // education section

        resume.setSummary(introSection.trim());
        resume.setWorkExperience(parseWorkExperience(workText, isZh));
        resume.setEducation(parseEducation(eduText, isZh));

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

    private String extractLocation(String text, boolean isZh) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "武汉",
            "西安", "重庆", "苏州", "天津", "长沙", "郑州", "东莞", "青岛", "厦门", "合肥",
            "Beijing", "Shanghai", "Shenzhen", "Guangzhou", "Hangzhou", "Nanjing",
            "Chengdu", "Wuhan", "London", "New York", "San Francisco", "Seattle"};
        for (String city : cities) {
            if (text.contains(city)) return city;
        }
        // Try "地点：xxx" or "Location: xxx" pattern
        Matcher m = Pattern.compile(isZh ? "地点[：:]\\s*(.+)" : "Location\\s*[:：]\\s*(.+)",
            Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }

    /**
     * Returns [intro, workSection, eduSection]
     */
    private String[] extractSection(String text, boolean isZh) {
        String workHeader = isZh ? "工作经历|工作经验|工作經驗|WORK EXPERIENCE|Work Experience|Professional Experience"
                                  : "WORK EXPERIENCE|Work Experience|Professional Experience|工作经历";
        String eduHeader = isZh ? "教育背景|教育经历|学历|EDUCATION|Education"
                                : "EDUCATION|Education|教育背景";

        String[] parts = text.split("(?i)(" + workHeader + ")");
        String intro = parts.length > 0 ? parts[0] : text;

        String workAndEdu = parts.length > 1 ? parts[1] : "";

        String[] eduParts = workAndEdu.split("(?i)(" + eduHeader + ")");
        String work = eduParts.length > 0 ? eduParts[0] : "";
        String edu = eduParts.length > 1 ? eduParts[1] : "";

        return new String[]{intro, work, edu};
    }

    private List<WorkExperience> parseWorkExperience(String text, boolean isZh) {
        List<WorkExperience> list = new ArrayList<>();
        if (text.trim().isEmpty()) return list;

        // Split by bullet points or numbered items
        String[] entries = text.split("\\n\\s*(?=[•\\-\\*●]|\\d+[.\\)])");
        if (entries.length <= 1) {
            entries = text.split("\\n{2,}"); // fallback: split by blank lines
        }

        for (String entry : entries) {
            String t = entry.trim();
            if (t.isEmpty() || t.length() < 10) continue;

            WorkExperience we = new WorkExperience();
            // Try to extract date range
            Matcher dateM = Pattern.compile(
                "(\\d{4}\\.\\d{1,2}|\\d{4}/\\d{1,2}|\\d{4}-\\d{1,2})\\s*[-–至到]\\s*(\\d{4}\\.\\d{1,2}|\\d{4}/\\d{1,2}|\\d{4}-\\d{1,2}|至今|现在|Present|Now)"
            ).matcher(t);
            if (dateM.find()) {
                we.setStartDate(dateM.group(1));
                we.setEndDate(dateM.group(2));
                t = t.replace(dateM.group(), "").trim();
            }

            // Try to extract company (often after date or at beginning)
            Matcher companyM = Pattern.compile(
                isZh ? "([\\u4e00-\\u9fff（）()A-Za-z]+(?:公司|集团|科技|有限|技术|银行|医院|学校|大学|学院|研究所|设计院))"
                     : "([A-Z][A-Za-z0-9\\s&.,]+(?:Inc|Corp|Ltd|LLC|Company|Group|Technologies|Labs|Bank|Hospital|University|College))"
            ).matcher(t);
            if (companyM.find()) {
                we.setCompany(companyM.group(1).trim());
                t = t.replace(companyM.group(), "").trim();
            }

            // Try to extract title (often contains 工程师/经理/主管/Developer/Engineer/Manager)
            Matcher titleM = Pattern.compile(
                isZh ? "(\\S*(?:工程师|经理|主管|总监|专员|设计师|架构师|分析师|顾问|代表|助理|实习生|负责人|主任))"
                     : "(\\S*(?:Engineer|Developer|Manager|Director|Lead|Architect|Analyst|Designer|Consultant|Specialist|Intern|Assistant|Head))"
            ).matcher(t);
            if (titleM.find()) {
                we.setTitle(titleM.group(1).trim());
                t = t.replace(titleM.group(), "").trim();
            }

            // Remaining text as highlights
            List<String> highlights = new ArrayList<>();
            // Split remaining text by bullets or newlines
            String[] hlLines = t.split("[\\n•\\-\\*●]");
            for (String hl : hlLines) {
                String h = hl.trim();
                if (h.length() > 3) highlights.add(h);
            }
            if (highlights.isEmpty() && t.length() > 3) {
                highlights.add(t);
            }
            we.setHighlights(highlights);

            if (we.getCompany() != null || we.getTitle() != null || !highlights.isEmpty()) {
                list.add(we);
            }
        }

        return list;
    }

    private List<Education> parseEducation(String text, boolean isZh) {
        List<Education> list = new ArrayList<>();
        if (text.trim().isEmpty()) return list;

        String[] entries = text.split("\\n\\s*(?=[•\\-\\*●]|\\d+[.\\)])");
        if (entries.length <= 1) {
            entries = text.split("\\n{2,}");
        }

        for (String entry : entries) {
            String t = entry.trim();
            if (t.isEmpty() || t.length() < 5) continue;

            Education edu = new Education();

            // School
            Matcher schoolM = Pattern.compile(
                isZh ? "([\\u4e00-\\u9fff()（）]+(?:大学|学院|学校|University|College|Institute))"
                     : "([A-Z][A-Za-z\\s]+(?:University|College|Institute|School))"
            ).matcher(t);
            if (schoolM.find()) {
                edu.setSchool(schoolM.group(1).trim());
            }

            // Degree
            Matcher degreeM = Pattern.compile(
                isZh ? "(博士|硕士|本科|学士|大专|MBA|Ph\\.D|Master|Bachelor|Associate)"
                     : "(Ph\\.D|PhD|Master|Bachelor|Associate|MBA|MS|BS|BA|M\\.S\\.|B\\.S\\.|M\\.A\\.|B\\.A\\.)"
            ).matcher(t);
            if (degreeM.find()) {
                edu.setDegree(degreeM.group(1).trim());
            }

            // Major
            Matcher majorM = Pattern.compile(
                isZh ? "(专业[：:]\\s*\\S+)" : "(Major\\s*[：:]\\s*\\S+)"
            ).matcher(t);
            if (majorM.find()) {
                String m = majorM.group(1);
                edu.setMajor(m.replaceAll("专业[：:]\\s*|Major\\s*[：:]\\s*", "").trim());
            }

            // Graduation year
            Matcher yearM = Pattern.compile("(20\\d{2}|19\\d{2})\\s*年?").matcher(t);
            if (yearM.find()) {
                edu.setGraduationYear(yearM.group(1));
            }

            if (edu.getSchool() != null || edu.getDegree() != null) {
                list.add(edu);
            }
        }

        return list;
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
