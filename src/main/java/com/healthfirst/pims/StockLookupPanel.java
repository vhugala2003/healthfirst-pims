package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

public final class StockLookupPanel extends JPanel {
    private final DefaultTableModel model = Ui.model("Medicine", "Company", "Type", "Price", "Available", "Expiry", "Supplier");
    private final JTable table = Ui.table(model);
    private final JTextField search = new JTextField(25);

    public StockLookupPanel() {
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
        titles.add(Ui.heading("Stock lookup"));
        titles.add(Ui.muted("Read-only availability and pricing for the counter team."));
        bar.add(titles, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        Ui.styleInput(search);
        search.putClientProperty("JTextField.placeholderText", "Search by medicine name");
        JButton find = Ui.primary("Search");
        find.addActionListener(event -> refresh());
        search.addActionListener(event -> refresh());
        actions.add(search);
        actions.add(find);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void refresh() {
        model.setRowCount(0);
        String sql = "SELECT m.name, m.company, m.medicine_type, m.price, m.quantity_in_stock, m.expiry_date, "
                + "COALESCE(s.name, '—') AS supplier FROM medicines m "
                + "LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id "
                + "WHERE m.name LIKE ? AND m.expiry_date >= CURRENT_DATE ORDER BY m.name";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + search.getText().trim() + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    model.addRow(new Object[]{result.getString("name"), result.getString("company"),
                            result.getString("medicine_type"), Ui.money(result.getBigDecimal("price")),
                            result.getInt("quantity_in_stock"), result.getDate("expiry_date"), result.getString("supplier")});
                }
            }
        } catch (SQLException exception) {
            Ui.error(this, "Could not load stock: " + exception.getMessage());
        }
    }
}