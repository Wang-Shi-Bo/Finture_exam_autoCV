package com.finture.resume.service;

import com.finture.resume.model.Education;
import com.finture.resume.model.Resume;
import com.finture.resume.model.WorkExperience;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ExportService {

    public byte[] exportToPdf(Resume resume) throws IOException, DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        // 使用系统自带中文字体
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font sectionFont = new Font(baseFont, 14, Font.BOLD);
        Font normalFont = new Font(baseFont, 11, Font.NORMAL);

        // 个人信息
        if (resume.getPersonalInfo() != null) {
            var info = resume.getPersonalInfo();
            if (info.getName() != null && !info.getName().isEmpty()) {
                document.add(new Paragraph(info.getName(), titleFont));
            }
            StringBuilder contact = new StringBuilder();
            if (info.getEmail() != null) contact.append(info.getEmail()).append(" | ");
            if (info.getPhone() != null) contact.append(info.getPhone());
            if (contact.length() > 0) {
                document.add(new Paragraph(contact.toString(), normalFont));
            }
            document.add(new Paragraph(" "));
        }

        // 工作经历
        if (resume.getWorkExperience() != null && !resume.getWorkExperience().isEmpty()) {
            document.add(new Paragraph("工作经历", sectionFont));
            for (WorkExperience we : resume.getWorkExperience()) {
                document.add(new Paragraph(
                    we.getTitle() + " | " + we.getCompany() +
                    " (" + we.getStartDate() + " - " + we.getEndDate() + ")", normalFont));
                if (we.getHighlights() != null) {
                    for (String h : we.getHighlights()) {
                        document.add(new Paragraph("  • " + h, normalFont));
                    }
                }
            }
            document.add(new Paragraph(" "));
        }

        // 教育
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
            document.add(new Paragraph("教育背景", sectionFont));
            for (Education edu : resume.getEducation()) {
                document.add(new Paragraph(
                    edu.getSchool() + " | " + edu.getDegree() + " - " + edu.getMajor() +
                    " (" + edu.getGraduationYear() + ")", normalFont));
            }
            document.add(new Paragraph(" "));
        }

        // 技能
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            document.add(new Paragraph("专业技能", sectionFont));
            document.add(new Paragraph(String.join(", ", resume.getSkills()), normalFont));
        }

        document.close();
        return out.toByteArray();
    }
}
