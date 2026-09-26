package com.pds.model;

import java.util.ArrayList;
import java.util.List;

public class Questionnaire {
    // 34
    public YesNoItem q34a3rdDegree = new YesNoItem();  // within 3rd degree
    public YesNoItem q34b4thDegree = new YesNoItem();  // within 4th degree (LGU career)
    // 35
    public YesNoItem q35aAdminOffense = new YesNoItem();
    public YesNoItem q35bCriminallyCharged = new YesNoItem();
    public String q35bDateFiled = "";
    public String q35bStatusOfCase = "";
    // 36
    public YesNoItem q36ConvictedCrime = new YesNoItem();
    // 37
    public YesNoItem q37SeparatedFromService = new YesNoItem();
    // 38
    public YesNoItem q38aCandidateElection = new YesNoItem();
    public YesNoItem q38bResignedToCampaign = new YesNoItem();
    // 39
    public YesNoItem q39ImmigrantStatus = new YesNoItem();
    // 40
    public YesNoItem q40aIndigenousGroup = new YesNoItem();
    public YesNoItem q40bPersonWithDisability = new YesNoItem();
    public String q40bIdNo = "";
    public YesNoItem q40cSoloParent = new YesNoItem();
    public String q40cIdNo = "";
    // 41 references
    public List<ReferenceEntry> references = new ArrayList<>();
    // 42 government issued ID
    public String govtIssuedId = "";
    public String idLicensePassportNo = "";
    public String dateOfIssuance = "";
    public String placeOfIssuance = "";
    public String dateAccomplished = "";
}
