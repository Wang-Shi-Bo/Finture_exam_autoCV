package com.finture.resume.service;

import com.finture.resume.model.PersonalInfo;
import com.finture.resume.model.Resume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParserServiceTest {

    private ParserService parserService;
    private OptimizerService mockOptimizer;

    @BeforeEach
    void setUp() {
        mockOptimizer = mock(OptimizerService.class);
        parserService = new ParserService(mockOptimizer);
    }

    @Test
    void parseUnsupportedFormat_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.png", "image/png", "test".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> parserService.parse(file));
    }

    @Test
    void parseDocx_shouldReturnResumeFromLLM() throws Exception {
        // Mock LLM response
        Resume mockResume = new Resume();
        PersonalInfo info = new PersonalInfo();
        info.setName("Test User");
        info.setEmail("test@example.com");
        info.setPhone("13812345678");
        mockResume.setPersonalInfo(info);
        mockResume.setSummary("A Java developer");
        mockResume.setWorkExperience(new ArrayList<>());
        mockResume.setEducation(new ArrayList<>());
        mockResume.setSkills(List.of("Java", "Spring Boot"));
        mockResume.setLanguage("en");

        when(mockOptimizer.parseResumeFromText(anyString())).thenReturn(mockResume);

        byte[] docxBytes = createMinimalDocx();
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxBytes
        );
        var resume = parserService.parse(file);
        assertNotNull(resume);
        assertEquals("Test User", resume.getPersonalInfo().getName());
        assertEquals("test@example.com", resume.getPersonalInfo().getEmail());
        assertEquals("13812345678", resume.getPersonalInfo().getPhone());
    }

    @Test
    void parseDocx_llmFailure_shouldFallback() throws Exception {
        // Mock LLM failure
        when(mockOptimizer.parseResumeFromText(anyString()))
            .thenThrow(new RuntimeException("API error"));

        byte[] docxBytes = createMinimalDocx();
        MockMultipartFile file = new MockMultipartFile(
            "resume", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxBytes
        );
        var resume = parserService.parse(file);
        assertNotNull(resume);
        // Fallback should still extract email/phone via regex
        assertEquals("test@example.com", resume.getPersonalInfo().getEmail());
        assertEquals("13812345678", resume.getPersonalInfo().getPhone());
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
}
