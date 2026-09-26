package com.pds.ui.sections;

import com.pds.model.VoluntaryWorkEntry;
import com.pds.ui.editors.DynamicTablePanel;
import com.pds.ui.editors.DynamicTablePanel.RowData;
import com.pds.ui.editors.DynamicTablePanel.Attachment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VoluntaryWorkPanel extends JPanel {

    private final DynamicTablePanel table = new DynamicTablePanel(new String[]{
            "Name & Address of Organization", "From", "To", "No. of Hours", "Position/Nature of Work"}, true);

    public VoluntaryWorkPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("VI. Voluntary Work / Involvement"));
        table.setColumnWidths(320, 80, 80, 80, 200);
        add(table, BorderLayout.CENTER);
    }

    public void loadFrom(List<VoluntaryWorkEntry> list) {
        List<RowData> rows = new ArrayList<>();
        for (VoluntaryWorkEntry v : list) {
            Attachment a = new Attachment();
            a.fileName = v.attachmentFileName;
            a.base64 = v.attachmentBase64;
            rows.add(new RowData(new String[]{v.organizationNameAddress, v.dateFrom, v.dateTo,
                    v.numberOfHours, v.positionNatureOfWork}, a));
        }
        table.setRowsWithAttachments(rows);
    }

    public void saveTo(List<VoluntaryWorkEntry> list) {
        list.clear();
        for (RowData rd : table.getRowsWithAttachments()) {
            String[] r = rd.values;
            VoluntaryWorkEntry v = new VoluntaryWorkEntry();
            v.organizationNameAddress = r[0]; v.dateFrom = r[1]; v.dateTo = r[2];
            v.numberOfHours = r[3]; v.positionNatureOfWork = r[4];
            if (rd.attachment != null) {
                v.attachmentFileName = rd.attachment.fileName;
                v.attachmentBase64 = rd.attachment.base64;
            }
            list.add(v);
        }
    }
}
