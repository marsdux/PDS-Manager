package com.pds.model;

public class Child {
    public String fullName = "";
    public String dateOfBirth = ""; // dd/mm/yyyy as text, validated at UI level
    public Child() {}
    public Child(String fullName, String dob) { this.fullName = fullName; this.dateOfBirth = dob; }
}
