package com.pds.ui;

import javax.swing.*;
import java.awt.*;

public final class FormUtil {
    private FormUtil() {}

    public static GridBagConstraints baseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    /** Adds a "Label: [field]" pair on its own row. Returns the next free row index. */
    public static int addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        panel.add(field, gbc);
        return row + 1;
    }

    /** Adds two "Label: [field]" pairs sharing one row (4 grid columns total). */
    public static int addRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label1, JComponent field1, String label2, JComponent field2) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(label1), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        panel.add(field1, gbc);
        gbc.gridx = 2; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(label2), gbc);
        gbc.gridx = 3; gbc.gridy = row; gbc.weightx = 1;
        panel.add(field2, gbc);
        return row + 1;
    }

    public static JTextField tf() { return new JTextField(18); }

    public static TitledBorderPanel section(String title) { return new TitledBorderPanel(title); }

    /** JPanel pre-configured with a GridBagLayout and a titled border. */
    public static class TitledBorderPanel extends JPanel {
        public TitledBorderPanel(String title) {
            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(title));
        }
    }
}
