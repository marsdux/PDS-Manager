package com.pds.ui;

import com.pds.model.*;
import com.pds.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class ReportsPanel extends JPanel {

    private final JTextArea area = new JTextArea();
    private static final DateTimeFormatter[] DOB_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    public ReportsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(area), BorderLayout.CENTER);

        JButton print = new JButton("Print Report");
        print.addActionListener(e -> doPrint());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(print);
        add(south, BorderLayout.SOUTH);
    }

    public void refresh(List<PdsRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("PERSONAL DATA SHEET - REPORT & SUMMARY\n");
        sb.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("=".repeat(70)).append("\n\n");

        // ---- Status breakdown ----
        Map<RecordStatus, Long> byStatus = new EnumMap<>(RecordStatus.class);
        for (RecordStatus s : RecordStatus.values()) byStatus.put(s, 0L);
        for (PdsRecord r : records) byStatus.merge(r.status, 1L, Long::sum);
        sb.append("TOTAL RECORDS: ").append(records.size()).append("\n");
        for (RecordStatus s : RecordStatus.values()) {
            sb.append(String.format("  %-12s: %d%n", s.label(), byStatus.get(s)));
        }

        // ---- Demographics ----
        sb.append("\n--- DEMOGRAPHICS ---\n");
        sb.append("By Sex:\n");
        appendCounts(sb, records, r -> blankAs(r.personalInfo.sexAtBirth, "Unspecified"));
        sb.append("By Civil Status:\n");
        appendCounts(sb, records, r -> blankAs(r.personalInfo.civilStatus, "Unspecified"));
        sb.append("By Blood Type:\n");
        appendCounts(sb, records, r -> blankAs(r.personalInfo.bloodType, "Unspecified"));

        List<Integer> ages = new ArrayList<>();
        for (PdsRecord r : records) parseAge(r.personalInfo.dateOfBirth).ifPresent(ages::add);
        if (!ages.isEmpty()) {
            double avgAge = ages.stream().mapToInt(Integer::intValue).average().orElse(0);
            sb.append(String.format("Average age: %.1f (from %d of %d records with a parseable date of birth)%n",
                    avgAge, ages.size(), records.size()));
            sb.append("Age brackets:\n");
            Map<String, Long> brackets = new TreeMap<>();
            for (int age : ages) {
                String bracket = (age / 10 * 10) + "s";
                brackets.merge(bracket, 1L, Long::sum);
            }
            for (var e : brackets.entrySet()) sb.append(String.format("  %-14s: %d%n", e.getKey(), e.getValue()));
        } else {
            sb.append("Average age: n/a (no parseable dates of birth yet - expected format dd/mm/yyyy)\n");
        }

        long dualCitizens = records.stream().filter(r -> r.personalInfo.citizenDual).count();
        sb.append("Dual citizenship holders: ").append(dualCitizens).append("\n");

        // ---- Education ----
        sb.append("\n--- EDUCATION ---\n");
        double avgEdu = avg(records, r -> (double) r.education.size());
        sb.append(String.format("Average education entries per record: %.1f%n", avgEdu));
        sb.append("Highest educational attainment reached (per record):\n");
        appendCounts(sb, records, this::highestEducationLevel);

        // ---- Eligibility ----
        sb.append("\n--- CIVIL SERVICE ELIGIBILITY ---\n");
        double avgElig = avg(records, r -> (double) r.eligibility.size());
        sb.append(String.format("Average eligibility entries per record: %.1f%n", avgElig));
        long noEligibility = records.stream().filter(r -> r.eligibility.isEmpty()).count();
        sb.append("Records with NO eligibility on file: ").append(noEligibility).append("\n");
        sb.append("Most common eligibility types:\n");
        Map<String, Long> eligTypes = new TreeMap<>();
        for (PdsRecord r : records) {
            for (EligibilityEntry e : r.eligibility) {
                if (!e.careerServiceEligibility.isBlank()) eligTypes.merge(e.careerServiceEligibility.trim(), 1L, Long::sum);
            }
        }
        appendTopN(sb, eligTypes, 10);

        // ---- Employment ----
        sb.append("\n--- EMPLOYMENT (most recent work experience entry per record) ---\n");
        appendCounts(sb, records, this::latestAppointmentStatus);
        long anyGovtService = records.stream()
                .filter(r -> r.workExperience.stream().anyMatch(w -> "Y".equalsIgnoreCase(w.govtService.trim())))
                .count();
        sb.append("Records with at least one government-service work entry: ").append(anyGovtService).append("\n");
        sb.append("Most common departments/agencies (latest entry):\n");
        Map<String, Long> depts = new TreeMap<>();
        for (PdsRecord r : records) {
            if (!r.workExperience.isEmpty()) {
                String d = r.workExperience.get(0).departmentAgency.trim();
                if (!d.isBlank()) depts.merge(d, 1L, Long::sum);
            }
        }
        appendTopN(sb, depts, 10);

        // ---- Learning & Development ----
        sb.append("\n--- LEARNING & DEVELOPMENT ---\n");
        int totalHours = 0, parsedEntries = 0;
        for (PdsRecord r : records) {
            for (LearningDevelopmentEntry ld : r.learningDevelopment) {
                try { totalHours += Integer.parseInt(ld.numberOfHours.trim()); parsedEntries++; } catch (Exception ignored) {}
            }
        }
        sb.append("Total L&D hours logged across all records: ").append(totalHours)
                .append(" (from ").append(parsedEntries).append(" entries with a numeric hour count)\n");

        // ---- Attachments / proof documents ----
        sb.append("\n--- SUPPORTING DOCUMENTS ---\n");
        long withPhoto = records.stream().filter(r -> r.personalInfo.photoBase64 != null && !r.personalInfo.photoBase64.isBlank()).count();
        sb.append("Records with a 2x2 photo attached: ").append(withPhoto).append(" of ").append(records.size()).append("\n");
        int totalProofs = 0;
        for (PdsRecord r : records) {
            totalProofs += countAttached(r.education, e -> e.attachmentBase64);
            totalProofs += countAttached(r.eligibility, e -> e.attachmentBase64);
            totalProofs += countAttached(r.workExperience, e -> e.attachmentBase64);
            totalProofs += countAttached(r.learningDevelopment, e -> e.attachmentBase64);
            totalProofs += countAttached(r.voluntaryWork, e -> e.attachmentBase64);
            totalProofs += countAttachedText(r.otherInfo.nonAcademicDistinctions);
            totalProofs += countAttachedText(r.otherInfo.membershipAssociations);
        }
        sb.append("Total scanned proof documents attached (education/eligibility/work/L&D/voluntary/distinctions/memberships): ")
                .append(totalProofs).append("\n");

        // ---- Data quality ----
        sb.append("\n--- DATA QUALITY ---\n");
        long completedWithIssues = records.stream()
                .filter(r -> r.status == RecordStatus.COMPLETED)
                .filter(r -> !Validator.validateForCompletion(r).isEmpty())
                .count();
        sb.append("Records marked Completed but still missing required fields: ").append(completedWithIssues).append("\n");
        long missingEmail = records.stream().filter(r -> r.personalInfo.emailAddress.isBlank()).count();
        long missingMobile = records.stream().filter(r -> r.personalInfo.mobileNo.isBlank()).count();
        sb.append("Records missing an email address: ").append(missingEmail).append("\n");
        sb.append("Records missing a mobile number: ").append(missingMobile).append("\n");

        // ---- Full listing ----
        sb.append("\n--- RECORDS LIST ---\n");
        sb.append(String.format("  %-30s %-12s %-25s%n", "Name", "Status", "Last Updated"));
        for (PdsRecord r : records) {
            sb.append(String.format("  %-30s %-12s %-25s%n", trim(r.displayName(), 30), r.status, r.updatedAt));
        }

        area.setText(sb.toString());
        area.setCaretPosition(0);
    }

    // ---------------- helpers ----------------

    private String highestEducationLevel(PdsRecord r) {
        String[] order = {"GRADUATE STUDIES", "COLLEGE", "VOCATIONAL", "SECONDARY", "ELEMENTARY"};
        Set<String> levels = new HashSet<>();
        for (EducationEntry e : r.education) levels.add(e.level.trim().toUpperCase(Locale.ROOT));
        for (String o : order) if (levels.contains(o)) return o;
        return r.education.isEmpty() ? "Unspecified" : "Other";
    }

    private String latestAppointmentStatus(PdsRecord r) {
        if (r.workExperience.isEmpty()) return "Unspecified";
        String s = r.workExperience.get(0).statusOfAppointment;
        return blankAs(s, "Unspecified");
    }

    private Optional<Integer> parseAge(String dob) {
        if (dob == null || dob.isBlank()) return Optional.empty();
        for (DateTimeFormatter fmt : DOB_FORMATS) {
            try {
                LocalDate d = LocalDate.parse(dob.trim(), fmt);
                int age = Period.between(d, LocalDate.now()).getYears();
                if (age >= 0 && age < 130) return Optional.of(age);
            } catch (Exception ignored) { /* try next format */ }
        }
        return Optional.empty();
    }

    private <T> int countAttached(List<T> list, Function<T, String> attachmentGetter) {
        int n = 0;
        for (T item : list) {
            String b64 = attachmentGetter.apply(item);
            if (b64 != null && !b64.isBlank()) n++;
        }
        return n;
    }

    private int countAttachedText(List<AttachableText> list) {
        int n = 0;
        for (AttachableText t : list) if (t.attachmentBase64 != null && !t.attachmentBase64.isBlank()) n++;
        return n;
    }

    private double avg(List<PdsRecord> records, java.util.function.ToDoubleFunction<PdsRecord> f) {
        return records.isEmpty() ? 0 : records.stream().mapToDouble(f).average().orElse(0);
    }

    private void appendCounts(StringBuilder sb, List<PdsRecord> records, Function<PdsRecord, String> key) {
        Map<String, Long> counts = new TreeMap<>();
        for (PdsRecord r : records) counts.merge(key.apply(r), 1L, Long::sum);
        for (var e : counts.entrySet()) sb.append(String.format("  %-16s: %d%n", e.getKey(), e.getValue()));
    }

    private void appendTopN(StringBuilder sb, Map<String, Long> counts, int n) {
        counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(n)
                .forEach(e -> sb.append(String.format("  %-30s: %d%n", trim(e.getKey(), 30), e.getValue())));
        if (counts.isEmpty()) sb.append("  (none on file yet)\n");
    }

    private static String blankAs(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }

    private String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 3) + "..."; }

    private void doPrint() {
        try {
            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            job.setJobName("PDS Report & Summary");
            job.setPrintable((graphics, pf, pageIndex) -> {
                String[] lines = area.getText().split("\n");
                int linesPerPage = (int) (pf.getImageableHeight() / 11) - 2;
                int start = pageIndex * linesPerPage;
                if (start >= lines.length) return java.awt.print.Printable.NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) graphics;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
                int y = 11;
                for (int i = start; i < Math.min(start + linesPerPage, lines.length); i++) {
                    g2.drawString(lines[i], 0, y);
                    y += 11;
                }
                return java.awt.print.Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) job.print();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
