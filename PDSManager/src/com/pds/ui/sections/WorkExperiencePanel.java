package com.pds.ui.sections;

import com.pds.model.WorkExperienceEntry;
import com.pds.ui.editors.DynamicTablePanel;
import com.pds.ui.editors.DynamicTablePanel.RowData;
import com.pds.ui.editors.DynamicTablePanel.Attachment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WorkExperiencePanel extends JPanel {

    private final DynamicTablePanel table = new DynamicTablePanel(new String[]{
            "From", "To", "Position Title", "Department/Agency/Office/Company",
            "Monthly Salary", "Salary/Job/Pay Grade", "Status of Appointment", "Gov't Service (Y/N)"}, true);

    public WorkExperiencePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("V. Work Experience (most recent first)"));
        table.setColumnWidths(80, 80, 180, 220, 90, 110, 110, 90);
        add(table, BorderLayout.CENTER);
    }

    public void loadFrom(List<WorkExperienceEntry> list) {
        List<RowData> rows = new ArrayList<>();
        for (WorkExperienceEntry w : list) {
            Attachment a = new Attachment();
            a.fileName = w.attachmentFileName;
            a.base64 = w.attachmentBase64;
            rows.add(new RowData(new String[]{w.dateFrom, w.dateTo, w.positionTitle,
                    w.departmentAgency, w.monthlySalary, w.salaryGrade, w.statusOfAppointment, w.govtService}, a));
        }
        table.setRowsWithAttachments(rows);
    }

    public void saveTo(List<WorkExperienceEntry> list) {
        list.clear();
        for (RowData rd : table.getRowsWithAttachments()) {
            String[] r = rd.values;
            WorkExperienceEntry w = new WorkExperienceEntry();
            w.dateFrom = r[0]; w.dateTo = r[1]; w.positionTitle = r[2]; w.departmentAgency = r[3];
            w.monthlySalary = r[4]; w.salaryGrade = r[5]; w.statusOfAppointment = r[6];
            w.govtService = r[7].isBlank() ? "N" : r[7];
            if (rd.attachment != null) {
                w.attachmentFileName = rd.attachment.fileName;
                w.attachmentBase64 = rd.attachment.base64;
            }
            list.add(w);
        }
    }
}
