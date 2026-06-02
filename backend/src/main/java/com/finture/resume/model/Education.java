package com.finture.resume.model;

public class Education {
    private String school;
    private String degree;
    private String major;
    private String graduationYear;

    public Education() {}

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGraduationYear() { return graduationYear; }
    public void setGraduationYear(String graduationYear) { this.graduationYear = graduationYear; }
}
