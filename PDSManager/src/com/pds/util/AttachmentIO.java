package com.pds.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public final class AttachmentIO {
    private AttachmentIO() {}

    public static final long MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024; // 5 MB per attached document

    public static class Loaded {
        public final String fileName;
        public final String base64;
        Loaded(String fileName, String base64) { this.fileName = fileName; this.base64 = base64; }
    }

    /** Reads a file into memory as Base64, enforcing the per-attachment size cap. Returns null (with a dialog shown) on failure. */
    public static Loaded readFile(Component parent, File f) {
        try {
            if (f.length() > MAX_ATTACHMENT_BYTES) {
                JOptionPane.showMessageDialog(parent,
                        "That file is " + (f.length() / (1024 * 1024)) + " MB - the limit per attachment is 5 MB.",
                        "File Too Large", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            byte[] bytes = Files.readAllBytes(f.toPath());
            return new Loaded(f.getName(), Base64.getEncoder().encodeToString(bytes));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not read file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /** Writes the Base64 payload to a uniquely-named temp file with the original extension, for opening/printing. */
    public static File toTempFile(String fileName, String base64) throws IOException {
        String suffix = (fileName != null && fileName.contains(".")) ? fileName.substring(fileName.lastIndexOf('.')) : "";
        File tmp = File.createTempFile("pds-attachment-", suffix);
        tmp.deleteOnExit();
        Files.write(tmp.toPath(), Base64.getDecoder().decode(base64));
        return tmp;
    }

    /** Opens an attachment with the OS's default viewer. Pure JDK (java.awt.Desktop) - no bundled PDF/image viewer needed. */
    public static void open(Component parent, String fileName, String base64) {
        if (base64 == null || base64.isEmpty()) return;
        try {
            File tmp = toTempFile(fileName, base64);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tmp);
            } else {
                JOptionPane.showMessageDialog(parent, "Saved to: " + tmp.getAbsolutePath()
                        + "\n(No default-application support detected on this system to auto-open it.)");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Could not open attachment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Sends an attachment straight to the OS's print handling (e.g. the system PDF viewer's print flow). Falls back to Open if unsupported. */
    public static void print(Component parent, String fileName, String base64) {
        if (base64 == null || base64.isEmpty()) return;
        try {
            File tmp = toTempFile(fileName, base64);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
                Desktop.getDesktop().print(tmp);
            } else {
                JOptionPane.showMessageDialog(parent,
                        "Direct printing of this file type isn't supported on this system - opening it instead so you can print from there.",
                        "Print Not Supported", JOptionPane.INFORMATION_MESSAGE);
                open(parent, fileName, base64);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Could not print attachment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
