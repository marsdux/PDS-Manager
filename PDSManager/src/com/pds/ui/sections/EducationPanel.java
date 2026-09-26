package com.pds.ui.sections;

import com.pds.model.EducationEntry;
import com.pds.ui.editors.DynamicTablePanel;
import com.pds.ui.editors.DynamicTablePanel.RowData;
import com.pds.ui.editors.DynamicTablePanel.Attachment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EducationPanel extends JPanel {

    private final DynamicTablePanel table = new DynamicTablePanel(new String[]{
            "Level", "Name of School", "Basic Ed./Degree/Course", "From", "To",
            "Highest Level/Units Earned", "Year Graduated", "Scholarship/Honors"}, true);

    public EducationPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("III. Educational Background"));
        table.setColumnWidths(110, 200, 180, 70, 70, 160, 90, 160);
        add(table, BorderLayout.CENTER);
        JLabel hint = new JLabel("  Tip: type ELEMENTARY / SECONDARY / VOCATIONAL / COLLEGE / GRADUATE STUDIES in Level");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        add(hint, BorderLayout.SOUTH);
    }

    public void loadFrom(List<EducationEntry> list) {
        List<RowData> rows = new ArrayList<>();
        for (EducationEntry e : list) {
            Attachment a = new Attachment();
            a.fileName = e.attachmentFileName;
            a.base64 = e.attachmentBase64;
            rows.add(new RowData(new String[]{e.level, e.schoolName, e.degreeCourse,
                    e.periodFrom, e.periodTo, e.highestLevelUnits, e.yearGraduated, e.honors}, a));
        }
        table.setRowsWithAttachments(rows);
    }

    public void saveTo(List<EducationEntry> list) {
        list.clear();
        for (RowData rd : table.getRowsWithAttachments()) {
            String[] r = rd.values;
            EducationEntry e = new EducationEntry(r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7]);
            if (rd.attachment != null) {
                e.attachmentFileName = rd.attachment.fileName;
                e.attachmentBase64 = rd.attachment.base64;
            }
            list.add(e);
        }
    }
}
