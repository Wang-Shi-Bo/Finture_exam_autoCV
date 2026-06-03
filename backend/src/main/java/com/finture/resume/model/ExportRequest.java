package com.finture.resume.model;

import java.util.List;

public class ExportRequest {
    private String fileId;
    private Resume resume;
    private List<Suggestion> suggestions;

    public ExportRequest() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }
}
