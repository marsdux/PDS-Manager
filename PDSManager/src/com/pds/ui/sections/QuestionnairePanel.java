package com.pds.ui.sections;

import com.pds.model.Questionnaire;
import com.pds.model.ReferenceEntry;
import com.pds.model.YesNoItem;
import com.pds.ui.FormUtil;
import com.pds.ui.editors.DynamicTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionnairePanel extends JScrollPane {

    /** One "question ... [Yes][No]  details:____" line. */
    private static class QRow {
        final JRadioButton yes = new JRadioButton("Yes");
        final JRadioButton no = new JRadioButton("No");
        final JTextField details = FormUtil.tf();
        QRow() {
            ButtonGroup g = new ButtonGroup();
            g.add(yes); g.add(no);
        }
        JPanel radios() {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            p.add(yes); p.add(no);
            return p;
        }
        void load(YesNoItem item) {
            if (Boolean.TRUE.equals(item.answer)) yes.setSelected(true);
            else if (Boolean.FALSE.equals(item.answer)) no.setSelected(true);
            details.setText(item.details);
        }
        void save(YesNoItem item) {
            item.answer = yes.isSelected() ? Boolean.TRUE : no.isSelected() ? Boolean.FALSE : null;
            item.details = details.getText().trim();
        }
    }

    private final QRow q34a = new QRow(), q34b = new QRow(), q35a = new QRow(), q35b = new QRow(),
            q36 = new QRow(), q37 = new QRow(), q38a = new QRow(), q38b = new QRow(), q39 = new QRow(),
            q40a = new QRow(), q40b = new QRow(), q40c = new QRow();
    private final JTextField dateFiled = FormUtil.tf(), statusOfCase = FormUtil.tf(),
            idNo40b = FormUtil.tf(), idNo40c = FormUtil.tf(),
            govtIssuedId = FormUtil.tf(), idLicenseNo = FormUtil.tf(), dateOfIssuance = FormUtil.tf(),
            placeOfIssuance = FormUtil.tf(), dateAccomplished = FormUtil.tf();

    private final DynamicTablePanel references = new DynamicTablePanel(
            new String[]{"Name", "Office/Residential Address", "Contact No. and/or Email"});

    public QuestionnairePanel() {
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        setViewportView(content);
        getVerticalScrollBar().setUnitIncrement(16);

        content.add(questionsSection());
        content.add(referencesSection());
        content.add(idSection());
    }

    private JPanel questionsSection() {
        var p = FormUtil.section("Background Questionnaire (34-40)");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = q(p, gbc, row, "34a. Related within the 3rd degree to appointing/recommending authority?", q34a);
        row = q(p, gbc, row, "34b. Related within the 4th degree (LGU career employees)?", q34b);
        row = q(p, gbc, row, "35a. Ever found guilty of an administrative offense?", q35a);
        row = q(p, gbc, row, "35b. Ever criminally charged before any court?", q35b);
        row = FormUtil.addRow(p, gbc, row, "  Date Filed", dateFiled, "  Status of Case", statusOfCase);
        row = q(p, gbc, row, "36. Ever convicted of any crime or violation of law/decree/ordinance?", q36);
        row = q(p, gbc, row, "37. Ever separated from service (resignation, retirement, dismissal, etc.)?", q37);
        row = q(p, gbc, row, "38a. Ever a candidate in a national/local election (excl. Barangay)?", q38a);
        row = q(p, gbc, row, "38b. Resigned during 3-month period before last election to campaign?", q38b);
        row = q(p, gbc, row, "39. Acquired immigrant/permanent resident status of another country?", q39);
        row = q(p, gbc, row, "40a. Member of any indigenous group?", q40a);
        row = q(p, gbc, row, "40b. Person with disability?", q40b, false);
        row = FormUtil.addRow(p, gbc, row, "  If yes, PWD ID No.", idNo40b, "", new JLabel());
        row = q(p, gbc, row, "40c. Solo parent?", q40c, false);
        row = FormUtil.addRow(p, gbc, row, "  If yes, Solo Parent ID No.", idNo40c, "", new JLabel());
        return p;
    }

    private int q(JPanel p, GridBagConstraints gbc, int row, String label, QRow qr) {
        return q(p, gbc, row, label, qr, true);
    }

    private int q(JPanel p, GridBagConstraints gbc, int row, String label, QRow qr, boolean withDetails) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 1; gbc.gridwidth = 3;
        p.add(new JLabel("<html><body style='width:520px'>" + label + "</body></html>"), gbc);
        gbc.gridx = 3; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        p.add(qr.radios(), gbc);
        row++;
        if (withDetails) {
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
            p.add(new JLabel("  If yes, give details"), gbc);
            gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 3; gbc.weightx = 1;
            p.add(qr.details, gbc);
            gbc.gridwidth = 1;
            row++;
        }
        return row;
    }

    private JPanel referencesSection() {
        JPanel p = FormUtil.section("41. References (not related by consanguinity or affinity)");
        p.setLayout(new BorderLayout());
        references.setColumnWidths(180, 260, 160);
        p.add(references, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(600, 200));
        return p;
    }

    private JPanel idSection() {
        var p = FormUtil.section("42. Government Issued ID");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "Type of ID", govtIssuedId, "ID/License/Passport No.", idLicenseNo);
        row = FormUtil.addRow(p, gbc, row, "Date of Issuance", dateOfIssuance, "Place of Issuance", placeOfIssuance);
        row = FormUtil.addRow(p, gbc, row, "Date Accomplished", dateAccomplished, "", new JLabel());
        return p;
    }

    public void loadFrom(Questionnaire q) {
        q34a.load(q.q34a3rdDegree); q34b.load(q.q34b4thDegree);
        q35a.load(q.q35aAdminOffense); q35b.load(q.q35bCriminallyCharged);
        dateFiled.setText(q.q35bDateFiled); statusOfCase.setText(q.q35bStatusOfCase);
        q36.load(q.q36ConvictedCrime); q37.load(q.q37SeparatedFromService);
        q38a.load(q.q38aCandidateElection); q38b.load(q.q38bResignedToCampaign);
        q39.load(q.q39ImmigrantStatus);
        q40a.load(q.q40aIndigenousGroup); q40b.load(q.q40bPersonWithDisability); idNo40b.setText(q.q40bIdNo);
        q40c.load(q.q40cSoloParent); idNo40c.setText(q.q40cIdNo);
        govtIssuedId.setText(q.govtIssuedId); idLicenseNo.setText(q.idLicensePassportNo);
        dateOfIssuance.setText(q.dateOfIssuance); placeOfIssuance.setText(q.placeOfIssuance);
        dateAccomplished.setText(q.dateAccomplished);
        List<String[]> rows = new ArrayList<>();
        for (ReferenceEntry r : q.references) rows.add(new String[]{r.name, r.address, r.contactNoEmail});
        references.setRows(rows);
    }

    public void saveTo(Questionnaire q) {
        q34a.save(q.q34a3rdDegree); q34b.save(q.q34b4thDegree);
        q35a.save(q.q35aAdminOffense); q35b.save(q.q35bCriminallyCharged);
        q.q35bDateFiled = dateFiled.getText().trim(); q.q35bStatusOfCase = statusOfCase.getText().trim();
        q36.save(q.q36ConvictedCrime); q37.save(q.q37SeparatedFromService);
        q38a.save(q.q38aCandidateElection); q38b.save(q.q38bResignedToCampaign);
        q39.save(q.q39ImmigrantStatus);
        q40a.save(q.q40aIndigenousGroup); q40b.save(q.q40bPersonWithDisability); q.q40bIdNo = idNo40b.getText().trim();
        q40c.save(q.q40cSoloParent); q.q40cIdNo = idNo40c.getText().trim();
        q.govtIssuedId = govtIssuedId.getText().trim(); q.idLicensePassportNo = idLicenseNo.getText().trim();
        q.dateOfIssuance = dateOfIssuance.getText().trim(); q.placeOfIssuance = placeOfIssuance.getText().trim();
        q.dateAccomplished = dateAccomplished.getText().trim();
        q.references.clear();
        for (String[] r : references.getRows()) {
            ReferenceEntry ref = new ReferenceEntry();
            ref.name = r[0]; ref.address = r[1]; ref.contactNoEmail = r[2];
            q.references.add(ref);
        }
    }
}
