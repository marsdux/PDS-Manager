package com.pds.ui.sections;

import com.pds.model.LearningDevelopmentEntry;
import com.pds.ui.editors.DynamicTablePanel;
import com.pds.ui.editors.DynamicTablePanel.RowData;
import com.pds.ui.editors.DynamicTablePanel.Attachment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LearningDevelopmentPanel extends JPanel {

    private final DynamicTablePanel table = new DynamicTablePanel(new String[]{
            "Title of L&D Intervention/Training Program", "From", "To", "No. of Hours", "Type", "Conducted/Sponsored By"}, true);

    public LearningDevelopmentPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("VII. Learning & Development Interventions / Training Programs"));
        table.setColumnWidths(300, 80, 80, 80, 130, 200);
        add(table, BorderLayout.CENTER);
    }

    public void loadFrom(List<LearningDevelopmentEntry> list) {
        List<RowData> rows = new ArrayList<>();
        for (LearningDevelopmentEntry e : list) {
            Attachment a = new Attachment();
            a.fileName = e.attachmentFileName;
            a.base64 = e.attachmentBase64;
            rows.add(new RowData(new String[]{e.title, e.dateFrom, e.dateTo,
                    e.numberOfHours, e.type, e.conductedSponsoredBy}, a));
        }
        table.setRowsWithAttachments(rows);
    }

    public void saveTo(List<LearningDevelopmentEntry> list) {
        list.clear();
        for (RowData rd : table.getRowsWithAttachments()) {
            String[] r = rd.values;
            LearningDevelopmentEntry e = new LearningDevelopmentEntry();
            e.title = r[0]; e.dateFrom = r[1]; e.dateTo = r[2]; e.numberOfHours = r[3];
            e.type = r[4]; e.conductedSponsoredBy = r[5];
            if (rd.attachment != null) {
                e.attachmentFileName = rd.attachment.fileName;
                e.attachmentBase64 = rd.attachment.base64;
            }
            list.add(e);
        }
    }
}
