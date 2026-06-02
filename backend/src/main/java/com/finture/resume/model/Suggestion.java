package com.finture.resume.model;

public class Suggestion {
    private String section;
    private String original;
    private String suggestion;
    private String reason;

    public Suggestion() {}

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
