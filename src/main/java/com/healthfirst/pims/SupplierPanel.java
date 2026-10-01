package com.healthfirst.pims;

import javax.swing.JButton;
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

public final class SupplierPanel extends JPanel {
    private final DefaultTableModel model = Ui.model("ID", "Supplier", "Contact", "Phone", "Email", "Address");
    private final JTable table = Ui.table(model);

    public SupplierPanel() {
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
        titles.add(Ui.heading("Supplier directory"));
        titles.add(Ui.muted("Keep delivery contacts current and linked to the catalogue."));
        bar.add(titles, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton refresh = Ui.secondary("Refresh");
        JButton edit = Ui.secondary("Edit selected");
        JButton remove = Ui.danger("Delete");
        JButton add = Ui.primary("＋  Add supplier");
        refresh.addActionListener(event -> refresh());
        edit.addActionListener(event -> editSelected());
        remove.addActionListener(event -> deleteSelected());
        add.addActionListener(event -> editSupplier(null));
        actions.add(refresh);
        actions.add(edit);
        actions.add(remove);
        actions.add(add);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void refresh() {
        model.setRowCount(0);
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT supplier_id, name, contact_person, phone, email, address FROM suppliers ORDER BY name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                model.addRow(new Object[]{result.getInt("supplier_id"), result.getString("name"),
                        result.getString("contact_person"), result.getString("phone"),
                        result.getString("email"), result.getString("address")});
            }
        } catch (SQLException exception) {
            Ui.error(this, "Could not load suppliers: " + exception.getMessage());
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.error(this, "Select a supplier first.");
            return;
        }
        editSupplier((Integer) model.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    private void editSupplier(Integer supplierId) {
        JTextField name = input();
        JTextField contact = input();
        JTextField phone = input();
        JTextField email = input();
        JTextField address = input();
        if (supplierId != null) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            name.setText(String.valueOf(model.getValueAt(row, 1)));
            contact.setText(String.valueOf(model.getValueAt(row, 2)));
            phone.setText(String.valueOf(model.getValueAt(row, 3)));
            email.setText(String.valueOf(model.getValueAt(row, 4)));
            address.setText(String.valueOf(model.getValueAt(row, 5)));
        }
        JPanel form = MedicinePanel.formPanel(
                new String[]{"Supplier name", "Contact person", "Phone", "Email", "Address"},
                new java.awt.Component[]{name, contact, phone, email, address});
        if (JOptionPane.showConfirmDialog(this, form,
                supplierId == null ? "Add supplier" : "Edit supplier", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        if (name.getText().isBlank() || contact.getText().isBlank() || phone.getText().isBlank()
                || email.getText().isBlank() || address.getText().isBlank()) {
            Ui.error(this, "Complete all supplier fields.");
            return;
        }
        String sql = supplierId == null
                ? "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE suppliers SET name = ?, contact_person = ?, phone = ?, email = ?, address = ? WHERE supplier_id = ?";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.getText().trim());
            statement.setString(2, contact.getText().trim());
            statement.setString(3, phone.getText().trim());
            statement.setString(4, email.getText().trim());
            statement.setString(5, address.getText().trim());
            if (supplierId != null) {
                statement.setInt(6, supplierId);
            }
            statement.executeUpdate();
            refresh();
        } catch (SQLException exception) {
            Ui.error(this, "Could not save supplier: " + exception.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.error(this, "Select a supplier first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int supplierId = (Integer) model.getValueAt(modelRow, 0);
        String supplierName = String.valueOf(model.getValueAt(modelRow, 1));
        if (JOptionPane.showConfirmDialog(this,
                "Delete " + supplierName + "? Medicines will remain in the catalogue without a supplier.",
                "Confirm delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM suppliers WHERE supplier_id = ?")) {
            statement.setInt(1, supplierId);
            statement.executeUpdate();
            refresh();
        } catch (SQLException exception) {
            Ui.error(this, "Could not delete supplier: " + exception.getMessage());
        }
    }

    private JTextField input() {
        JTextField field = new JTextField(20);
        Ui.styleInput(field);
        return field;
    }
}