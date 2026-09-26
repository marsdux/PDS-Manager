package com.pds.ui.sections;

import com.pds.model.AttachableText;
import com.pds.model.OtherInfo;
import com.pds.ui.editors.DynamicTablePanel;
import com.pds.ui.editors.DynamicTablePanel.RowData;
import com.pds.ui.editors.DynamicTablePanel.Attachment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OtherInfoPanel extends JPanel {

    private final DynamicTablePanel skills = new DynamicTablePanel(new String[]{"Special Skill / Hobby"});
    private final DynamicTablePanel distinctions = new DynamicTablePanel(
            new String[]{"Non-Academic Distinction / Recognition"}, true);
    private final DynamicTablePanel memberships = new DynamicTablePanel(
            new String[]{"Membership in Association/Organization"}, true);

    public OtherInfoPanel() {
        super(new GridLayout(1, 3, 6, 0));
        setBorder(BorderFactory.createTitledBorder("VIII. Other Information"));
        add(wrap("31. Special Skills and Hobbies", skills));
        add(wrap("32. Non-Academic Distinctions/Recognition", distinctions));
        add(wrap("33. Membership in Association/Organization", memberships));
    }

    private JPanel wrap(String title, DynamicTablePanel t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(t, BorderLayout.CENTER);
        return p;
    }

    public void loadFrom(OtherInfo o) {
        List<String[]> skillRows = new ArrayList<>();
        for (String v : o.specialSkillsHobbies) skillRows.add(new String[]{v});
        skills.setRows(skillRows);

        distinctions.setRowsWithAttachments(toRowData(o.nonAcademicDistinctions));
        memberships.setRowsWithAttachments(toRowData(o.membershipAssociations));
    }

    public void saveTo(OtherInfo o) {
        List<String> skillList = new ArrayList<>();
        for (String[] r : skills.getRows()) skillList.add(r[0]);
        o.specialSkillsHobbies = skillList;

        o.nonAcademicDistinctions = fromRowData(distinctions.getRowsWithAttachments());
        o.membershipAssociations = fromRowData(memberships.getRowsWithAttachments());
    }

    private List<RowData> toRowData(List<AttachableText> items) {
        List<RowData> rows = new ArrayList<>();
        for (AttachableText it : items) {
            Attachment a = new Attachment();
            a.fileName = it.attachmentFileName;
            a.base64 = it.attachmentBase64;
            rows.add(new RowData(new String[]{it.text}, a));
        }
        return rows;
    }

    private List<AttachableText> fromRowData(List<RowData> rows) {
        List<AttachableText> out = new ArrayList<>();
        for (RowData rd : rows) {
            AttachableText it = new AttachableText(rd.values[0]);
            if (rd.attachment != null) {
                it.attachmentFileName = rd.attachment.fileName;
                it.attachmentBase64 = rd.attachment.base64;
            }
            out.add(it);
        }
        return out;
    }
}
