package com.healthfirst.pims;

import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class UsersPanel extends JPanel {
    private final DefaultTableModel model = Ui.model("ID", "Name", "Username", "Role", "Status", "Created");
    private final JTable table = Ui.table(model);

    public UsersPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Ui.CANVAS);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(toolbar(), BorderLayout.NORTH);
        add(Ui.scroll(table), BorderLayout.CENTER);
        refresh();
    }

    private JPanel toolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(Ui.heading("Team access"));
        titles.add(Ui.muted("Create cashier logins, reset passwords and suspend access."));
        bar.add(titles, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton refresh = Ui.secondary("Refresh");
        JButton reset = Ui.secondary("Reset password");
        JButton deactivate = Ui.danger("Suspend / restore");
        JButton add = Ui.primary("＋  New cashier");
        refresh.addActionListener(event -> refresh());
        reset.addActionListener(event -> resetPassword());
        deactivate.addActionListener(event -> toggleActive());
        add.addActionListener(event -> createCashier());
        actions.add(refresh);
        actions.add(reset);
        actions.add(deactivate);
        actions.add(add);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void refresh() {
        model.setRowCount(0);
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT user_id, full_name, username, role, active, created_at FROM users ORDER BY role, full_name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                model.addRow(new Object[]{result.getInt("user_id"), result.getString("full_name"),
                        result.getString("username"), result.getString("role"),
                        result.getBoolean("active") ? "Active" : "Suspended", result.getTimestamp("created_at")});
            }
        } catch (SQLException exception) {
            Ui.error(this, "Could not load users: " + exception.getMessage());
        }
    }

    private void createCashier() {
        JTextField fullName = new JTextField(20);
        JTextField username = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        Ui.styleInput(fullName);
        Ui.styleInput(username);
        Ui.styleInput(password);
        JPanel form = MedicinePanel.formPanel(
                new String[]{"Full name", "Username", "Temporary password"},
                new java.awt.Component[]{fullName, username, password});
        if (JOptionPane.showConfirmDialog(this, form, "Create cashier account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            java.util.Arrays.fill(password.getPassword(), '\0');
            return;
        }
        char[] plainPassword = password.getPassword();
        if (fullName.getText().isBlank() || username.getText().isBlank() || plainPassword.length < 8) {
            java.util.Arrays.fill(plainPassword, '\0');
            Ui.error(this, "Enter a name, username, and password with at least 8 characters.");
            return;
        }
        String encoded = PasswordUtil.hash(plainPassword);
        java.util.Arrays.fill(plainPassword, '\0');
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'Cashier', ?)")) {
            statement.setString(1, username.getText().trim());
            statement.setString(2, encoded);
            statement.setString(3, fullName.getText().trim());
            statement.executeUpdate();
            refresh();
        } catch (SQLException exception) {
            Ui.error(this, "Could not create cashier. The username may already be in use. " + exception.getMessage());
        }
    }

    private void resetPassword() {
        int row = selectedRow();
        if (row < 0) {
            return;
        }
        int userId = (Integer) model.getValueAt(row, 0);
        String role = String.valueOf(model.getValueAt(row, 3));
        if (!"Cashier".equals(role)) {
            Ui.error(this, "Only cashier passwords can be reset here.");
            return;
        }
        JPasswordField password = new JPasswordField(20);
        Ui.styleInput(password);
        if (JOptionPane.showConfirmDialog(this, password, "New cashier password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            java.util.Arrays.fill(password.getPassword(), '\0');
            return;
        }
        char[] plainPassword = password.getPassword();
        if (plainPassword.length < 8) {
            java.util.Arrays.fill(plainPassword, '\0');
            Ui.error(this, "Use a password with at least 8 characters.");
            return;
        }
        String encoded = PasswordUtil.hash(plainPassword);
        java.util.Arrays.fill(plainPassword, '\0');
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET password = ? WHERE user_id = ? AND role = 'Cashier'")) {
            statement.setString(1, encoded);
            statement.setInt(2, userId);
            statement.executeUpdate();
            Ui.error(this, "Password updated.");
        } catch (SQLException exception) {
            Ui.error(this, "Could not reset password: " + exception.getMessage());
        }
    }

    private void toggleActive() {
        int row = selectedRow();
        if (row < 0) {
            return;
        }
        int userId = (Integer) model.getValueAt(row, 0);
        String role = String.valueOf(model.getValueAt(row, 3));
        String status = String.valueOf(model.getValueAt(row, 4));
        if (!"Cashier".equals(role)) {
            Ui.error(this, "Administrator accounts cannot be suspended from this screen.");
            return;
        }
        boolean activate = "Suspended".equals(status);
        String action = activate ? "restore" : "suspend";
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to " + action + " this cashier login?",
                "Confirm access change", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET active = ? WHERE user_id = ? AND role = 'Cashier'")) {
            statement.setBoolean(1, activate);
            statement.setInt(2, userId);
            statement.executeUpdate();
            refresh();
        } catch (SQLException exception) {
            Ui.error(this, "Could not update cashier access: " + exception.getMessage());
        }
    }

    private int selectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Ui.error(this, "Select a cashier first.");
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }
}