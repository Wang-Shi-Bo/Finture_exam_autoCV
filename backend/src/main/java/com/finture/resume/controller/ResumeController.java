package com.finture.resume.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.finture.resume.model.OptimizeResponse;
import com.finture.resume.model.Resume;
import com.finture.resume.service.ExportService;
import com.finture.resume.service.OptimizerService;
import com.finture.resume.service.ParserService;
import com.lowagie.text.DocumentException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ParserService parserService;
    private final OptimizerService optimizerService;
    private final ExportService exportService;

    public ResumeController(ParserService parserService,
                            OptimizerService optimizerService,
                            ExportService exportService) {
        this.parserService = parserService;
        this.optimizerService = optimizerService;
        this.exportService = exportService;
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parse(@RequestParam("file") MultipartFile file) {
        try {
            Resume resume = parserService.parse(file);
            return ResponseEntity.ok(resume);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(422)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/optimize")
    public ResponseEntity<?> optimize(@RequestBody Resume resume) {
        try {
            OptimizeResponse result = optimizerService.optimize(resume);
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "简历格式无效"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(502)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/export")
    public ResponseEntity<?> export(@RequestBody Resume resume) {
        try {
            byte[] pdfBytes = exportService.exportToPdf(resume);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment().filename("optimized-resume.pdf").build()
            );
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException | DocumentException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "PDF 生成失败: " + e.getMessage()));
        }
    }
}
