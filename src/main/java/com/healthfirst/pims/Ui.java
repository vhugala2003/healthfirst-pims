package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class Ui {
    static final Color FOREST = new Color(28, 78, 65);
    static final Color FOREST_DARK = new Color(18, 52, 45);
    static final Color LIME = new Color(212, 230, 180);
    static final Color CORAL = new Color(224, 105, 78);
    static final Color INK = new Color(31, 48, 44);
    static final Color MUTED = new Color(104, 121, 115);
    static final Color CANVAS = new Color(244, 246, 242);
    static final Color LINE = new Color(222, 229, 221);
    static final Color WHITE = Color.WHITE;

    private Ui() {
    }

    static JPanel page() {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 16));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        return panel;
    }

    static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
        label.setForeground(INK);
        return label;
    }

    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setFocusPainted(false);
        button.setMargin(new Insets(9, 16, 9, 16));
        button.putClientProperty("Button.arc", 10);
        return button;
    }

    static JButton primary(String text) {
        return button(text, FOREST, WHITE);
    }

    static JButton secondary(String text) {
        return button(text, WHITE, FOREST);
    }

    static JButton danger(String text) {
        return button(text, new Color(255, 235, 229), new Color(158, 58, 43));
    }

    static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionBackground(new Color(227, 238, 219));
        table.setSelectionForeground(INK);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.setRowHeight(34);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(new Color(239, 243, 237));
        header.setForeground(MUTED);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        return table;
    }

    static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(WHITE);
        return scroll;
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA")).format(amount);
    }

    static void error(java.awt.Component parent, String message) {
        javax.swing.JOptionPane.showMessageDialog(parent, message, "HealthFirst PIMS", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    static void styleInput(javax.swing.JComponent component) {
        component.putClientProperty("JComponent.roundRect", true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        component.setBackground(WHITE);
        component.setForeground(INK);
    }

    static void installDefaults() {
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("TabbedPane.background", CANVAS);
        UIManager.put("TabbedPane.selectedBackground", WHITE);
        UIManager.put("TabbedPane.underlineColor", FOREST);
        UIManager.put("Table.alternateRowColor", new Color(249, 251, 247));
    }
}