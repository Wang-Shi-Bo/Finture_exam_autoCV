package com.finture.resume.service;

import com.finture.resume.model.Suggestion;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class DocumentModifierService {

    private static final Logger log = LoggerFactory.getLogger(DocumentModifierService.class);

    /**
     * Load a .docx file, find paragraphs containing original skill keywords,
     * replace them with optimized skill descriptions, preserving formatting.
     */
    public byte[] replaceSkills(byte[] docxBytes, List<Suggestion> suggestions, List<String> newSkills) throws IOException {
        if (suggestions == null || suggestions.isEmpty() || newSkills == null || newSkills.isEmpty()) {
            log.info("No skills to replace, returning original document");
            return docxBytes;
        }

        // Extract original skill keywords from suggestions
        Set<String> oldKeywords = extractOldKeywords(suggestions);
        if (oldKeywords.isEmpty()) {
            log.info("No old skill keywords found in suggestions, returning original");
            return docxBytes;
        }

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            // Find and replace skill paragraphs in body
            replaceSkillParagraphs(doc, oldKeywords, newSkills);

            // Also check tables
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        replaceSkillParagraphsInCell(cell, oldKeywords, newSkills);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private Set<String> extractOldKeywords(List<Suggestion> suggestions) {
        Set<String> keywords = new LinkedHashSet<>();
        for (Suggestion s : suggestions) {
            if (s.getOriginal() != null) {
                // Original might be comma-separated or newline-separated
                for (String part : s.getOriginal().split("[,\n]")) {
                    String trimmed = part.trim().replaceAll("^['\"\\[\\]]+", "").replaceAll("['\"\\[\\]]+$", "");
                    if (!trimmed.isEmpty() && trimmed.length() < 50) {
                        keywords.add(trimmed);
                    }
                }
            }
        }
        return keywords;
    }

    private void replaceSkillParagraphs(XWPFDocument doc, Set<String> oldKeywords, List<String> newSkills) {
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        List<Integer> skillParagraphIndices = new ArrayList<>();

        // Find paragraphs containing skill keywords
        for (int i = 0; i < paragraphs.size(); i++) {
            String text = paragraphs.get(i).getText().trim();
            if (text.isEmpty()) continue;

            int matchCount = 0;
            for (String kw : oldKeywords) {
                if (text.toLowerCase().contains(kw.toLowerCase())) {
                    matchCount++;
                }
            }

            // A skill paragraph: contains ≥2 keywords, or is short (≤20 chars) and has ≥1 match
            if (matchCount >= 2 || (text.length() <= 20 && matchCount >= 1)) {
                skillParagraphIndices.add(i);
            }
        }

        if (skillParagraphIndices.isEmpty()) {
            log.info("No skill paragraphs found in document");
            return;
        }

        // Save formatting from first skill paragraph's first run
        XWPFRun templateRun = null;
        ParagraphAlignment alignment = null;
        for (int idx : skillParagraphIndices) {
            XWPFParagraph p = paragraphs.get(idx);
            if (!p.getRuns().isEmpty()) {
                templateRun = p.getRuns().get(0);
                alignment = p.getAlignment();
                break;
            }
        }

        // Remove old skill paragraphs in reverse order
        for (int i = skillParagraphIndices.size() - 1; i >= 0; i--) {
            doc.removeBodyElement(doc.getPosOfParagraph(paragraphs.get(skillParagraphIndices.get(i))));
        }

        // Insert new skill paragraphs at the position of the first old skill paragraph
        int insertPos = skillParagraphIndices.get(0);
        for (int i = newSkills.size() - 1; i >= 0; i--) {
            XWPFParagraph newPara = doc.insertNewParagraph(doc.getParagraphs().get(insertPos).getCTP().newCursor());
            newPara.setAlignment(alignment);

            XWPFRun run = newPara.createRun();
            run.setText(newSkills.get(i));

            // Copy formatting from template
            if (templateRun != null) {
                copyRunFormatting(templateRun, run);
            }
        }

        log.info("Replaced {} skill paragraphs with {} new ones", skillParagraphIndices.size(), newSkills.size());
    }

    private void replaceSkillParagraphsInCell(XWPFTableCell cell, Set<String> oldKeywords, List<String> newSkills) {
        // Simplified: just check each paragraph in the cell
        for (XWPFParagraph para : cell.getParagraphs()) {
            String text = para.getText().trim();
            if (text.isEmpty()) continue;

            int matchCount = 0;
            for (String kw : oldKeywords) {
                if (text.toLowerCase().contains(kw.toLowerCase())) {
                    matchCount++;
                }
            }

            if (matchCount >= 2 || (text.length() <= 20 && matchCount >= 1)) {
                // Replace text in this paragraph with new skills (joined)
                if (!para.getRuns().isEmpty()) {
                    XWPFRun firstRun = para.getRuns().get(0);
                    // Remove other runs
                    for (int i = para.getRuns().size() - 1; i > 0; i--) {
                        para.removeRun(i);
                    }
                    firstRun.setText(String.join("，", newSkills), 0);
                }
            }
        }
    }

    private void copyRunFormatting(XWPFRun source, XWPFRun target) {
        try {
            target.setBold(source.isBold());
        } catch (Exception ignored) {}
        try {
            target.setItalic(source.isItalic());
        } catch (Exception ignored) {}
        try {
            target.setFontSize(source.getFontSize());
        } catch (Exception ignored) {}
        try {
            target.setFontFamily(source.getFontFamily());
        } catch (Exception ignored) {}
        try {
            target.setColor(source.getColor());
        } catch (Exception ignored) {}
        try {
            target.setUnderline(source.getUnderline());
        } catch (Exception ignored) {}
    }
}
