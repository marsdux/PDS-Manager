package com.pds.util;

import com.pds.model.PdsRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lightweight CSV helper - no external library. Handles quoting/escaping per RFC 4180. */
public final class CsvUtil {

    private CsvUtil() {}

    private static final String[] HEADERS = {
        "ID", "Status", "Surname", "First Name", "Middle Name", "Date of Birth",
        "Sex", "Civil Status", "Mobile No.", "Email", "Position Title (latest)",
        "Department/Agency (latest)", "Remarks", "Updated At"
    };

    public static void exportSummary(Path file, List<PdsRecord> records) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\r\n");
        for (PdsRecord r : records) {
            String posTitle = "", dept = "";
            if (!r.workExperience.isEmpty()) {
                var w = r.workExperience.get(0);
                posTitle = w.positionTitle;
                dept = w.departmentAgency;
            }
            List<String> row = new ArrayList<>();
            row.add(r.id);
            row.add(r.status.name());
            row.add(r.personalInfo.surname);
            row.add(r.personalInfo.firstName);
            row.add(r.personalInfo.middleName);
            row.add(r.personalInfo.dateOfBirth);
            row.add(r.personalInfo.sexAtBirth);
            row.add(r.personalInfo.civilStatus);
            row.add(r.personalInfo.mobileNo);
            row.add(r.personalInfo.emailAddress);
            row.add(posTitle);
            row.add(dept);
            row.add(r.remarks);
            row.add(r.updatedAt);
            sb.append(toCsvLine(row)).append("\r\n");
        }
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String toCsvLine(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(values.get(i)));
        }
        return sb.toString();
    }

    private static String quote(String v) {
        if (v == null) v = "";
        boolean needsQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String escaped = v.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }

    /** Very small RFC-4180 reader sufficient for re-importing our own export format. */
    public static List<List<String>> readCsv(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else inQuotes = false;
                } else field.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { row.add(field.toString()); field.setLength(0); }
                else if (c == '\r') { /* skip */ }
                else if (c == '\n') { row.add(field.toString()); field.setLength(0); rows.add(row); row = new ArrayList<>(); }
                else field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) { row.add(field.toString()); rows.add(row); }
        return rows;
    }
}
