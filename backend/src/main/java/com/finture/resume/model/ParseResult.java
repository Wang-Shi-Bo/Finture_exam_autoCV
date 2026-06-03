package com.finture.resume.model;

public class ParseResult {
    private Resume resume;
    private String fileId;

    public ParseResult() {}

    public ParseResult(Resume resume, String fileId) {
        this.resume = resume;
        this.fileId = fileId;
    }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
}
