package com.pds.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A single Personal Data Sheet (CS Form No. 212) record: one person, with every
 * section of the form plus workflow metadata used by the dashboard/reports.
 */
public class PdsRecord {
    // ---- metadata ----
    public String id;                 // stable unique id (UUID), assigned once at creation
    public RecordStatus status = RecordStatus.ONGOING;
    public String createdAt = "";     // ISO timestamp
    public String updatedAt = "";     // ISO timestamp
    public String remarks = "";       // free-text notes for dashboard/report use

    // ---- form sections ----
    public PersonalInfo personalInfo = new PersonalInfo();
    public FamilyBackground familyBackground = new FamilyBackground();
    public List<EducationEntry> education = new ArrayList<>();
    public List<EligibilityEntry> eligibility = new ArrayList<>();
    public List<WorkExperienceEntry> workExperience = new ArrayList<>();
    public List<LearningDevelopmentEntry> learningDevelopment = new ArrayList<>();
    public List<VoluntaryWorkEntry> voluntaryWork = new ArrayList<>();
    public OtherInfo otherInfo = new OtherInfo();
    public Questionnaire questionnaire = new Questionnaire();
    public NotarizationInfo notarization = new NotarizationInfo();

    public PdsRecord() {}

    public PdsRecord(String id) { this.id = id; }

    /** Display label used in lists: "SURNAME, FIRST NAME MIDDLE NAME". */
    public String displayName() {
        PersonalInfo p = personalInfo;
        String n = (nz(p.surname) + ", " + nz(p.firstName) + " " + nz(p.middleName)).trim();
        return n.replace(" ,", ",").trim().isEmpty() ? "(Unnamed record)" : n;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
