package com.pds.ui;

import com.pds.model.*;
import com.pds.util.AttachmentIO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AttachmentsPanel extends JPanel {

    private static class Ref {
        String section, item, fileName, base64;
    }

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Section", "Item", "File Name", "Approx. Size"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final List<Ref> refs = new ArrayList<>();
    private final JLabel countLabel = new JLabel(" ");

    public AttachmentsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton view = new JButton("View / Open Selected");
        JButton print = new JButton("Print Selected");
        view.addActionListener(e -> withSelected(r -> AttachmentIO.open(this, r.fileName, r.base64)));
        print.addActionListener(e -> withSelected(r -> AttachmentIO.print(this, r.fileName, r.base64)));

        JPanel south = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(view);
        buttons.add(print);
        south.add(buttons, BorderLayout.WEST);
        south.add(countLabel, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        JLabel hint = new JLabel("  Most scanned proofs and the e-notary file are typically PDFs - View/Print opens them in your system's default PDF viewer.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        add(hint, BorderLayout.NORTH);
    }

    private void withSelected(java.util.function.Consumer<Ref> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an attachment first.", "Nothing Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        action.accept(refs.get(table.convertRowIndexToModel(row)));
    }

    /** Rebuilds the list from the record's current in-memory state (call after syncing UI -> model, e.g. on tab switch). */
    public void refresh(PdsRecord r) {
        model.setRowCount(0);
        refs.clear();
        if (r == null) { countLabel.setText(" "); return; }

        addIfPresent("I. Personal Info", "2x2 ID Photo", "photo.jpg", r.personalInfo.photoBase64);

        for (int i = 0; i < r.education.size(); i++) {
            EducationEntry e = r.education.get(i);
            addIfPresent("III. Education", "Row " + (i + 1) + ": " + blankAs(e.schoolName, "(untitled)"), e.attachmentFileName, e.attachmentBase64);
        }
        for (int i = 0; i < r.eligibility.size(); i++) {
            EligibilityEntry e = r.eligibility.get(i);
            addIfPresent("IV. Eligibility", "Row " + (i + 1) + ": " + blankAs(e.careerServiceEligibility, "(untitled)"), e.attachmentFileName, e.attachmentBase64);
        }
        for (int i = 0; i < r.workExperience.size(); i++) {
            WorkExperienceEntry w = r.workExperience.get(i);
            addIfPresent("V. Work Experience", "Row " + (i + 1) + ": " + blankAs(w.positionTitle, "(untitled)"), w.attachmentFileName, w.attachmentBase64);
        }
        for (int i = 0; i < r.voluntaryWork.size(); i++) {
            VoluntaryWorkEntry v = r.voluntaryWork.get(i);
            addIfPresent("VI. Voluntary Work", "Row " + (i + 1) + ": " + blankAs(v.organizationNameAddress, "(untitled)"), v.attachmentFileName, v.attachmentBase64);
        }
        for (int i = 0; i < r.learningDevelopment.size(); i++) {
            LearningDevelopmentEntry ld = r.learningDevelopment.get(i);
            addIfPresent("VII. L&D", "Row " + (i + 1) + ": " + blankAs(ld.title, "(untitled)"), ld.attachmentFileName, ld.attachmentBase64);
        }
        for (int i = 0; i < r.otherInfo.nonAcademicDistinctions.size(); i++) {
            AttachableText t = r.otherInfo.nonAcademicDistinctions.get(i);
            addIfPresent("VIII.32 Distinctions", "Row " + (i + 1) + ": " + blankAs(t.text, "(untitled)"), t.attachmentFileName, t.attachmentBase64);
        }
        for (int i = 0; i < r.otherInfo.membershipAssociations.size(); i++) {
            AttachableText t = r.otherInfo.membershipAssociations.get(i);
            addIfPresent("VIII.33 Memberships", "Row " + (i + 1) + ": " + blankAs(t.text, "(untitled)"), t.attachmentFileName, t.attachmentBase64);
        }
        addIfPresent("Notarization", "Electronic notary document", r.notarization.attachmentFileName, r.notarization.attachmentBase64);

        countLabel.setText(refs.size() + " attachment(s) on this record");
    }

    private void addIfPresent(String section, String item, String fileName, String base64) {
        if (base64 == null || base64.isBlank()) return;
        Ref ref = new Ref();
        ref.section = section;
        ref.item = item;
        ref.fileName = (fileName == null || fileName.isBlank()) ? "(unnamed file)" : fileName;
        ref.base64 = base64;
        refs.add(ref);
        long approxBytes = (long) (base64.length() * 0.75); // Base64 -> raw byte size estimate
        String size = approxBytes > 1024 * 1024
                ? String.format("%.1f MB", approxBytes / (1024.0 * 1024))
                : String.format("%.0f KB", Math.max(1, approxBytes / 1024.0));
        model.addRow(new Object[]{section, item, ref.fileName, size});
    }

    private static String blankAs(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }
}
