package com.pds.ui;

import com.pds.model.PdsRecord;
import com.pds.model.RecordStatus;
import com.pds.storage.PdsRecordStore;
import com.pds.util.CsvUtil;
import com.pds.util.IdGen;
import com.pds.util.PrintUtil;
import com.pds.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class MainFrame extends JFrame {

    private final PdsRecordStore store;
    private final RecordListPanel recordList = new RecordListPanel();
    private final RecordEditorPanel editor = new RecordEditorPanel();
    private final DashboardPanel dashboard = new DashboardPanel();
    private final ReportsPanel reports = new ReportsPanel();
    private final JTabbedPane mainTabs = new JTabbedPane();
    private final JLabel statusBar = new JLabel(" ");

    public MainFrame(PdsRecordStore store) {
        super("PDS Manager - Personal Data Sheet (CS Form No. 212)");
        this.store = store;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { confirmExit(); }
        });

        setJMenuBar(buildMenuBar());

        mainTabs.addTab("Record Editor", editor);
        mainTabs.addTab("Dashboard", dashboard);
        mainTabs.addTab("Reports & Summary", reports);
        mainTabs.addChangeListener(e -> refreshAggregates());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, recordList, mainTabs);
        split.setDividerLocation(280);
        add(split, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        recordList.addSelectionListener(this::onSelectionChanged);

        setSize(1200, 800);
        setLocationRelativeTo(null);
        reloadFromDisk();
    }

    // ---------------- menu ----------------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(menuItem("New Record", e -> newRecord()));
        file.add(menuItem("Save Record", e -> saveCurrent()));
        file.add(menuItem("Delete Record", e -> deleteCurrent()));
        file.addSeparator();
        file.add(menuItem("Import Record...", e -> importRecord()));
        file.add(menuItem("Export Selected Record...", e -> exportRecord()));
        file.add(menuItem("Export Summary (CSV)...", e -> exportCsv()));
        file.addSeparator();
        file.add(menuItem("Print Selected Record...", e -> printCurrent()));
        file.addSeparator();
        file.add(menuItem("Change Master Password...", e -> changePassword()));
        file.addSeparator();
        file.add(menuItem("Exit", e -> confirmExit()));
        bar.add(file);

        JMenu edit = new JMenu("Edit");
        edit.add(menuItem("Find / Search", e -> recordList.getJList().requestFocusInWindow()));
        edit.add(menuItem("Refresh from Disk", e -> reloadFromDisk()));
        bar.add(edit);

        JMenu help = new JMenu("Help");
        help.add(menuItem("About", e -> JOptionPane.showMessageDialog(this,
                "PDS Manager\nBuilt with Java Swing (no external libraries)\n" +
                "Records are stored locally with AES-256-GCM encryption.",
                "About", JOptionPane.INFORMATION_MESSAGE)));
        bar.add(help);

        return bar;
    }

    private JMenuItem menuItem(String label, java.awt.event.ActionListener l) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(l);
        return item;
    }

    // ---------------- data flow ----------------

    private java.util.List<PdsRecord> currentList = new java.util.ArrayList<>();

    private void reloadFromDisk() {
        try {
            PdsRecordStore.LoadResult result = store.loadAll();
            currentList = result.records;
            recordList.setRecords(currentList);
            if (!result.corruptedFiles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Warning: " + result.corruptedFiles.size() + " record file(s) could not be read " +
                                "(corrupted, tampered, or wrong password):\n" + String.join("\n", result.corruptedFiles),
                        "Data Integrity Warning", JOptionPane.WARNING_MESSAGE);
            }
            refreshAggregates();
            status("Loaded " + currentList.size() + " record(s).");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load records: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAggregates() {
        dashboard.refresh(currentList);
        reports.refresh(currentList);
    }

    private void onSelectionChanged() {
        PdsRecord r = recordList.getSelected();
        if (r != null) {
            editor.loadRecord(r);
            status("Editing: " + r.displayName());
        }
    }

    private void newRecord() {
        PdsRecord r = new PdsRecord(IdGen.newId());
        currentList.add(r);
        recordList.setRecords(currentList);
        recordList.selectById(r.id);
        editor.loadRecord(r);
        status("New record created (unsaved). Fill in the form, then File > Save Record.");
    }

    private void saveCurrent() {
        PdsRecord r = editor.commitToRecord();
        if (r == null) { status("No record selected."); return; }
        if (r.status == RecordStatus.COMPLETED) {
            List<String> errors = Validator.validateForCompletion(r);
            if (!errors.isEmpty()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "This record is marked Completed but has issues:\n- " + String.join("\n- ", errors) +
                                "\n\nSave anyway?", "Incomplete Data", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
            }
        }
        try {
            store.save(r);
            recordList.setRecords(currentList);
            recordList.selectById(r.id);
            refreshAggregates();
            status("Saved: " + r.displayName() + " at " + r.updatedAt);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCurrent() {
        PdsRecord r = recordList.getSelected();
        if (r == null) { status("No record selected."); return; }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete \"" + r.displayName() + "\"?\nThe encrypted file is moved to the deleted/ folder for recovery, not permanently erased.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            store.delete(r.id);
            currentList.removeIf(x -> x.id.equals(r.id));
            recordList.setRecords(currentList);
            editor.clear();
            refreshAggregates();
            status("Deleted: " + r.displayName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printCurrent() {
        PdsRecord r = recordList.getSelected();
        if (r == null) { status("No record selected."); return; }
        try {
            PrintUtil.printRecord(this, r);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Print failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("pds_summary_export.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            CsvUtil.exportSummary(chooser.getSelectedFile().toPath(), currentList);
            status("Exported summary CSV to " + chooser.getSelectedFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Exports the selected record's own encrypted file as-is - it can be imported into any PDS Manager
     *  data folder protected by the SAME master password. */
    private void exportRecord() {
        PdsRecord r = recordList.getSelected();
        if (r == null) { status("No record selected."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(safeFileName(r.displayName()) + ".pdsrecord"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path src = store.recordFilePath(r.id);
            Files.copy(src, chooser.getSelectedFile().toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            status("Exported record to " + chooser.getSelectedFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importRecord() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            PdsRecord imported = store.importRecordFile(chooser.getSelectedFile().toPath());
            currentList.add(imported);
            recordList.setRecords(currentList);
            recordList.selectById(imported.id);
            refreshAggregates();
            status("Imported: " + imported.displayName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Import failed - the file may be corrupted, or protected by a different master password.\n" + e.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changePassword() {
        JPasswordField oldPw = new JPasswordField();
        JPasswordField newPw = new JPasswordField();
        JPasswordField confirmPw = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(3, 2, 4, 4));
        panel.add(new JLabel("Current password:")); panel.add(oldPw);
        panel.add(new JLabel("New password:")); panel.add(newPw);
        panel.add(new JLabel("Confirm new password:")); panel.add(confirmPw);
        int choice = JOptionPane.showConfirmDialog(this, panel, "Change Master Password", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) return;
        char[] o = oldPw.getPassword(), n = newPw.getPassword(), c = confirmPw.getPassword();
        try {
            if (!Arrays.equals(n, c)) { JOptionPane.showMessageDialog(this, "New passwords do not match."); return; }
            if (n.length < 6) { JOptionPane.showMessageDialog(this, "New password must be at least 6 characters."); return; }
            store.changePassword(o, n);
            JOptionPane.showMessageDialog(this, "Master password changed successfully.");
            status("Master password changed.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(o, '\0'); Arrays.fill(n, '\0'); Arrays.fill(c, '\0');
        }
    }

    private void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this, "Exit PDS Manager?\nUnsaved changes in the current record will be lost.",
                "Confirm Exit", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            store.lock();
            dispose();
            System.exit(0);
        }
    }

    private void status(String s) { statusBar.setText(" " + s); }

    private static String safeFileName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
