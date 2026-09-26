package com.pds.ui.sections;

import com.pds.model.Child;
import com.pds.model.FamilyBackground;
import com.pds.ui.FormUtil;
import com.pds.ui.editors.DynamicTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FamilyBackgroundPanel extends JScrollPane {

    private final JTextField spouseSurname = FormUtil.tf(), spouseFirst = FormUtil.tf(), spouseMiddle = FormUtil.tf(),
            spouseExt = FormUtil.tf(), spouseOccupation = FormUtil.tf(), spouseEmployer = FormUtil.tf(),
            spouseBizAddr = FormUtil.tf(), spouseTel = FormUtil.tf(),
            fatherSurname = FormUtil.tf(), fatherFirst = FormUtil.tf(), fatherMiddle = FormUtil.tf(), fatherExt = FormUtil.tf(),
            motherSurname = FormUtil.tf(), motherFirst = FormUtil.tf(), motherMiddle = FormUtil.tf();

    private final DynamicTablePanel children = new DynamicTablePanel(new String[]{"Full Name", "Date of Birth (dd/mm/yyyy)"});

    public FamilyBackgroundPanel() {
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        setViewportView(content);
        getVerticalScrollBar().setUnitIncrement(16);

        content.add(spouseSection());
        content.add(childrenSection());
        content.add(parentsSection());
    }

    private JPanel spouseSection() {
        var p = FormUtil.section("22. Spouse");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "Surname", spouseSurname, "Name Extension", spouseExt);
        row = FormUtil.addRow(p, gbc, row, "First Name", spouseFirst, "Middle Name", spouseMiddle);
        row = FormUtil.addRow(p, gbc, row, "Occupation", spouseOccupation, "Employer/Business Name", spouseEmployer);
        row = FormUtil.addRow(p, gbc, row, "Business Address", spouseBizAddr, "Telephone No.", spouseTel);
        return p;
    }

    private JPanel childrenSection() {
        JPanel p = FormUtil.section("23. Name of Children (write full name and list all)");
        p.setLayout(new BorderLayout());
        children.setColumnWidths(300, 150);
        p.add(children, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(600, 220));
        return p;
    }

    private JPanel parentsSection() {
        var p = FormUtil.section("24-25. Father / Mother's Maiden Name");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "Father's Surname", fatherSurname, "Name Extension", fatherExt);
        row = FormUtil.addRow(p, gbc, row, "Father's First Name", fatherFirst, "Father's Middle Name", fatherMiddle);
        row = FormUtil.addRow(p, gbc, row, "Mother's Maiden Surname", motherSurname, "Mother's First Name", motherFirst);
        row = FormUtil.addRow(p, gbc, row, "Mother's Middle Name", motherMiddle, "", new JLabel());
        return p;
    }

    public void loadFrom(FamilyBackground fb) {
        spouseSurname.setText(fb.spouseSurname); spouseFirst.setText(fb.spouseFirstName);
        spouseMiddle.setText(fb.spouseMiddleName); spouseExt.setText(fb.spouseNameExtension);
        spouseOccupation.setText(fb.spouseOccupation); spouseEmployer.setText(fb.spouseEmployerBusinessName);
        spouseBizAddr.setText(fb.spouseBusinessAddress); spouseTel.setText(fb.spouseTelephoneNo);
        fatherSurname.setText(fb.fatherSurname); fatherFirst.setText(fb.fatherFirstName);
        fatherMiddle.setText(fb.fatherMiddleName); fatherExt.setText(fb.fatherNameExtension);
        motherSurname.setText(fb.motherMaidenSurname); motherFirst.setText(fb.motherFirstName);
        motherMiddle.setText(fb.motherMiddleName);
        List<String[]> rows = new ArrayList<>();
        for (Child c : fb.children) rows.add(new String[]{c.fullName, c.dateOfBirth});
        children.setRows(rows);
    }

    public void saveTo(FamilyBackground fb) {
        fb.spouseSurname = spouseSurname.getText().trim(); fb.spouseFirstName = spouseFirst.getText().trim();
        fb.spouseMiddleName = spouseMiddle.getText().trim(); fb.spouseNameExtension = spouseExt.getText().trim();
        fb.spouseOccupation = spouseOccupation.getText().trim(); fb.spouseEmployerBusinessName = spouseEmployer.getText().trim();
        fb.spouseBusinessAddress = spouseBizAddr.getText().trim(); fb.spouseTelephoneNo = spouseTel.getText().trim();
        fb.fatherSurname = fatherSurname.getText().trim(); fb.fatherFirstName = fatherFirst.getText().trim();
        fb.fatherMiddleName = fatherMiddle.getText().trim(); fb.fatherNameExtension = fatherExt.getText().trim();
        fb.motherMaidenSurname = motherSurname.getText().trim(); fb.motherFirstName = motherFirst.getText().trim();
        fb.motherMiddleName = motherMiddle.getText().trim();
        fb.children.clear();
        for (String[] row : children.getRows()) fb.children.add(new Child(row[0], row[1]));
    }
}
