package com.finture.resume.model;

import java.util.List;

public class OptimizeResponse {
    private List<Suggestion> suggestions;
    private Resume optimizedResume;

    public OptimizeResponse() {}

    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }
    public Resume getOptimizedResume() { return optimizedResume; }
    public void setOptimizedResume(Resume optimizedResume) { this.optimizedResume = optimizedResume; }
}
