package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ReportsPanel extends JPanel {
    public ReportsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Ui.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(Ui.heading("Business reports"));
        titles.add(Ui.muted("Sales performance, stock risk and products nearing expiry."));
        add(titles, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sales", new ReportView("Sales register",
                "SELECT s.sale_id AS 'Sale #', DATE_FORMAT(s.sale_date, '%Y-%m-%d %H:%i') AS 'Date / time', "
                        + "u.full_name AS Cashier, COUNT(si.sale_item_id) AS Lines, s.total_amount AS 'Total (ZAR)' "
                        + "FROM sales s JOIN users u ON u.user_id = s.user_id "
                        + "LEFT JOIN sale_items si ON si.sale_id = s.sale_id "
                        + "GROUP BY s.sale_id, s.sale_date, u.full_name ORDER BY s.sale_date DESC"));
        tabs.addTab("Item-wise", new ReportView("Item performance",
                "SELECT m.name AS Medicine, SUM(si.quantity_sold) AS 'Units sold', "
                        + "COUNT(DISTINCT si.sale_id) AS Transactions, "
                        + "SUM(si.quantity_sold * si.price_at_sale) AS 'Revenue (ZAR)' "
                        + "FROM sale_items si JOIN medicines m ON m.medicine_id = si.medicine_id "
                        + "GROUP BY m.medicine_id, m.name ORDER BY SUM(si.quantity_sold) DESC, m.name"));
        tabs.addTab("Low stock", new ReportView("Reorder watch",
                "SELECT m.name AS Medicine, m.quantity_in_stock AS 'In stock', m.reorder_level AS 'Reorder at', "
                        + "GREATEST(m.reorder_level - m.quantity_in_stock, 0) AS 'Units to reorder', "
                        + "COALESCE(s.name, 'Unassigned') AS Supplier "
                        + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id "
                        + "WHERE m.quantity_in_stock <= m.reorder_level ORDER BY m.quantity_in_stock, m.name"));
        tabs.addTab("Expiry", new ReportView("Expiry in the next month",
                "SELECT m.name AS Medicine, m.company AS Company, m.quantity_in_stock AS 'In stock', "
                        + "m.expiry_date AS 'Expiry date', DATEDIFF(m.expiry_date, CURRENT_DATE) AS 'Days left', "
                        + "COALESCE(s.name, 'Unassigned') AS Supplier "
                        + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id "
                        + "WHERE m.quantity_in_stock > 0 AND m.expiry_date BETWEEN CURRENT_DATE "
                        + "AND DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH) ORDER BY m.expiry_date, m.name"));
        add(tabs, BorderLayout.CENTER);
    }

    private static final class ReportView extends JPanel {
        private final String sql;
        private final DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final JTable table = Ui.table(model);

        private ReportView(String title, String sql) {
            this.sql = sql;
            setLayout(new BorderLayout(0, 12));
            setBackground(Ui.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(Ui.heading(title), BorderLayout.WEST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton refresh = Ui.secondary("Refresh");
            JButton export = Ui.primary("Export CSV");
            refresh.addActionListener(event -> load());
            export.addActionListener(event -> exportCsv());
            actions.add(refresh);
            actions.add(export);
            header.add(actions, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
            add(Ui.scroll(table), BorderLayout.CENTER);
            load();
        }

        private void load() {
            model.setRowCount(0);
            try (Connection connection = Database.connect();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                int columnCount = metadata.getColumnCount();
                model.setColumnCount(0);
                for (int column = 1; column <= columnCount; column++) {
                    model.addColumn(metadata.getColumnLabel(column));
                }
                while (result.next()) {
                    Object[] row = new Object[columnCount];
                    for (int column = 1; column <= columnCount; column++) {
                        Object value = result.getObject(column);
                        String label = metadata.getColumnLabel(column).toLowerCase(java.util.Locale.ROOT);
                        if (value instanceof java.math.BigDecimal amount &&
                                (label.contains("zar") || label.contains("revenue"))) {
                            value = Ui.money(amount);
                        }
                        row[column - 1] = value;
                    }
                    model.addRow(row);
                }
            } catch (SQLException exception) {
                Ui.error(this, "Could not load report: " + exception.getMessage());
            }
        }

        private void exportCsv() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export report as CSV");
            chooser.setSelectedFile(new java.io.File("healthfirst-report.csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            List<String> lines = new ArrayList<>();
            lines.add(csvRow(columnNames()));
            for (int row = 0; row < model.getRowCount(); row++) {
                List<String> values = new ArrayList<>();
                for (int column = 0; column < model.getColumnCount(); column++) {
                    values.add(String.valueOf(model.getValueAt(row, column)));
                }
                lines.add(csvRow(values));
            }
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), String.join(System.lineSeparator(), lines));
            } catch (IOException exception) {
                Ui.error(this, "Could not export report: " + exception.getMessage());
            }
        }

        private List<String> columnNames() {
            List<String> names = new ArrayList<>();
            for (int column = 0; column < model.getColumnCount(); column++) {
                names.add(model.getColumnName(column));
            }
            return names;
        }

        private String csvRow(List<String> values) {
            return values.stream()
                    .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                    .collect(java.util.stream.Collectors.joining(","));
        }
    }
}