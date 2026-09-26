package com.pds.ui.sections;

import com.pds.model.EligibilityEntry;
import com.pds.ui.editors.DynamicTablePanel;
import com.pds.ui.editors.DynamicTablePanel.RowData;
import com.pds.ui.editors.DynamicTablePanel.Attachment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EligibilityPanel extends JPanel {

    private final DynamicTablePanel table = new DynamicTablePanel(new String[]{
            "Career Service / RA 1080 / Board / Bar / Barangay / Driver's License",
            "Rating", "Date of Exam/Conferment", "Place of Exam/Conferment", "License No.", "License Validity"}, true);

    public EligibilityPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("IV. Civil Service Eligibility"));
        table.setColumnWidths(260, 70, 130, 200, 110, 110);
        add(table, BorderLayout.CENTER);
    }

    public void loadFrom(List<EligibilityEntry> list) {
        List<RowData> rows = new ArrayList<>();
        for (EligibilityEntry e : list) {
            Attachment a = new Attachment();
            a.fileName = e.attachmentFileName;
            a.base64 = e.attachmentBase64;
            rows.add(new RowData(new String[]{e.careerServiceEligibility, e.rating,
                    e.dateOfExamConferment, e.placeOfExamConferment, e.licenseNumber, e.licenseValidity}, a));
        }
        table.setRowsWithAttachments(rows);
    }

    public void saveTo(List<EligibilityEntry> list) {
        list.clear();
        for (RowData rd : table.getRowsWithAttachments()) {
            String[] r = rd.values;
            EligibilityEntry e = new EligibilityEntry();
            e.careerServiceEligibility = r[0]; e.rating = r[1]; e.dateOfExamConferment = r[2];
            e.placeOfExamConferment = r[3]; e.licenseNumber = r[4]; e.licenseValidity = r[5];
            if (rd.attachment != null) {
                e.attachmentFileName = rd.attachment.fileName;
                e.attachmentBase64 = rd.attachment.base64;
            }
            list.add(e);
        }
    }
}
