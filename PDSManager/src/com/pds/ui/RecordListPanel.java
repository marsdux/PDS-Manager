package com.pds.ui;

import com.pds.model.PdsRecord;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordListPanel extends JPanel {

    private final DefaultListModel<PdsRecord> model = new DefaultListModel<>();
    private final JList<PdsRecord> list = new JList<>(model);
    private final JTextField search = new JTextField();
    private List<PdsRecord> allRecords = new ArrayList<>();

    public RecordListPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("PDS Records"));
        setPreferredSize(new Dimension(260, 100));

        search.putClientProperty("JTextField.placeholderText", "Search name / status / remarks...");
        search.getDocument().addDocumentListener((SimpleDocListener) e -> applyFilter());
        add(search, BorderLayout.NORTH);

        list.setCellRenderer(new RecordCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setRecords(List<PdsRecord> records) {
        this.allRecords = records;
        applyFilter();
    }

    private void applyFilter() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        PdsRecord selected = list.getSelectedValue();
        model.clear();
        for (PdsRecord r : allRecords) {
            String haystack = (r.displayName() + " " + r.status + " " + r.remarks).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || haystack.contains(q)) model.addElement(r);
        }
        if (selected != null) list.setSelectedValue(selected, true);
    }

    public PdsRecord getSelected() { return list.getSelectedValue(); }

    public void selectById(String id) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id.equals(id)) { list.setSelectedIndex(i); return; }
        }
    }

    public void addSelectionListener(Runnable onChange) {
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onChange.run(); });
    }

    public JList<PdsRecord> getJList() { return list; }

    private static class RecordCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel, boolean focus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(l, value, index, sel, focus);
            if (value instanceof PdsRecord r) {
                lbl.setText("<html>" + r.displayName() + "<br><small>" + r.status + "</small></html>");
                switch (r.status) {
                    case COMPLETED -> lbl.setForeground(sel ? Color.WHITE : new Color(0, 120, 0));
                    case CANCELLED -> lbl.setForeground(sel ? Color.WHITE : new Color(150, 0, 0));
                    default -> lbl.setForeground(sel ? Color.WHITE : new Color(150, 100, 0));
                }
            }
            return lbl;
        }
    }

    @FunctionalInterface
    private interface SimpleDocListener extends DocumentListener {
        void update(DocumentEvent e);
        @Override default void insertUpdate(DocumentEvent e) { update(e); }
        @Override default void removeUpdate(DocumentEvent e) { update(e); }
        @Override default void changedUpdate(DocumentEvent e) { update(e); }
    }
}
