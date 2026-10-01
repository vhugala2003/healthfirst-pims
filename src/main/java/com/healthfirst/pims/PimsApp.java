package com.healthfirst.pims;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

public final class PimsApp {
    private PimsApp() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("TabbedPane.showTabSeparators", true);
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}