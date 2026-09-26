package com.pds.model;

/** Workflow status of a Personal Data Sheet record. */
public enum RecordStatus {
    ONGOING("Ongoing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;
    RecordStatus(String label) { this.label = label; }
    public String label() { return label; }

    @Override public String toString() { return label; }

    public static RecordStatus fromLabel(String s) {
        for (RecordStatus r : values()) if (r.label.equalsIgnoreCase(s) || r.name().equalsIgnoreCase(s)) return r;
        return ONGOING;
    }
}
