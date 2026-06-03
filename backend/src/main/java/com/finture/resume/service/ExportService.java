package com.finture.resume.service;

import com.finture.resume.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class ExportService {

    private final FileStorageService fileStorageService;
    private final DocumentModifierService documentModifierService;

    public ExportService(FileStorageService fileStorageService,
                         DocumentModifierService documentModifierService) {
        this.fileStorageService = fileStorageService;
        this.documentModifierService = documentModifierService;
    }

    /**
     * Main export: load original file, modify skills in-place, return modified .docx.
     * Preserves all original formatting.
     */
    public byte[] exportToDocx(ExportRequest request) throws IOException {
        String fileId = request.getFileId();
        if (fileId == null || fileId.isEmpty()) {
            throw new IllegalArgumentException("缺少 fileId，请重新上传文件");
        }

        byte[] originalBytes = fileStorageService.load(fileId);
        String filename = fileStorageService.getOriginalFilename(fileId);

        // Convert .doc to .docx if needed
        byte[] docxBytes;
        if (filename.endsWith(".doc")) {
            docxBytes = convertDocToDocx(originalBytes);
        } else {
            docxBytes = originalBytes;
        }

        // Modify skills in-place
        if (request.getSuggestions() != null && !request.getSuggestions().isEmpty()
                && request.getResume() != null && request.getResume().getSkills() != null) {
            docxBytes = documentModifierService.replaceSkills(
                docxBytes,
                request.getSuggestions(),
                request.getResume().getSkills()
            );
        }

        return docxBytes;
    }

    /**
     * Fallback: generate PDF from JSON using OpenPDF (loses original formatting).
     */
    public byte[] exportToPdf(Resume resume) throws IOException, DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font sectionFont = new Font(baseFont, 14, Font.BOLD);
        Font normalFont = new Font(baseFont, 11, Font.NORMAL);

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

        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
            document.add(new Paragraph("教育背景", sectionFont));
            for (Education edu : resume.getEducation()) {
                document.add(new Paragraph(
                    edu.getSchool() + " | " + edu.getDegree() + " - " + edu.getMajor() +
                    " (" + edu.getGraduationYear() + ")", normalFont));
            }
            document.add(new Paragraph(" "));
        }

        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            document.add(new Paragraph("专业技能", sectionFont));
            for (String skill : resume.getSkills()) {
                document.add(new Paragraph("  • " + skill, normalFont));
            }
        }

        document.close();
        return out.toByteArray();
    }

    /**
     * Convert .doc (OLE2) to .docx using HWPFDocument → XWPFDocument.
     * Basic formatting (bold, italic) is preserved; complex formatting may be lost.
     */
    private byte[] convertDocToDocx(byte[] docBytes) throws IOException {
        try (HWPFDocument hwpf = new HWPFDocument(new ByteArrayInputStream(docBytes));
             XWPFDocument xwpf = new XWPFDocument()) {

            var range = hwpf.getRange();
            for (int i = 0; i < range.numParagraphs(); i++) {
                var hwpfPara = range.getParagraph(i);
                XWPFParagraph xwpfPara = xwpf.createParagraph();
                XWPFRun run = xwpfPara.createRun();
                run.setText(hwpfPara.text());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            xwpf.write(out);
            return out.toByteArray();
        }
    }
}
