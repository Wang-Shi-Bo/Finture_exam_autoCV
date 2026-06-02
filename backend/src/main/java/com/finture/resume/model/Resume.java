package com.finture.resume.model;

import java.util.List;

public class Resume {
    private PersonalInfo personalInfo;
    private String summary;
    private List<WorkExperience> workExperience;
    private List<Education> education;
    private List<Project> projects;
    private List<String> skills;
    private String language;

    public Resume() {}

    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public void setPersonalInfo(PersonalInfo personalInfo) { this.personalInfo = personalInfo; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<WorkExperience> getWorkExperience() { return workExperience; }
    public void setWorkExperience(List<WorkExperience> workExperience) { this.workExperience = workExperience; }
    public List<Education> getEducation() { return education; }
    public void setEducation(List<Education> education) { this.education = education; }
    public List<Project> getProjects() { return projects; }
    public void setProjects(List<Project> projects) { this.projects = projects; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
