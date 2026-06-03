package com.finture.resume.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${resume.storage.dir:./data/originals}")
    private String storageDir;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(Path.of(storageDir));
    }

    public String save(byte[] fileBytes, String originalFilename) throws IOException {
        String fileId = UUID.randomUUID().toString();
        Path filePath = Path.of(storageDir, fileId);
        Files.write(filePath, fileBytes);

        // Save original filename as metadata
        if (originalFilename != null) {
            Path metaPath = Path.of(storageDir, fileId + ".meta");
            Files.writeString(metaPath, originalFilename);
        }
        return fileId;
    }

    public byte[] load(String fileId) throws IOException {
        Path filePath = Path.of(storageDir, fileId);
        if (!Files.exists(filePath)) {
            throw new IOException("原始文件不存在或已过期，请重新上传");
        }
        return Files.readAllBytes(filePath);
    }

    public String getOriginalFilename(String fileId) {
        try {
            Path metaPath = Path.of(storageDir, fileId + ".meta");
            if (Files.exists(metaPath)) {
                return Files.readString(metaPath).trim();
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    public void delete(String fileId) throws IOException {
        Files.deleteIfExists(Path.of(storageDir, fileId));
        Files.deleteIfExists(Path.of(storageDir, fileId + ".meta"));
    }
}
