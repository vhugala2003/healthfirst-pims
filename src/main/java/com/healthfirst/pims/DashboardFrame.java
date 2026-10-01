package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DashboardFrame extends JFrame {
    private final PimsUser user;

    public DashboardFrame(PimsUser user) {
        super("HealthFirst PIMS | " + user.role());
        this.user = user;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setSize(1240, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
        if (user.isAdmin()) {
            tabs.addTab("Overview", overview());
            tabs.addTab("Medicines", new MedicinePanel());
            tabs.addTab("Suppliers", new SupplierPanel());
            tabs.addTab("Team", new UsersPanel());
            tabs.addTab("Reports", new ReportsPanel());
        } else {
            tabs.addTab("Point of sale", new PosPanel(user));
            tabs.addTab("Stock lookup", new StockLookupPanel());
        }
        add(tabs, BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                new LoginFrame().setVisible(true);
            }
        });
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(Ui.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(16, 26, 16, 26)));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("H+");
        mark.setOpaque(true);
        mark.setBackground(Ui.FOREST);
        mark.setForeground(Color.WHITE);
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 16f));
        mark.setPreferredSize(new Dimension(40, 40));
        JLabel title = new JLabel("HealthFirst");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(Ui.INK);
        JLabel sub = new JLabel("PHARMACY OPERATIONS");
        sub.setForeground(Ui.MUTED);
        sub.setFont(sub.getFont().deriveFont(Font.BOLD, 9f));
        JPanel labels = new JPanel(new GridLayout(2, 1));
        labels.setOpaque(false);
        labels.add(title);
        labels.add(sub);
        brand.add(mark, BorderLayout.WEST);
        brand.add(labels, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);

        JPanel account = new JPanel(new BorderLayout(12, 0));
        account.setOpaque(false);
        JLabel userInfo = new JLabel("<html><b>" + escape(user.fullName()) + "</b><br><font color='#687973'>"
                + user.role().toUpperCase() + " · " + escape(user.username()) + "</font></html>");
        userInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        account.add(userInfo, BorderLayout.CENTER);
        javax.swing.JButton signOut = Ui.secondary("Sign out");
        signOut.addActionListener(event -> dispose());
        account.add(signOut, BorderLayout.EAST);
        header.add(account, BorderLayout.EAST);
        return header;
    }

    private JPanel overview() {
        JPanel page = Ui.page();
        JPanel intro = new JPanel(new BorderLayout());
        intro.setOpaque(false);
        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(Ui.heading("Good day, " + user.fullName().split(" ")[0]));
        titleBlock.add(Ui.muted("A live view of stock and trading activity."));
        intro.add(titleBlock, BorderLayout.WEST);
        page.add(intro, BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(metric("ACTIVE MEDICINES", "SELECT COUNT(*) FROM medicines", Ui.FOREST));
        stats.add(metric("LOW STOCK ITEMS", "SELECT COUNT(*) FROM medicines WHERE quantity_in_stock <= reorder_level", Ui.CORAL));
        stats.add(metric("EXPIRING IN 30 DAYS", "SELECT COUNT(*) FROM medicines WHERE expiry_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY) AND quantity_in_stock > 0", new Color(171, 129, 42)));
        page.add(stats, BorderLayout.CENTER);
        return page;
    }

    private JPanel metric(String caption, String sql, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Ui.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel label = new JLabel(caption);
        label.setForeground(Ui.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        JLabel value = new JLabel("—");
        value.setFont(value.getFont().deriveFont(Font.BOLD, 30f));
        value.setForeground(Ui.INK);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                value.setText(Integer.toString(result.getInt(1)));
            }
        } catch (SQLException exception) {
            value.setToolTipText(exception.getMessage());
        }
        return card;
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}