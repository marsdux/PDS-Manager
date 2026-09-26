package com.pds.ui;

import com.pds.storage.PdsRecordStore;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class LoginDialog extends JDialog {

    private boolean success = false;

    public LoginDialog(Frame owner, PdsRecordStore store) {
        super(owner, store.isInitialized() ? "Unlock PDS Manager" : "Set Up PDS Manager", true);
        boolean firstRun = !store.isInitialized();

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        content.add(new JLabel(firstRun
                ? "<html>No local database found.<br>Choose a master password to encrypt all PDS records on this computer.<br>" +
                  "<b>There is no recovery if you forget it - write it down somewhere safe.</b></html>"
                : "<html>Enter the master password to unlock your PDS database.</html>"), gbc);

        gbc.gridy++;
        JPasswordField pw1 = new JPasswordField(22);
        content.add(labeled(firstRun ? "New master password:" : "Master password:", pw1), gbc);

        JPasswordField pw2 = new JPasswordField(22);
        if (firstRun) {
            gbc.gridy++;
            content.add(labeled("Confirm password:", pw2), gbc);
        }

        JLabel error = new JLabel(" ");
        error.setForeground(Color.RED);
        gbc.gridy++;
        content.add(error, gbc);

        JButton ok = new JButton(firstRun ? "Create Database" : "Unlock");
        JButton cancel = new JButton("Exit");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);
        gbc.gridy++;
        content.add(buttons, gbc);

        ok.addActionListener(e -> {
            char[] p1 = pw1.getPassword();
            char[] p2 = pw2.getPassword();
            try {
                if (firstRun) {
                    if (p1.length < 6) { error.setText("Password must be at least 6 characters."); return; }
                    if (!Arrays.equals(p1, p2)) { error.setText("Passwords do not match."); return; }
                    store.initialize(p1);
                    success = true;
                    dispose();
                } else {
                    if (store.unlock(p1)) {
                        success = true;
                        dispose();
                    } else {
                        error.setText("Incorrect password. Try again.");
                    }
                }
            } catch (Exception ex) {
                error.setText("Error: " + ex.getMessage());
            } finally {
                Arrays.fill(p1, '\0');
                Arrays.fill(p2, '\0');
            }
        });
        cancel.addActionListener(e -> { success = false; dispose(); });

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    public boolean isSuccess() { return success; }
}
