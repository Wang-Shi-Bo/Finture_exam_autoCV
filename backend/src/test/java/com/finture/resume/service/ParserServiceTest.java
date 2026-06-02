package com.finture.resume.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ParserServiceTest {

    private final ParserService parserService = new ParserService();

    @Test
    void parseUnsupportedFormat_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.png", "image/png", "test".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> parserService.parse(file));
    }

    @Test
    void parseDocx_shouldReturnResume() throws Exception {
        // Create a minimal valid docx (empty document)
        byte[] docxBytes = createMinimalDocx();
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxBytes
        );
        var resume = parserService.parse(file);
        assertNotNull(resume);
        // Empty docx produces empty text, so language detection may fallback
    }

    private byte[] createMinimalDocx() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out);
        zip.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
        zip.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="xml" ContentType="application/xml"/>
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>""".getBytes());
        zip.closeEntry();
        zip.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
        zip.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>""".getBytes());
        zip.closeEntry();
        zip.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
        zip.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Java Developer with 5 years experience. Email: test@example.com Phone: 13812345678. Skills: Java, Spring Boot, MySQL.</w:t></w:r></w:p>
              </w:body>
            </w:document>""".getBytes());
        zip.closeEntry();
        zip.finish();
        return out.toByteArray();
    }

    @Test
    void parseDocx_shouldExtractEmailAndPhone() throws Exception {
        byte[] docxBytes = createMinimalDocx();
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxBytes
        );
        var resume = parserService.parse(file);
        assertNotNull(resume);
        assertNotNull(resume.getPersonalInfo());
        assertEquals("test@example.com", resume.getPersonalInfo().getEmail());
        assertEquals("13812345678", resume.getPersonalInfo().getPhone());
        assertTrue(resume.getSkills().contains("Java"));
        assertTrue(resume.getSkills().contains("Spring Boot"));
        assertTrue(resume.getSkills().contains("MySQL"));
    }
}
