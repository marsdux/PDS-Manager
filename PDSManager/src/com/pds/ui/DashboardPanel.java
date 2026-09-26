package com.pds.ui;

import com.pds.model.PdsRecord;
import com.pds.model.RecordStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final JLabel ongoingCount = bigLabel("0");
    private final JLabel completedCount = bigLabel("0");
    private final JLabel cancelledCount = bigLabel("0");
    private final JLabel totalCount = bigLabel("0");
    private final DefaultTableModel recentModel = new DefaultTableModel(
            new String[]{"Name", "Status", "Last Updated", "Remarks"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public DashboardPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.add(card("Ongoing", ongoingCount, new Color(255, 243, 205)));
        cards.add(card("Completed", completedCount, new Color(212, 237, 218)));
        cards.add(card("Cancelled", cancelledCount, new Color(248, 215, 218)));
        cards.add(card("Total Records", totalCount, new Color(222, 226, 230)));
        add(cards, BorderLayout.NORTH);

        JTable recent = new JTable(recentModel);
        recent.setRowHeight(22);
        add(new JScrollPane(recent), BorderLayout.CENTER);
    }

    public void refresh(List<PdsRecord> records) {
        long ongoing = records.stream().filter(r -> r.status == RecordStatus.ONGOING).count();
        long completed = records.stream().filter(r -> r.status == RecordStatus.COMPLETED).count();
        long cancelled = records.stream().filter(r -> r.status == RecordStatus.CANCELLED).count();
        ongoingCount.setText(String.valueOf(ongoing));
        completedCount.setText(String.valueOf(completed));
        cancelledCount.setText(String.valueOf(cancelled));
        totalCount.setText(String.valueOf(records.size()));

        recentModel.setRowCount(0);
        records.stream()
                .sorted(Comparator.comparing((PdsRecord r) -> r.updatedAt == null ? "" : r.updatedAt).reversed())
                .forEach(r -> recentModel.addRow(new Object[]{r.displayName(), r.status, r.updatedAt, r.remarks}));
    }

    private JPanel card(String title, JLabel valueLabel, Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker()), BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel title2 = new JLabel(title);
        title2.setFont(title2.getFont().deriveFont(Font.BOLD, 13f));
        p.add(title2, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private JLabel bigLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 32f));
        return l;
    }
}
