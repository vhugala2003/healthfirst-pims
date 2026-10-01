package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class MedicinePanel extends JPanel {
    private final DefaultTableModel model = Ui.model(
            "ID", "Medicine", "Company", "Type", "Price", "In stock", "Reorder at", "Expiry", "Supplier");
    private final JTable table = Ui.table(model);

    public MedicinePanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Ui.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(toolbar(), BorderLayout.NORTH);
        add(Ui.scroll(table), BorderLayout.CENTER);
        refresh();
    }

    private JPanel toolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(Ui.heading("Medicine catalogue"));
        titles.add(Ui.muted("Maintain prices, expiry dates and live stock levels."));
        bar.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton refresh = Ui.secondary("Refresh");
        JButton edit = Ui.secondary("Edit selected");
        JButton remove = Ui.danger("Delete");
        JButton add = Ui.primary("＋  Add medicine");
        refresh.addActionListener(event -> refresh());
        edit.addActionListener(event -> editSelected());
        remove.addActionListener(event -> deleteSelected());
        add.addActionListener(event -> editMedicine(null));
        actions.add(refresh);
        actions.add(edit);
        actions.add(remove);
        actions.add(add);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void refresh() {
        model.setRowCount(0);
        String sql = "SELECT m.medicine_id, m.name, m.company, m.medicine_type, m.price, "
                + "m.quantity_in_stock, m.reorder_level, m.expiry_date, COALESCE(s.name, '—') AS supplier "
                + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id ORDER BY m.name";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                model.addRow(new Object[]{result.getInt("medicine_id"), result.getString("name"),
                        result.getString("company"), result.getString("medicine_type"),
                        Ui.money(result.getBigDecimal("price")), result.getInt("quantity_in_stock"),
                        result.getInt("reorder_level"), result.getDate("expiry_date"), result.getString("supplier")});
            }
        } catch (SQLException exception) {
            Ui.error(this, "Could not load medicines: " + exception.getMessage());
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.error(this, "Select a medicine first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        editMedicine((Integer) model.getValueAt(modelRow, 0));
    }

    private void editMedicine(Integer medicineId) {
        JTextField name = input();
        JTextField company = input();
        JComboBox<String> type = new JComboBox<>(new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Other"});
        JTextField price = input();
        JTextField quantity = input();
        JTextField reorder = input();
        JTextField expiry = input();
        JComboBox<SupplierChoice> supplier = new JComboBox<>();
        supplier.addItem(new SupplierChoice(0, "No supplier"));
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT supplier_id, name FROM suppliers ORDER BY name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                supplier.addItem(new SupplierChoice(result.getInt(1), result.getString(2)));
            }
        } catch (SQLException exception) {
            Ui.error(this, "Could not load suppliers: " + exception.getMessage());
            return;
        }

        if (medicineId != null) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            name.setText(String.valueOf(model.getValueAt(row, 1)));
            company.setText(String.valueOf(model.getValueAt(row, 2)));
            type.setSelectedItem(model.getValueAt(row, 3));
            price.setText(String.valueOf(model.getValueAt(row, 4)).replace("R", "").replace(",", "").trim());
            quantity.setText(String.valueOf(model.getValueAt(row, 5)));
            reorder.setText(String.valueOf(model.getValueAt(row, 6)));
            expiry.setText(String.valueOf(model.getValueAt(row, 7)));
            String supplierName = String.valueOf(model.getValueAt(row, 8));
            for (int index = 0; index < supplier.getItemCount(); index++) {
                if (supplier.getItemAt(index).name().equals(supplierName)) {
                    supplier.setSelectedIndex(index);
                    break;
                }
            }
        } else {
            expiry.setText(LocalDate.now().plusYears(1).toString());
            quantity.setText("0");
            reorder.setText("10");
            price.setText("0.00");
        }

        JPanel form = formPanel(new String[]{"Medicine name", "Company", "Type", "Price (ZAR)",
                        "Quantity", "Reorder level", "Expiry date (YYYY-MM-DD)", "Supplier"},
                new java.awt.Component[]{name, company, type, price, quantity, reorder, expiry, supplier});
        int answer = JOptionPane.showConfirmDialog(this, form,
                medicineId == null ? "Add medicine" : "Edit medicine", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            BigDecimal parsedPrice = new BigDecimal(price.getText().trim());
            int parsedQuantity = Integer.parseInt(quantity.getText().trim());
            int parsedReorder = Integer.parseInt(reorder.getText().trim());
            LocalDate parsedExpiry = LocalDate.parse(expiry.getText().trim());
            if (name.getText().isBlank() || company.getText().isBlank() || parsedPrice.signum() < 0
                    || parsedQuantity < 0 || parsedReorder < 0) {
                throw new IllegalArgumentException("Complete all required fields with non-negative values.");
            }
            SupplierChoice choice = (SupplierChoice) supplier.getSelectedItem();
            try (Connection connection = Database.connect()) {
                String sql = medicineId == null
                        ? "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                        : "UPDATE medicines SET name = ?, company = ?, medicine_type = ?, price = ?, quantity_in_stock = ?, reorder_level = ?, expiry_date = ?, supplier_id = ? WHERE medicine_id = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, name.getText().trim());
                    statement.setString(2, company.getText().trim());
                    statement.setString(3, String.valueOf(type.getSelectedItem()));
                    statement.setBigDecimal(4, parsedPrice);
                    statement.setInt(5, parsedQuantity);
                    statement.setInt(6, parsedReorder);
                    statement.setDate(7, Date.valueOf(parsedExpiry));
                    if (choice == null || choice.id() == 0) {
                        statement.setNull(8, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(8, choice.id());
                    }
                    if (medicineId != null) {
                        statement.setInt(9, medicineId);
                    }
                    statement.executeUpdate();
                }
            }
            refresh();
        } catch (NumberFormatException | java.time.format.DateTimeParseException exception) {
            Ui.error(this, "Enter a valid price, whole-number quantity, and expiry date in YYYY-MM-DD format.");
        } catch (IllegalArgumentException exception) {
            Ui.error(this, exception.getMessage());
        } catch (SQLException exception) {
            Ui.error(this, "Could not save medicine: " + exception.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.error(this, "Select a medicine first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (Integer) model.getValueAt(modelRow, 0);
        String name = String.valueOf(model.getValueAt(modelRow, 1));
        if (JOptionPane.showConfirmDialog(this, "Delete " + name + " from the catalogue?\nSales history is protected.",
                "Confirm delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM medicines WHERE medicine_id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
            refresh();
        } catch (SQLException exception) {
            Ui.error(this, "This medicine may be referenced by sales history and cannot be deleted. " + exception.getMessage());
        }
    }

    private JTextField input() {
        JTextField field = new JTextField(20);
        Ui.styleInput(field);
        return field;
    }

    static JPanel formPanel(String[] labels, java.awt.Component[] inputs) {
        JPanel form = new JPanel(new GridLayout(labels.length, 2, 12, 9));
        form.setBorder(BorderFactory.createEmptyBorder(9, 4, 9, 4));
        for (int index = 0; index < labels.length; index++) {
            form.add(new JLabel(labels[index]));
            form.add(inputs[index]);
        }
        return form;
    }

    private record SupplierChoice(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}