package com.pds.model;

public class YesNoItem {
    public Boolean answer = null; // null = unanswered, true = Yes, false = No
    public String details = "";

    public String answerText() { return answer == null ? "" : (answer ? "Yes" : "No"); }
}
