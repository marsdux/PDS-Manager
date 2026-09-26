package com.pds.ui.sections;

import com.pds.model.NotarizationInfo;
import com.pds.ui.FormUtil;
import com.pds.util.AttachmentIO;

import javax.swing.*;
import java.awt.*;

public class NotarizationPanel extends JScrollPane {

    private final JTextField subscribedDate = FormUtil.tf(), subscribedPlace = FormUtil.tf(),
            notaryName = FormUtil.tf(), commissionNo = FormUtil.tf(), ptrNo = FormUtil.tf(),
            ibpNo = FormUtil.tf(), rollNo = FormUtil.tf(), docNo = FormUtil.tf(), pageNo = FormUtil.tf(),
            bookNo = FormUtil.tf(), seriesOf = FormUtil.tf();

    private final JLabel status = new JLabel("No electronic notary document attached.");
    private String attachmentFileName = "";
    private String attachmentBase64 = "";

    public NotarizationPanel() {
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        setViewportView(content);
        getVerticalScrollBar().setUnitIncrement(16);

        content.add(intro());
        content.add(fieldsSection());
        content.add(attachmentSection());
    }

    private JPanel intro() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JLabel l = new JLabel("<html>Once your PDS is printed and physically signed, your lawyer/notary public completes the "
                + "jurat below and provides you the notarized copy (usually as a scanned/e-signed PDF). Record the "
                + "notary's details here and attach that file.</html>");
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private JPanel fieldsSection() {
        var p = FormUtil.section("Notarial Acknowledgment / Jurat");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "Subscribed & Sworn Date", subscribedDate, "Subscribed & Sworn At (City/Municipality)", subscribedPlace);
        row = FormUtil.addRow(p, gbc, row, "Notary Public Name", notaryName, "Commission No.", commissionNo);
        row = FormUtil.addRow(p, gbc, row, "PTR No.", ptrNo, "IBP No.", ibpNo);
        row = FormUtil.addRow(p, gbc, row, "Roll of Attorneys No.", rollNo, "", new JLabel());
        row = FormUtil.addRow(p, gbc, row, "Doc. No.", docNo, "Page No.", pageNo);
        row = FormUtil.addRow(p, gbc, row, "Book No.", bookNo, "Series of", seriesOf);
        return p;
    }

    private JPanel attachmentSection() {
        var p = FormUtil.section("Electronic Notary Document");
        JButton attach = new JButton("Attach...");
        JButton view = new JButton("View / Open");
        JButton print = new JButton("Print");
        JButton remove = new JButton("Remove");

        attach.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Attach electronic notary document");
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            AttachmentIO.Loaded loaded = AttachmentIO.readFile(this, chooser.getSelectedFile());
            if (loaded == null) return;
            attachmentFileName = loaded.fileName;
            attachmentBase64 = loaded.base64;
            updateStatus();
        });
        view.addActionListener(e -> AttachmentIO.open(this, attachmentFileName, attachmentBase64));
        print.addActionListener(e -> AttachmentIO.print(this, attachmentFileName, attachmentBase64));
        remove.addActionListener(e -> { attachmentFileName = ""; attachmentBase64 = ""; updateStatus(); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(attach);
        buttons.add(view);
        buttons.add(print);
        buttons.add(remove);

        GridBagConstraints gbc = FormUtil.baseConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        p.add(status, gbc);
        gbc.gridy = 1;
        p.add(buttons, gbc);
        return p;
    }

    private void updateStatus() {
        status.setText(attachmentBase64.isBlank() ? "No electronic notary document attached."
                : "Attached: " + attachmentFileName);
    }

    public void loadFrom(NotarizationInfo n) {
        subscribedDate.setText(n.subscribedDate); subscribedPlace.setText(n.subscribedPlace);
        notaryName.setText(n.notaryPublicName); commissionNo.setText(n.notaryCommissionNo);
        ptrNo.setText(n.notaryPtrNo); ibpNo.setText(n.notaryIbpNo); rollNo.setText(n.notaryRollNo);
        docNo.setText(n.docNo); pageNo.setText(n.pageNo); bookNo.setText(n.bookNo); seriesOf.setText(n.seriesOf);
        attachmentFileName = n.attachmentFileName == null ? "" : n.attachmentFileName;
        attachmentBase64 = n.attachmentBase64 == null ? "" : n.attachmentBase64;
        updateStatus();
    }

    public void saveTo(NotarizationInfo n) {
        n.subscribedDate = subscribedDate.getText().trim(); n.subscribedPlace = subscribedPlace.getText().trim();
        n.notaryPublicName = notaryName.getText().trim(); n.notaryCommissionNo = commissionNo.getText().trim();
        n.notaryPtrNo = ptrNo.getText().trim(); n.notaryIbpNo = ibpNo.getText().trim(); n.notaryRollNo = rollNo.getText().trim();
        n.docNo = docNo.getText().trim(); n.pageNo = pageNo.getText().trim(); n.bookNo = bookNo.getText().trim();
        n.seriesOf = seriesOf.getText().trim();
        n.attachmentFileName = attachmentFileName;
        n.attachmentBase64 = attachmentBase64;
    }
}
