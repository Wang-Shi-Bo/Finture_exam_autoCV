package com.finture.resume.controller;

import com.finture.resume.model.ParseResult;
import com.finture.resume.model.Resume;
import com.finture.resume.service.ExportService;
import com.finture.resume.service.OptimizerService;
import com.finture.resume.service.ParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParserService parserService;

    @MockBean
    private OptimizerService optimizerService;

    @MockBean
    private ExportService exportService;

    @Test
    void parse_shouldReturnResumeJson() throws Exception {
        Resume mockResume = new Resume();
        mockResume.setLanguage("zh");
        ParseResult mockResult = new ParseResult(mockResume, "test-uuid");
        when(parserService.parse(any())).thenReturn(mockResult);

        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test".getBytes()
        );

        mockMvc.perform(multipart("/api/resume/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resume.language").value("zh"))
            .andExpect(jsonPath("$.fileId").value("test-uuid"));
    }

    @Test
    void parse_wrongFormat_shouldReturn400() throws Exception {
        when(parserService.parse(any()))
            .thenThrow(new IllegalArgumentException("仅支持 Word (.doc/.docx) 格式"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.png", "image/png", "test".getBytes()
        );

        mockMvc.perform(multipart("/api/resume/parse").file(file))
            .andExpect(status().isBadRequest());
    }

    @Test
    void parse_serverError_shouldReturn422() throws Exception {
        when(parserService.parse(any()))
            .thenThrow(new java.io.IOException("Parse error"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test".getBytes()
        );

        mockMvc.perform(multipart("/api/resume/parse").file(file))
            .andExpect(status().is(422));
    }
}
