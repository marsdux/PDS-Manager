package com.pds.model;

public class EducationEntry {
    public String level = "";           // ELEMENTARY, SECONDARY, VOCATIONAL, COLLEGE, GRADUATE STUDIES, etc.
    public String schoolName = "";
    public String degreeCourse = "";
    public String periodFrom = "";
    public String periodTo = "";
    public String highestLevelUnits = "";
    public String yearGraduated = "";
    public String honors = "";
    public String attachmentFileName = ""; // scanned proof (e.g. diploma/TOR), optional
    public String attachmentBase64 = "";

    public EducationEntry() {}
    public EducationEntry(String level, String schoolName, String degreeCourse, String periodFrom,
                           String periodTo, String highestLevelUnits, String yearGraduated, String honors) {
        this.level = level; this.schoolName = schoolName; this.degreeCourse = degreeCourse;
        this.periodFrom = periodFrom; this.periodTo = periodTo; this.highestLevelUnits = highestLevelUnits;
        this.yearGraduated = yearGraduated; this.honors = honors;
    }
}
