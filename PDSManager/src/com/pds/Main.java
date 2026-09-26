package com.pds;

import com.pds.storage.PdsRecordStore;
import com.pds.ui.LoginDialog;
import com.pds.ui.MainFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* fall back to default L&F */ }

        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Path dataDir = resolveDataDir();
        PdsRecordStore store = new PdsRecordStore(dataDir);

        JFrame ownerForDialog = new JFrame();
        LoginDialog login = new LoginDialog(ownerForDialog, store);
        login.setVisible(true);
        ownerForDialog.dispose();

        if (!login.isSuccess() || !store.isUnlocked()) {
            System.exit(0);
            return;
        }

        MainFrame frame = new MainFrame(store);
        frame.setVisible(true);
    }

    /** Stores data under the user's home directory so it survives regardless of where the app/jar is launched from. */
    private static Path resolveDataDir() {
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, "PDSManagerData");
    }
}
