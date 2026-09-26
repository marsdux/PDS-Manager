package com.pds.model;

import java.util.ArrayList;
import java.util.List;

public class FamilyBackground {
    // 22 spouse
    public String spouseSurname = "";
    public String spouseFirstName = "";
    public String spouseMiddleName = "";
    public String spouseNameExtension = "";
    public String spouseOccupation = "";
    public String spouseEmployerBusinessName = "";
    public String spouseBusinessAddress = "";
    public String spouseTelephoneNo = "";
    // 23 children
    public List<Child> children = new ArrayList<>();
    // 24 father
    public String fatherSurname = "";
    public String fatherFirstName = "";
    public String fatherMiddleName = "";
    public String fatherNameExtension = "";
    // 25 mother
    public String motherMaidenSurname = "";
    public String motherFirstName = "";
    public String motherMiddleName = "";
}
