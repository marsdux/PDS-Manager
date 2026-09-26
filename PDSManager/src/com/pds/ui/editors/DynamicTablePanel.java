package com.pds.ui.editors;

import com.pds.util.AttachmentIO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A JTable of String rows with toolbar "+" (add row) and "-" (remove selected
 * row(s)) buttons. Section panels convert their List&lt;SomeEntry&gt; to/from
 * String[] rows via simple mapper lambdas, keeping this component generic.
 *
 * When constructed with attachments enabled, an extra "Proof" column is added
 * whose cell is a button rather than text: it lets the user attach (or view/
 * replace/remove) a scanned supporting document for that specific row - e.g.
 * a diploma for an education entry, an appointment paper for a work entry.
 * The attachment travels with the row (it lives in that row's own model
 * cell), so adding/removing/reordering rows can never mix up whose proof
 * belongs to whom.
 */
public class DynamicTablePanel extends JPanel {

    /** A single attached file, kept in memory as Base64 - no external library needed to store or reopen it. */
    public static class Attachment {
        public String fileName = "";
        public String base64 = "";
        public boolean isEmpty() { return base64 == null || base64.isEmpty(); }
    }

    /** One row's plain text values plus its (possibly empty) attachment. */
    public static class RowData {
        public String[] values;
        public Attachment attachment;
        public RowData(String[] values, Attachment attachment) { this.values = values; this.attachment = attachment; }
    }

    private final DefaultTableModel model;
    private final JTable table;
    private final boolean attachmentsEnabled;
    private final int dataColumnCount; // number of real (visible-as-text) columns, excluding the Proof column

    public DynamicTablePanel(String[] columnNames) {
        this(columnNames, false);
    }

    public DynamicTablePanel(String[] columnNames, boolean withAttachments) {
        super(new BorderLayout(4, 4));
        this.attachmentsEnabled = withAttachments;
        this.dataColumnCount = columnNames.length;

        String[] cols = columnNames;
        if (withAttachments) {
            cols = new String[columnNames.length + 1];
            System.arraycopy(columnNames, 0, cols, 0, columnNames.length);
            cols[columnNames.length] = "Proof";
        }

        model = new DefaultTableModel(cols, 0);
        table = new JTable(model);
        table.setRowHeight(26);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setSurrendersFocusOnKeystroke(true);

        if (withAttachments) {
            var col = table.getColumnModel().getColumn(dataColumnCount);
            col.setCellRenderer(new AttachButtonRenderer());
            col.setCellEditor(new AttachButtonEditor());
            col.setPreferredWidth(120);
            col.setMaxWidth(140);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(100, 150));
        add(scroll, BorderLayout.CENTER);

        JButton add = new JButton("+ Add Row");
        JButton remove = new JButton("- Remove Row");
        add.setToolTipText("Add a new row");
        remove.setToolTipText("Remove the selected row(s)");
        add.addActionListener(e -> addRow());
        remove.addActionListener(e -> removeSelectedRows());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(add);
        toolbar.add(remove);
        if (withAttachments) {
            JLabel hint = new JLabel("  (\"Proof\" column: attach a scanned document for that row)");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            toolbar.add(hint);
        }
        add(toolbar, BorderLayout.NORTH);

        // Tab at the very last cell of the very last row creates a new row and
        // moves focus into it, so a fast typist never has to reach for the mouse.
        table.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && !e.isShiftDown()) {
                    int row = table.getSelectedRow();
                    int col = table.getSelectedColumn();
                    boolean lastCell = row == model.getRowCount() - 1 && col == model.getColumnCount() - 1;
                    if (lastCell && !table.isEditing()) {
                        addRow();
                        e.consume();
                        SwingUtilities.invokeLater(() -> {
                            table.changeSelection(model.getRowCount() - 1, 0, false, false);
                            table.editCellAt(model.getRowCount() - 1, 0);
                            Component ed = table.getEditorComponent();
                            if (ed != null) ed.requestFocusInWindow();
                        });
                    }
                }
            }
        });
    }

    public void addRow() {
        Object[] blank = new Object[model.getColumnCount()];
        for (int i = 0; i < dataColumnCount; i++) blank[i] = "";
        if (attachmentsEnabled) blank[dataColumnCount] = new Attachment();
        model.addRow(blank);
    }

    public void removeSelectedRows() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        int[] rows = table.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) model.removeRow(rows[i]);
    }

    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < dataColumnCount; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Plain-text rows only - used by sections that don't support attachments. */
    public void setRows(List<String[]> rows) {
        List<RowData> wrapped = new ArrayList<>();
        for (String[] r : rows) wrapped.add(new RowData(r, null));
        setRowsWithAttachments(wrapped);
    }

    public List<String[]> getRows() {
        List<String[]> out = new ArrayList<>();
        for (RowData rd : getRowsWithAttachments()) out.add(rd.values);
        return out;
    }

    public void setRowsWithAttachments(List<RowData> rows) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        model.setRowCount(0);
        for (RowData rd : rows) {
            Object[] rowObjs = new Object[model.getColumnCount()];
            System.arraycopy(rd.values, 0, rowObjs, 0, dataColumnCount);
            if (attachmentsEnabled) {
                rowObjs[dataColumnCount] = rd.attachment != null ? rd.attachment : new Attachment();
            }
            model.addRow(rowObjs);
        }
    }

    public List<RowData> getRowsWithAttachments() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        List<RowData> out = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String[] row = new String[dataColumnCount];
            for (int c = 0; c < dataColumnCount; c++) {
                Object v = model.getValueAt(r, c);
                row[c] = v == null ? "" : v.toString();
            }
            Attachment att = null;
            if (attachmentsEnabled) {
                Object raw = model.getValueAt(r, dataColumnCount);
                att = (raw instanceof Attachment) ? (Attachment) raw : new Attachment();
            }
            boolean allBlank = true;
            for (String s : row) if (!s.isBlank()) { allBlank = false; break; }
            if (allBlank && (att == null || att.isEmpty())) continue; // drop fully-empty trailing rows
            out.add(new RowData(row, att));
        }
        return out;
    }

    public JTable getTable() { return table; }

    // ---------------- Proof (attachment) button column ----------------

    private static String labelFor(Attachment a) {
        return (a != null && !a.isEmpty()) ? "Attached \u2713" : "Attach...";
    }

    private static class AttachButtonRenderer extends JButton implements TableCellRenderer {
        AttachButtonRenderer() { setMargin(new Insets(0, 4, 0, 4)); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            setText(labelFor(value instanceof Attachment ? (Attachment) value : null));
            return this;
        }
    }

    private class AttachButtonEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {
        private Attachment current;
        private final JButton button = new JButton();

        AttachButtonEditor() {
            button.setMargin(new Insets(0, 4, 0, 4));
            button.addActionListener(this::onClick);
        }

        private void onClick(ActionEvent e) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem attach = new JMenuItem(current != null && !current.isEmpty() ? "Replace file..." : "Attach file...");
            attach.addActionListener(a -> { chooseAndAttach(); fireEditingStopped(); });
            menu.add(attach);
            if (current != null && !current.isEmpty()) {
                JMenuItem view = new JMenuItem("View / open attached file");
                view.addActionListener(a -> { AttachmentIO.open(DynamicTablePanel.this, current.fileName, current.base64); fireEditingStopped(); });
                JMenuItem print = new JMenuItem("Print attached file");
                print.addActionListener(a -> { AttachmentIO.print(DynamicTablePanel.this, current.fileName, current.base64); fireEditingStopped(); });
                JMenuItem remove = new JMenuItem("Remove attachment");
                remove.addActionListener(a -> { current = new Attachment(); fireEditingStopped(); });
                menu.add(view);
                menu.add(print);
                menu.add(remove);
            }
            menu.show(button, 0, button.getHeight());
        }

        private void chooseAndAttach() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Attach supporting document");
            if (chooser.showOpenDialog(DynamicTablePanel.this) != JFileChooser.APPROVE_OPTION) return;
            AttachmentIO.Loaded loaded = AttachmentIO.readFile(DynamicTablePanel.this, chooser.getSelectedFile());
            if (loaded == null) return;
            Attachment a = new Attachment();
            a.fileName = loaded.fileName;
            a.base64 = loaded.base64;
            current = a;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            current = (value instanceof Attachment) ? (Attachment) value : new Attachment();
            button.setText(labelFor(current));
            return button;
        }

        @Override
        public Object getCellEditorValue() { return current; }
    }

}
